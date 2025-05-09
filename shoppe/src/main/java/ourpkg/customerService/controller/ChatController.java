package ourpkg.customerService.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import ourpkg.customerService.dto.ChatCreationRequest;
import ourpkg.customerService.dto.ChatMessageDTO;
import ourpkg.customerService.dto.ChatRoomDetailDTO;
import ourpkg.customerService.dto.ConversationDTO;
import ourpkg.customerService.dto.ShopDTO;
import ourpkg.customerService.entity.ChatMessageEntity;
import ourpkg.customerService.entity.ChatRoomEntity;
import ourpkg.customerService.service.ChatService;
import ourpkg.shop.SellerShopRepository;
import ourpkg.shop.Shop;
import ourpkg.user_role_permission.user.User;
import ourpkg.user_role_permission.user.dto.UserDTO;
import ourpkg.user_role_permission.user.service.UserService;

@RestController
@RequestMapping("/api/chat")
public class ChatController {
	private static final Logger log = LoggerFactory.getLogger(ChatController.class);

	@Autowired
	private ChatService chatService;
	@Autowired
	private SimpMessagingTemplate simpMessagingTemplate;
	@Autowired
	private UserService userService;

	@Autowired
	private SellerShopRepository shopRepository;

	// ==== 聊天室建立端點 ====
	@PostMapping("/create")
	@Transactional
	public ResponseEntity<Map<String, Object>> createChatRoom(@RequestBody ChatCreationRequest chatRequest,
			Authentication authentication) {

		User buyer = userService.findByUsername(authentication.getName())
				.orElseThrow(() -> new AccessDeniedException("用户未登录"));

// 阻止卖家创建自己店铺的聊天室
		if (chatService.isShopOwner(chatRequest.getShopId(), buyer.getUserId())) {
			throw new AccessDeniedException("卖家无法创建自己店铺的聊天室");
		}

		ChatRoomEntity chatRoom = chatService.createChatRoom(buyer.getUserId(), chatRequest.getShopId());

// 推送通知给卖家
		Shop shop = shopRepository.findById(chatRequest.getShopId()).orElseThrow();
		simpMessagingTemplate.convertAndSendToUser(shop.getUser().getUserId().toString(), "/queue/new-chat",
				Map.of("shopId", shop.getShopId()));
		return ResponseEntity.ok(Map.of("chatRoomId", chatRoom.getChatRoomId(), "alreadyExists", false));
	}

	@GetMapping("/{chatRoomId}/messages")
	// 1. 修改返回類型為 List<ChatMessageDTO>
	public ResponseEntity<List<ChatMessageDTO>> getMessages(@PathVariable Integer chatRoomId,
			Authentication authentication) {
		User currentUser = userService.findByUsername(authentication.getName())
				.orElseThrow(() -> new AccessDeniedException("使用者未登入"));

		// 權限驗證
		chatService.validateChatRoom(chatRoomId, currentUser.getUserId());

		// 從 Service 獲取 Entity 列表
		List<ChatMessageEntity> messageEntities = chatService.getMessages(chatRoomId);

		// 2. 將 Entity 列表轉換為 DTO 列表
		List<ChatMessageDTO> messageDTOs = messageEntities.stream().map(this::convertToMessageDTOWithSenderInfo) // 使用包含
																													// Sender
																													// 資訊的轉換方法
				.collect(Collectors.toList());

		return ResponseEntity.ok(messageDTOs);
	}

	// 3. 提供或修改一個轉換方法，確保包含 senderId 或 sender DTO
	private ChatMessageDTO convertToMessageDTOWithSenderInfo(ChatMessageEntity entity) {
		if (entity == null) {
			return null;
		}

		UserDTO senderDto = null;
		// 確保 entity.getSender() 返回的是 User 物件，並且該 User 物件有 userId
		if (entity.getSender() != null) {
			senderDto = new UserDTO();
			senderDto.setUserId(entity.getSender().getUserId()); // <-- 獲取 senderId
			senderDto.setUserName(entity.getSender().getUsername()); // 獲取 senderName (假設 User 實體有 getUsername 方法)
			log.debug("轉換訊息 ID: {}, 找到 Sender ID: {}, Sender Name: {}", entity.getMessageId(), senderDto.getUserId(),
					senderDto.getUserName());
		} else {
			// 如果 entity.getSender() 為 null，記錄警告，senderDto 保持 null
			log.warn("訊息 ID {} 的 Sender 為 null，無法獲取 Sender ID 和 Name", entity.getMessageId());
		}

		// 使用 Builder 或 Setter 建立 ChatMessageDTO
		return ChatMessageDTO.builder().messageId(entity.getMessageId())
				.chatRoomId(entity.getChatRoomEntity() != null ? entity.getChatRoomEntity().getChatRoomId() : null) // 添加
																													// null
																													// 檢查
				.content(entity.getContent()).timestamp(entity.getTimestamp()) // 確保類型正確 (Instant/Date/LocalDateTime?)
				.sender(senderDto) // <-- **包含含有 userId 的 sender DTO**
				.senderName(senderDto != null ? senderDto.getUserName() : entity.getSenderName()) // 優先使用 DTO 的名字，備用
																									// entity 可能有的
																									// senderName
				.isRead(entity.getIsRead())
				// 如果前端標準化需要頂層 senderId，可以在 DTO 加欄位並在此設置
				// .senderId(senderDto != null ? senderDto.getUserId() : null)
				.build();
	}

	// ==== 取得聊天室詳情 ====
	@GetMapping("/{chatRoomId}")
	public ResponseEntity<ChatRoomDetailDTO> getChatRoomDetails(@PathVariable Integer chatRoomId,
			Authentication authentication) {
		User currentUser = userService.findByUsername(authentication.getName())
				.orElseThrow(() -> new AccessDeniedException("使用者未登入"));

		// 權限驗證
		ChatRoomEntity chatRoom = chatService.validateChatRoom(chatRoomId, currentUser.getUserId());

		// 構建 DTO
		ChatRoomDetailDTO dto = new ChatRoomDetailDTO();
		dto.setChatRoomId(chatRoom.getChatRoomId());

		// 買家資訊
		dto.setBuyer(convertToUserDTO(chatRoom.getBuyer()));

		// 賣家資訊 (現在直接從 ChatRoomEntity 獲取)
		dto.setSeller(convertToUserDTO(chatRoom.getSeller()));

		// 商店資訊 (可能需要根據您的需求調整)
		// 由於 ChatRoomEntity 不再直接關聯 Shop，您可能需要根據 chatRoomId 或 sellerId 額外查詢 Shop 資訊
		// 這裡提供一個根據 sellerId 查詢 Shop 的範例，您可能需要根據您的實際業務邏輯調整
		Optional<Shop> shopOptional = shopRepository.findByUserUserId(chatRoom.getSeller().getUserId());
		if (shopOptional.isPresent()) {
			dto.setShop(convertToShopDTO(shopOptional.get()));
		}

		return ResponseEntity.ok(dto);
	}

	// ==== WebSocket 訊息處理 ====
	@MessageMapping("/chat/{chatRoomId}/send")
	@SendTo("/topic/chat/{chatRoomId}") // 保留 @SendTo 以廣播訊息本身
	@Transactional
	public Map<String, Object> handleChatMessage(@DestinationVariable Integer chatRoomId,
			@Payload ChatMessageDTO messageDTO, SimpMessageHeaderAccessor headerAccessor) {

		String sessionId = headerAccessor.getSessionId(); // 獲取 Session ID
		log.info("========== [MSG-{}] START Controller Processing ==========", sessionId); // 標記開始

		// --- 【必要日誌 A】記錄從 Session 讀取的 userId ---
		Integer userIdFromSession = (Integer) headerAccessor.getSessionAttributes().get("userId");
		log.info(">>> [MSG-{}] userId from session attributes: {}", sessionId, userIdFromSession);

		// --- 【必要日誌 B】記錄從客戶端收到的原始 DTO ---
		// 注意：如果 DTO 包含敏感資訊，請考慮只記錄部分欄位或調整日誌級別
		log.info(">>> [MSG-{}] Received DTO from client (@Payload): {}", sessionId, messageDTO);

		// --- 執行原本的驗證和填充邏輯 ---
		if (userIdFromSession == null) {
			log.error(">>> [MSG-{}] WebSocket session attribute 'userId' is NULL. Rejecting message.", sessionId);
			// 考慮是否要拋出異常或返回錯誤訊息給客戶端 (需要不同機制，不能直接 return null)
			throw new AccessDeniedException("用戶未識別 (Session無userId)");
		}

		messageDTO.setChatRoomId(chatRoomId); // 設置 ChatRoom ID

		if (messageDTO.getSender() == null || messageDTO.getSender().getUserId() == null) {
			log.debug(">>> [MSG-{}] Payload DTO missing sender info. Populating from session userId: {}", sessionId,
					userIdFromSession);
			UserDTO senderDto = new UserDTO();
			senderDto.setUserId(userIdFromSession);
			// 這裡可以選擇性地加入查找 username 的邏輯，但 Service 層也會做
			// userService.findById(userIdFromSession).ifPresent(u ->
			// senderDto.setUserName(u.getUsername()));
			messageDTO.setSender(senderDto);
		} else if (!messageDTO.getSender().getUserId().equals(userIdFromSession)) {
			log.error(">>> [MSG-{}] Mismatch! Payload Sender ID: {} != Session User ID: {}. Rejecting message.",
					sessionId, messageDTO.getSender().getUserId(), userIdFromSession);
			throw new AccessDeniedException("发送者ID与认证用户不符");
		}
		// 確保頂層 userId 也設置正確（來自 Session）
		messageDTO.setUserId(userIdFromSession);
		// --- 【必要日誌 C】記錄傳遞給 Service 前的 DTO ---
		log.info(">>> [MSG-{}] DTO state before calling chatService.saveMessage: {}", sessionId, messageDTO);

		// --- 呼叫 Service ---
		ChatMessageDTO savedMessageDTO = chatService.saveMessage(messageDTO);
		log.debug(">>> [MSG-{}] Received DTO back from chatService.saveMessage: {}", sessionId, savedMessageDTO); // 記錄
																													// Service
																													// 返回結果

		// --- 推送未讀數更新 ---
		try {
			ChatRoomEntity chatRoom = chatService.getChatRoomById(chatRoomId); // 獲取聊天室實體 (包含 User 實體)
			UserDTO senderDTO = savedMessageDTO.getSender(); // *** 從 DTO 獲取 senderDTO ***
			if (senderDTO == null || senderDTO.getUserId() == null) {
				log.error("儲存後的訊息 DTO 缺少發送者資訊！無法判斷接收者。");
				throw new IllegalStateException("無法從已存訊息中確定發送者");
			}
			Integer senderId = senderDTO.getUserId(); // *** 從 senderDTO 獲取發送者 ID ***

			User recipientEntity = null; // ** 接收者需要是 User 實體，以便檢查角色和獲取 username **

			// 判斷接收者 User 實體
			if (chatRoom.getBuyer() != null && chatRoom.getBuyer().getUserId().equals(senderId)) {
				recipientEntity = chatRoom.getSeller(); // 接收者是聊天室的 Seller User 實體
			} else if (chatRoom.getSeller() != null && chatRoom.getSeller().getUserId().equals(senderId)) {
				recipientEntity = chatRoom.getBuyer(); // 接收者是聊天室的 Buyer User 實體
			}

			// 如果找到了接收者，並且接收者是賣家，則推送更新
			if (recipientEntity != null
					&& recipientEntity.getRole().stream().anyMatch(role -> role.getRoleName().equals("SELLER"))) {
				Integer recipientId = recipientEntity.getUserId();
				String recipientUsername = recipientEntity.getUsername(); // *** 從 User 實體獲取 username ***

				log.info("接收者是賣家 (ID: {}), 正在計算其未讀數...", recipientId);
				Map<Integer, Integer> updatedUnreadCounts = chatService.getUnreadCounts(recipientId);

				String destination = "/queue/unread-update";
				log.info("準備推送未讀數更新給使用者 '{}' (Principal Name)，目的地: {}, 內容: {}", recipientUsername, destination,
						updatedUnreadCounts);

				// *** 使用 recipientUsername (Spring Security Principal Name) 作為推送目標 ***
				simpMessagingTemplate.convertAndSendToUser(recipientUsername, // 使用 Username
						destination, updatedUnreadCounts);
				log.info("已推送未讀數更新給使用者 '{}'", recipientUsername);
			} else {
				log.debug("接收者不是賣家或未找到，跳過未讀數推送。接收者 User ID: {}",
						recipientEntity != null ? recipientEntity.getUserId() : "null");
			}
		} catch (Exception e) {
			log.error("為聊天室 ID {} 推送未讀數更新時發生錯誤: {}", chatRoomId, e.getMessage(), e);
			// 這裡不應中斷主訊息的廣播
		}
		// --- 推送邏輯結束 ---

		// --- 【必要日誌 D】記錄廣播前的最終 Sender 資訊 ---
        log.info(">>> [MSG-{}] === Final Check Before Broadcast ===", sessionId);
        Integer finalSenderId = (savedMessageDTO.getSender() != null) ? savedMessageDTO.getSender().getUserId() : null;
        String finalSenderName = savedMessageDTO.getSenderName(); // 使用優先處理過的頂層 senderName
        log.info(">>> [MSG-{}] Broadcasting with Sender ID: {}", sessionId, finalSenderId);
        log.info(">>> [MSG-{}] Broadcasting with Sender Name: {}", sessionId, finalSenderName);
        log.info(">>> [MSG-{}] ====================================", sessionId);

		// 構建要廣播到 /topic/chat/{chatRoomId} 的訊息內容 (使用 savedMessageDTO)
		Map<String, Object> response = new HashMap<>();
		response.put("id", savedMessageDTO.getMessageId()); // 使用後端生成的 ID
		response.put("tempId", messageDTO.getTempId()); // 回傳前端的臨時 ID
		response.put("content", savedMessageDTO.getContent());
		response.put("senderId", savedMessageDTO.getSender().getUserId()); // *** 從 sender DTO 取 ID ***
		response.put("senderName", savedMessageDTO.getSender().getUserName()); // *** 從 sender DTO 取 Name ***
		response.put("timestamp", savedMessageDTO.getTimestamp().toString()); // 確保格式一致
		response.put("isRead", savedMessageDTO.isRead()); // 包含讀取狀態

		 log.info("========== [MSG-{}] END Controller Processing ==========", sessionId); // 標記結束
		// 如果使用 @SendTo，直接 return response
		return response;
	}

	// 新增根据店铺ID获取聊天室的端点
	@GetMapping("/shop/{shopId}")
	public ResponseEntity<Map<String, Object>> getChatRoomByShop(@PathVariable Integer shopId,
			Authentication authentication) {
		// 1. 身份驗證
		User currentUser = userService.findByUsername(authentication.getName())
				.orElseThrow(() -> new AccessDeniedException("用户未登录"));

		// 2. 驗證店鋪擁有權
		if (!chatService.isShopOwner(shopId, currentUser.getUserId())) {
			throw new AccessDeniedException("無操作權限");
		}

		// 3. 查詢聊天室
		Optional<ChatRoomEntity> chatRoom = chatService.findByShopId(shopId);

		// 4. 返回結果
		Map<String, Object> response = new HashMap<>();
		if (chatRoom.isPresent()) {
			response.put("chatRoomId", chatRoom.get().getChatRoomId());
			return ResponseEntity.ok(response);
		} else {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
		}
	}

	// 新增商店归属校验接口
	@GetMapping("/{shopId}/check-ownership")
	public ResponseEntity<Map<String, Boolean>> checkShopOwnership(@PathVariable Integer shopId,
			Authentication authentication) {

		// 1. 获取当前登录用户
		User currentUser = userService.findByUsername(authentication.getName())
				.orElseThrow(() -> new AccessDeniedException("用户未登录"));

		// 2. 调用服务层检查商店归属
		boolean isOwner = chatService.isShopOwner(shopId, currentUser.getUserId());

		// 3. 构建返回结果
		Map<String, Boolean> response = new HashMap<>();
		response.put("isOwner", isOwner);

		return ResponseEntity.ok(response);
	}

	// 异常处理：商店不存在
	@ExceptionHandler(EntityNotFoundException.class)
	public ResponseEntity<String> handleEntityNotFound(EntityNotFoundException e) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
	}

	// 异常处理：权限不足
	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<String> handleAccessDenied(AccessDeniedException e) {
		return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
	}

	/**
	 * 修改後的端點：供賣家或買家查找與特定對方的聊天室 使用查詢參數 buyerId
	 */
	@GetMapping("/find") // <-- 修改路徑為 /find，使用查詢參數
	public ResponseEntity<Map<String, Object>> findSpecificChatRoom(@RequestParam Integer buyerId, // <-- 接收 buyerId 參數
			Authentication authentication) {

		log.info("====== [Find Specific Chat] START ======");
		log.info("[Find Specific Chat] 請求查找與 buyerId: {} 的聊天室", buyerId);

		if (authentication == null || !authentication.isAuthenticated()) {
			log.warn("[Find Specific Chat] 請求未經驗證！返回 401");
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "未登入或認證無效"));
		}
		String username = authentication.getName();
		log.info("[Find Specific Chat] 認證使用者名稱 (賣家): {}", username);

		try {
			// 1. 獲取當前使用者 (賣家) 的 ID
			User currentUser = userService.findByUsername(username)
					.orElseThrow(() -> new AccessDeniedException("當前用戶不存在"));
			Integer currentUserId = currentUser.getUserId(); // 這裡應該是賣家 ID
			log.info("[Find Specific Chat] 當前使用者 ID (賣家): {}", currentUserId);

			// --- 2. 使用新的 Service 方法查找聊天室 ---
			log.debug("[Find Specific Chat] 步驟 2: 查找參與者 {} 和 {} 的聊天室", currentUserId, buyerId);
			Optional<ChatRoomEntity> chatRoomOpt = chatService.findRoomByParticipants(currentUserId, buyerId);
			// --- ---

			Map<String, Object> response = new HashMap<>();
			if (chatRoomOpt.isPresent()) {
				ChatRoomEntity chatRoom = chatRoomOpt.get();
				response.put("chatRoomId", chatRoom.getChatRoomId());
				log.info("[Find Specific Chat] 找到聊天室，ID: {}. 返回 200 OK", chatRoom.getChatRoomId());
				log.info("====== [Find Specific Chat] END - SUCCESS (200) ======");
				return ResponseEntity.ok(response);
			} else {
				// 如果賣家主動找，但房間不存在，可以考慮創建或返回 404
				// 這裡我們先返回 404，與之前「尚未有買家發起對話」的邏輯類似
				response.put("message", "尚未與該買家建立對話");
				log.warn("[Find Specific Chat] 未找到參與者 {} 和 {} 的聊天室。返回 404 Not Found", currentUserId, buyerId);
				log.info("====== [Find Specific Chat] END - NOT FOUND (404) ======");
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
			}

		} catch (AccessDeniedException ade) {
			log.error("[Find Specific Chat] 權限錯誤: {}", ade.getMessage());
			log.info("====== [Find Specific Chat] END - FORBIDDEN (403) - Exception ======");
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", ade.getMessage()));
		} catch (Exception e) {
			log.error("[Find Specific Chat] 處理 /find?buyerId={} 時發生未預期錯誤", buyerId, e);
			log.info("====== [Find Specific Chat] END - INTERNAL ERROR (500) ======");
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "伺服器內部錯誤"));
		}
	}

	// 新增 HTTP POST 消息发送端点
	@PostMapping("/{chatRoomId}/send")
	@Transactional
	public ResponseEntity<Map<String, Object>> sendMessageViaHttp(@PathVariable Integer chatRoomId,
			@RequestBody Map<String, String> requestBody, Authentication authentication) {

		// 1. 身份验证
		User sender = userService.findByUsername(authentication.getName())
				.orElseThrow(() -> new AccessDeniedException("用户未登录"));

		// 2. 权限验证
		chatService.validateChatRoom(chatRoomId, sender.getUserId());

		// 3. 处理临时消息ID
		String tempId = requestBody.get("tempId");
		String content = requestBody.get("content");

		// 4. 保存消息
		ChatMessageEntity savedMessage = chatService.sendMessage(chatRoomId, sender.getUserId(), content);

		// 5. 构建响应
		Map<String, Object> response = new HashMap<>();
		response.put("messageId", savedMessage.getMessageId());
		response.put("tempId", tempId);
		response.put("timestamp", savedMessage.getTimestamp());

		// 6. 广播消息
		simpMessagingTemplate.convertAndSend("/topic/chat/" + chatRoomId, savedMessage);

		// 7. 更新未读计数
		Integer receiverId = chatService.getChatRoomById(chatRoomId).getSeller().getUserId();
		Integer unreadCount = chatService.getUnreadCount(chatRoomId, receiverId);
		simpMessagingTemplate.convertAndSendToUser(receiverId.toString(), "/queue/notifications",
				Map.of("chatRoomId", chatRoomId, "unreadCount", unreadCount));
		return ResponseEntity.ok(response);
	}

	// ChatController.java 新增端点
	@PostMapping("/{chatRoomId}/mark-read")
	public ResponseEntity<Void> markMessagesAsRead(@PathVariable Integer chatRoomId, Authentication authentication) {

		User user = userService.findByUsername(authentication.getName())
				.orElseThrow(() -> new AccessDeniedException("用户未登录"));

		chatService.markMessagesAsRead(chatRoomId, user.getUserId());

		// 广播已读状态
		simpMessagingTemplate.convertAndSend("/topic/chat/" + chatRoomId + "/read-status",
				Map.of("readerId", user.getUserId()));

		return ResponseEntity.ok().build();
	}

	@GetMapping("/unread")
	public ResponseEntity<Map<Integer, Integer>> getUnreadCounts(@RequestParam Integer sellerId,
			Authentication authentication) {

		// 权限验证
		User currentUser = userService.findByUsername(authentication.getName())
				.orElseThrow(() -> new AccessDeniedException("用户未登录"));

		log.info("[getUnreadCounts] Authenticated User ID from security context: {}, Type: {}", currentUser.getUserId(),
				(currentUser.getUserId() != null ? currentUser.getUserId().getClass().getName() : "null"));
		log.info("[getUnreadCounts] Request parameter sellerId: {}, Type: {}", sellerId,
				(sellerId != null ? sellerId.getClass().getName() : "null"));

		if (!currentUser.getUserId().equals(sellerId)) {
			throw new AccessDeniedException("无权访问该数据");
		}

		return ResponseEntity.ok(chatService.getUnreadCounts(sellerId));
	}

	// ChatController.java (後端) 新增端點
	@GetMapping("/existing-room")
	public ResponseEntity<Map<String, Object>> checkExistingChatRoom(@RequestParam Integer shopId,
			@RequestParam Integer buyerId) {
		Optional<Integer> existingRoom = chatService.findExistingChatRoom(buyerId, shopId);

		Map<String, Object> response = new HashMap<>();
		if (existingRoom.isPresent()) {
			response.put("chatRoomId", existingRoom.get());
			return ResponseEntity.ok(response);
		}
		return ResponseEntity.notFound().build();
	}

	// ==== DTO 轉換工具方法 ====
	private UserDTO convertToUserDTO(User user) {
		UserDTO dto = new UserDTO();
		dto.setUserId(user.getUserId());
		dto.setUserName(user.getUsername());
		return dto;
	}

	private ShopDTO convertToShopDTO(Shop shop) {
		ShopDTO dto = new ShopDTO();
		dto.setShopId(shop.getShopId());
		dto.setShopName(shop.getShopName());
		return dto;
	}

	private ChatMessageDTO convertToMessageDTO(ChatMessageEntity entity) {
		return ChatMessageDTO.builder().messageId(entity.getMessageId())
				.chatRoomId(entity.getChatRoomEntity().getChatRoomId()).content(entity.getContent())
				.timestamp(entity.getTimestamp()).sender(new UserDTO(entity.getSender()))
				.senderName(entity.getSenderName()).isRead(entity.getIsRead()).build();
	}

	// 🟠 DTO轉換方法
	private ChatRoomDetailDTO convertToChatRoomDTO(ChatRoomEntity entity) {
		ChatRoomDetailDTO dto = new ChatRoomDetailDTO();
		dto.setChatRoomId(entity.getChatRoomId());
		dto.setBuyer(convertToUserDTO(entity.getBuyer()));

		// 賣家資訊直接從 ChatRoomEntity 獲取
		dto.setSeller(convertToUserDTO(entity.getSeller()));

		// 商店資訊 (可能需要根據您的需求調整)
		// 由於 ChatRoomEntity 不再直接關聯 Shop，您可能需要根據 sellerId 額外查詢 Shop 資訊
		// 這裡提供一個根據 sellerId 查詢 Shop 的範例，您可能需要根據您的實際業務邏輯調整
		Optional<Shop> shopOptional = shopRepository.findByUserUserId(entity.getSeller().getUserId());
		if (shopOptional.isPresent()) {
			dto.setShop(convertToShopDTO(shopOptional.get()));
		} else {
			// 如果找不到商店，可以將 Shop 設為 null 或創建一個空的 ShopDTO
			dto.setShop(null); // 或者 dto.setShop(new ShopDTO());
		}

		return dto;
	}

	// --- >>> 新增 API 端點：獲取賣家對話列表 <<< ---
	@GetMapping("/seller/conversations")
	public ResponseEntity<?> getSellerConversations(Authentication authentication) {
		// ... (與上一個回答相同的實現) ...
		log.info("====== [Get Seller Conversations] START ======");
		// ... 驗證 authentication ...
		String username = authentication.getName();
		try {
			User seller = userService.findByUsername(username).orElseThrow(() -> new AccessDeniedException("當前用戶不存在"));
			Integer sellerId = seller.getUserId();
			log.info("[Get Seller Conversations] 賣家 ID: {}", sellerId);

			List<ConversationDTO> conversations = chatService.getSellerConversations(sellerId); // 調用 Service

			log.info("[Get Seller Conversations] 成功獲取 {} 條對話。返回 200 OK", conversations.size());
			log.info("====== [Get Seller Conversations] END - SUCCESS (200) ======");
			return ResponseEntity.ok(conversations);

		} catch (AccessDeniedException ade) {
			// ... 錯誤處理 ...
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", ade.getMessage()));
		} catch (Exception e) {
			// ... 錯誤處理 ...
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "伺服器內部錯誤，無法獲取對話列表"));
		}
	}
	// --- >>> 新增 API 端點結束 <<< ---

	// --- >>> 新增 API 端點：獲取買家對話列表 <<< ---
	@GetMapping("/buyer/conversations")
	public ResponseEntity<?> getBuyerConversations(Authentication authentication) {
		log.info("====== [Get Buyer Conversations] START ======");

		if (authentication == null || !authentication.isAuthenticated()) {
			log.warn("[Get Buyer Conversations] 請求未經驗證！返回 401");
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "未登入或認證無效"));
		}
		String username = authentication.getName(); // 這裡是買家的 username
		log.info("[Get Buyer Conversations] 請求用戶: {}", username);

		try {
			User buyer = userService.findByUsername(username).orElseThrow(() -> new AccessDeniedException("當前用戶不存在"));
			Integer buyerId = buyer.getUserId();
			log.info("[Get Buyer Conversations] 買家 ID: {}", buyerId);

			// 調用 Service 方法獲取對話列表
			List<ConversationDTO> conversations = chatService.getBuyerConversations(buyerId);

			log.info("[Get Buyer Conversations] 成功獲取 {} 條對話。返回 200 OK", conversations.size());
			log.info("====== [Get Buyer Conversations] END - SUCCESS (200) ======");
			return ResponseEntity.ok(conversations);

		} catch (AccessDeniedException ade) {
			log.error("[Get Buyer Conversations] 權限錯誤: {}", ade.getMessage());
			log.info("====== [Get Buyer Conversations] END - FORBIDDEN (403) ======");
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", ade.getMessage()));
		} catch (Exception e) {
			log.error("[Get Buyer Conversations] 獲取對話列表時發生未預期錯誤", e);
			log.info("====== [Get Buyer Conversations] END - INTERNAL ERROR (500) ======");
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "伺服器內部錯誤，無法獲取對話列表"));
		}
	}
	// --- >>> 新增 API 端點結束 <<< ---

}