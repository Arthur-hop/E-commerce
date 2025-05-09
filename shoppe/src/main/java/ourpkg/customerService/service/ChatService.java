package ourpkg.customerService.service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import ourpkg.customerService.dto.ChatMessageDTO;
import ourpkg.customerService.dto.ConversationDTO;
import ourpkg.customerService.entity.ChatMessageEntity;
import ourpkg.customerService.entity.ChatRoomEntity;
import ourpkg.customerService.repository.ChatMessageRepository;
import ourpkg.customerService.repository.ChatMessageRepository.UnreadCountProjection;
import ourpkg.customerService.repository.ChatRoomRepository;
import ourpkg.shop.SellerShopRepository;
import ourpkg.shop.Shop;
import ourpkg.user_role_permission.user.User;
import ourpkg.user_role_permission.user.UserRepository;
import ourpkg.user_role_permission.user.dto.UserDTO;

@Service
public class ChatService {
	@Autowired
	private ChatRoomRepository chatRoomRepository;
	@Autowired
	private ChatMessageRepository chatMessageRepository;
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private SellerShopRepository sellerShopRepository;
	
	 private static final Logger log = LoggerFactory.getLogger(ChatService.class); // 加入這行

	@Transactional
	public ChatRoomEntity createChatRoom(Integer buyerId, Integer shopId) {
		User buyer = userRepository.findById(buyerId).orElseThrow();
		Shop shop = sellerShopRepository.findById(shopId).orElseThrow();
		User seller = shop.getUser(); // 從 Shop 實體中獲取賣家 User
		Optional<ChatRoomEntity> existingRoom = chatRoomRepository.findByBuyerAndSeller_Shops(buyer, shop);
		if (existingRoom.isPresent()) {
			return existingRoom.get();
		}
		ChatRoomEntity chatRoom = new ChatRoomEntity();

		chatRoom.setBuyer(buyer);
		chatRoom.setSeller(seller);
		chatRoom.setCreatedAt(LocalDateTime.now());
		return chatRoomRepository.save(chatRoom);
	}

	@Transactional
	public Integer getUnreadCount(Integer chatRoomId, Integer sellerId) {
		// 使用正確的 Repository 方法名：SenderUserId 對應 User.userId
		return chatMessageRepository.countByChatRoomEntityChatRoomIdAndIsReadFalseAndSenderUserIdNot(chatRoomId,
				sellerId);
	}

	@Transactional
	public Integer findExistingRoom(Integer buyerId, Integer shopId) {
		Optional<User> buyerOpt = userRepository.findById(buyerId);
		Optional<Shop> shopOpt = sellerShopRepository.findById(shopId);
		if (buyerOpt.isEmpty() || shopOpt.isEmpty()) {
			return null;
		}
		User buyer = buyerOpt.get();
		Shop shop = shopOpt.get();
		Optional<ChatRoomEntity> existingRoom = chatRoomRepository.findByBuyerAndSeller_Shops(buyer, shop);
		return existingRoom.map(ChatRoomEntity::getChatRoomId).orElse(null);
	}

	// ==== 核心修改 3：強化驗證邏輯 ====
	@Transactional
	public ChatRoomEntity validateChatRoom(Integer chatRoomId, Integer currentUserId) {
		ChatRoomEntity chatRoom = chatRoomRepository.findWithAssociations(chatRoomId)
				.orElseThrow(() -> new EntityNotFoundException("聊天室不存在 (ID: " + chatRoomId + ")"));

		// 强制校验卖家权限
		if (chatRoom.getSeller() != null && chatRoom.getSeller().getUserId().equals(currentUserId)) {
			// ✅ 新增：检查是否存在买家消息
			Integer buyerMessageCount = chatMessageRepository
					.countByChatRoomEntityChatRoomIdAndIsReadFalseAndSenderUserIdNot(chatRoomId, currentUserId);
			
		}

		// 先檢查現有的買家與賣家欄位
		boolean isBuyer = chatRoom.getBuyer() != null && chatRoom.getBuyer().getUserId().equals(currentUserId);
		boolean isSeller = chatRoom.getSeller() != null && chatRoom.getSeller().getUserId().equals(currentUserId);

		// 如果都不匹配，嘗試自動設定對應的角色
		if (!isBuyer && !isSeller) {
			// 取得目前使用者資料（假設 userRepository 可用）
			User currentUser = userRepository.findById(currentUserId)
					.orElseThrow(() -> new EntityNotFoundException("使用者不存在 (ID: " + currentUserId + ")"));

			// 如果聊天室還沒有設定買家，就把目前使用者設為買家
			if (chatRoom.getBuyer() == null) {
				chatRoom.setBuyer(currentUser);
				isBuyer = true;
			}
			// 如果聊天室已設定買家但賣家尚未設定，
			// 且目前使用者有 SELLER 身份（且不與買家重複），則將其設為賣家
			else if (chatRoom.getSeller() == null && currentUser.getRole().contains("SELLER")
					&& (chatRoom.getBuyer() == null
							|| !chatRoom.getBuyer().getUserId().equals(currentUser.getUserId()))) {
				chatRoom.setSeller(currentUser);
				isSeller = true;
			}
		}

		System.out.println("買家:" + isBuyer);
		System.out.println("賣家:" + isSeller);

		if (!isBuyer && !isSeller) {
			throw new AccessDeniedException("使用者 " + currentUserId + " 無權訪問此聊天室");
		}

		return chatRoom;
	}

	 // ==== 核心修改 4：強化訊息發送安全 (加入 Try-Catch 和 Log) ====
    @Transactional
    public ChatMessageEntity sendMessage(Integer chatRoomId, Integer senderId, String content) {
        log.debug("準備發送訊息 (sendMessage) - chatRoomId: {}, senderId: {}, content 長度: {}",
                  chatRoomId, senderId, (content != null ? content.length() : "null"));

        // 先驗證權限
        ChatRoomEntity chatRoom = validateChatRoom(chatRoomId, senderId);
        chatRoom.updateLastActive(); // 更新房間活躍時間
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new EntityNotFoundException("使用者不存在 (ID: " + senderId + ")")); // 加入 ID 到錯誤訊息

        // 創建訊息實體
        ChatMessageEntity message = new ChatMessageEntity();
        message.setChatRoomEntity(chatRoom);
        message.setSender(sender);
        message.setContent(content);
        message.setTimestamp(LocalDateTime.now()); // 明確設置時間戳
        message.setIsRead(false);
        // 考慮是否需要設置 senderName? 如果 ChatMessageEntity 有此欄位
        // if (sender != null) {
        //     message.setSenderName(sender.getUsername()); // 確保 User 有 getUsername()
        // }

        ChatMessageEntity savedMessage = null; // 預先宣告
        try {
            // 在儲存前 Log 準備好的 Entity 的關鍵資訊
            log.debug("準備儲存 ChatMessageEntity: senderId={}, chatRoomId={}, content='{}'",
                      (message.getSender() != null ? message.getSender().getUserId() : "null"), // 安全獲取 ID
                      (message.getChatRoomEntity() != null ? message.getChatRoomEntity().getChatRoomId() : "null"), // 安全獲取 Room ID
                      message.getContent()
            );

            // *** 儲存 ChatRoom (更新時間) - 也可能拋錯 ***
            log.debug("儲存 ChatRoomEntity (更新 lastActive)...");
            chatRoomRepository.save(chatRoom);
            log.debug("ChatRoomEntity 已儲存。");

            // *** 儲存 ChatMessage - 這是主要目標 ***
            log.debug("儲存 ChatMessageEntity...");
            savedMessage = chatMessageRepository.save(message); // <--- 執行資料庫儲存
            // 只有在 save 成功且不拋異常時，才會執行到這裡
            log.debug("ChatMessageEntity 已成功儲存，資料庫 ID: {}", (savedMessage != null ? savedMessage.getMessageId() : "null"));

        } catch (Exception e) {
            // ******** 捕獲所有可能的異常並詳細記錄 ********
            log.error("!!!!!! 資料庫儲存訊息或聊天室 (來自 sendMessage 方法) 時發生嚴重錯誤 !!!!!!", e); // 包含詳細異常堆疊訊息
            // ******** 記錄結束 ********
            // 重新拋出異常，確保 @Transactional 能偵測到並回滾，同時通知 Controller 出錯
            throw new RuntimeException("儲存聊天訊息時發生資料庫錯誤", e);
        }

        // 如果程式能執行到這裡，表示 try 區塊成功完成
        if (savedMessage == null) {
             // 雖然不太可能發生 (JPA save 失敗通常會拋異常)，但還是加上檢查
             log.error("chatMessageRepository.save 返回了 null，但未拋出異常！這不符合預期。");
             throw new IllegalStateException("儲存訊息後未能獲取已保存的實體 (來自 sendMessage)");
        }

        log.debug("sendMessage 方法成功完成，返回 savedMessage (ID: {})", savedMessage.getMessageId());
        return savedMessage; // 返回保存後的 Entity
    }

	// ==== 其他方法改進 ====
	@Transactional
	public List<ChatMessageEntity> getMessages(Integer chatRoomId) {
		if (!chatRoomRepository.existsById(chatRoomId)) {
			throw new EntityNotFoundException("聊天室不存在");
		}
		List<ChatMessageEntity> messages = chatMessageRepository
				.findByChatRoomEntityChatRoomIdOrderByTimestampAsc(chatRoomId);
		// 強制加載關聯對象以避免 LazyInitializationException
		messages.forEach(msg -> {
			if (msg.getSender() != null) {
				msg.getSender().getUsername(); // 觸發加載
			}
		});
		return messages;
	}

	@Transactional
	public ChatRoomEntity getChatRoomById(Integer chatRoomId) {
		return chatRoomRepository.findById(chatRoomId)
				.orElseThrow(() -> new EntityNotFoundException("找不到聊天室，ID：" + chatRoomId));
	}

	@Transactional
    public ChatMessageDTO saveMessage(ChatMessageDTO messageDTO) {
        log.debug("開始儲存訊息，傳入 DTO: {}", messageDTO);

        ChatMessageEntity message = convertToEntity(messageDTO);
        if (message.getContent() == null || message.getContent().isBlank()) {
            throw new IllegalArgumentException("消息内容不能为空");
        }

        // 步驟 1：確定 senderId 的值
        Integer determinedSenderId = messageDTO.getUserId();
        if (determinedSenderId == null && messageDTO.getSender() != null) {
            determinedSenderId = messageDTO.getSender().getUserId();
        }

        // 步驟 2：檢查 senderId 是否仍然為 null
        if (determinedSenderId == null) {
            log.error("saveMessage: 無法從 DTO 確定 senderId: {}", messageDTO);
            throw new IllegalArgumentException("無法確定訊息發送者 ID");
        }

        // *** 步驟 3：將確定的值賦給一個 final (或 effectively final) 的新變數 ***
        final Integer finalSenderId = determinedSenderId;
        log.info("[saveMessage] Determined finalSenderId: {}", finalSenderId); // 改為 INFO
        
        // 步驟 4：查詢 ChatRoom (這裡 lambda 沒用 senderId，所以沒問題)
        ChatRoomEntity chatRoom = chatRoomRepository.findById(messageDTO.getChatRoomId())
                .orElseThrow(() -> new EntityNotFoundException("聊天室不存在 (ID: " + messageDTO.getChatRoomId() + ")"));

        // *** 步驟 5：使用 finalSenderId 進行查詢和在 Lambda 中引用 ***
        User sender = userRepository.findById(finalSenderId) // 使用 finalSenderId 查詢
                .orElseThrow(() -> new EntityNotFoundException("使用者不存在 (ID: " + finalSenderId + ")")); // 在 Lambda 中使用 finalSenderId
        log.info("[saveMessage] Fetched User - ID: {}, Username: {}", sender.getUserId(), sender.getUsername()); // 改為 INFO
        
        // ... 方法的其餘部分保持不變 (使用 'sender' 物件) ...
        message.setChatRoomEntity(chatRoom);
        message.setSender(sender);
        message.setSenderName(sender.getUsername());
        message.setTimestamp(LocalDateTime.now());
        message.setIsRead(false);

        log.debug("準備儲存訊息 Entity: ...");
        ChatMessageEntity savedEntity = chatMessageRepository.save(message);
        log.debug("訊息 Entity 已儲存，ID: {}", savedEntity.getMessageId());

        ChatMessageDTO resultDTO = convertToDTO(savedEntity);
        log.info("[saveMessage] Converted result DTO: Sender ID: {}, Sender Name: {}",
                resultDTO.getSender() != null ? resultDTO.getSender().getUserId() : "null",
                resultDTO.getSender() != null ? resultDTO.getSender().getUserName() : "null"); // 改為 INFO
       
        return resultDTO;
        
    }


	public Optional<ChatRoomEntity> findLatestByShopId(Integer shopId) {
		return chatRoomRepository.findTopByShop_ShopIdOrderByCreatedAtDesc(shopId);
	}

	public Optional<ChatRoomEntity> findActiveByShopId(Integer shopId) {
	    log.info("查找商店 {} 的聊天室 (已暫時移除時間過濾)...", shopId);
	    // 直接呼叫一個不包含時間過濾的 Repository 方法
	    return chatRoomRepository.findLatestByShopId(shopId); // 假設你新增了這個方法
	}

	@Transactional
	public List<ChatRoomEntity> findActiveChatRoomsByShop(Integer shopId) {
		return chatRoomRepository.findByShop_ShopIdWithUnreadMessages(shopId, shopId);
	}

	@Transactional
	public void markMessagesAsRead(Integer chatRoomId, Integer userId) {
		List<ChatMessageEntity> messages = chatMessageRepository.findUnreadMessages(chatRoomId, userId);
		messages.forEach(msg -> msg.setIsRead(true));
	}

	
@Transactional
public Map<Integer, Integer> getUnreadCounts(Integer sellerId) {
    // 1. 獲取賣家所有店鋪
    // 使用 sellerShopRepository (假設它關聯了用戶和商店)
    List<Shop> shops = sellerShopRepository.findByUser_UserId(sellerId);
    log.info("賣家 {} 擁有的商店列表: {}", sellerId, shops); // 建議加入日誌

    // 2. 若無店鋪直接返回空結果
    if (shops.isEmpty()) {
        log.info("賣家 {} 沒有任何商店，返回空的未讀計數。", sellerId); // 建議加入日誌
        return Collections.emptyMap();
    }

    // 3. 提取店鋪ID列表
    List<Integer> shopIds = shops.stream()
            .map(Shop::getShopId)
            .collect(Collectors.toList());
    log.info("賣家 {} 擁有的 shop IDs: {}", sellerId, shopIds); // 建議加入日誌

    // 4. 批量查詢未讀計數
    // 呼叫 chatMessageRepository.countUnreadByShopsAndSeller(shopIds, sellerId)
    // 這個 Repository 方法是現在最關鍵的未知部分
    List<Object[]> results = chatMessageRepository.countUnreadByShopsAndSeller(shopIds, sellerId);
    // 建議加入日誌，打印從資料庫查到的原始結果
    log.info("Repository 為賣家 {} 查詢到的原始未讀計數結果: {}", sellerId,
             results.stream().map(Arrays::toString).collect(Collectors.toList()));

    // 5. 轉換為 Map<shopId, unreadCount>
    // 假設 results 是 Object[]，其中 result[0] 是 shopId (Integer)，result[1] 是 count (Long)
    Map<Integer, Integer> unreadCountsMap = results.stream()
            .collect(Collectors.toMap(
                    result -> (Integer) result[0], // 如果類型不是 Integer 會拋 ClassCastException
                    result -> ((Long) result[1]).intValue() // 如果類型不是 Long 會拋 ClassCastException
            ));
    log.info("為賣家 {} 產生的最終未讀計數 Map: {}", sellerId, unreadCountsMap); // 建議加入日誌
    return unreadCountsMap;
}

// 假設 sellerShopRepository 和 chatMessageRepository 是注入的依賴
// 假設 Shop 實體有 getShopId() 方法回傳 Integer
// 假設有 log 物件可用 (例如 SLF4J Logger)
	public boolean isShopOwner(Integer shopId, Integer userId) {
		Shop shop = sellerShopRepository.findById(shopId)
				.orElseThrow(() -> new EntityNotFoundException("商店不存在 (ID: " + shopId + ")"));
		return shop.getUser().getUserId().equals(userId);
	}

	public Optional<ChatRoomEntity> findByShopId(Integer shopId) {
		List<ChatRoomEntity> chatRooms = chatRoomRepository.findByShop_ShopId(shopId);

		if (chatRooms.size() > 1) {
			log.error("数据异常: 店铺 {} 存在多个聊天室", shopId);
			throw new IllegalStateException("发现重复聊天室记录");
		}

		return chatRooms.stream().findFirst();
	}

	// ==== 新增 DTO 轉換方法 ====
	// 🟠 修改 convertToEntity 方法
	private ChatMessageEntity convertToEntity(ChatMessageDTO dto) {
		ChatMessageEntity entity = new ChatMessageEntity();

		// 從 DTO 複製基本字段
		entity.setContent(dto.getContent());
		entity.setTimestamp(dto.getTimestamp());
		entity.setIsRead(dto.isRead());

		// 注意：這裡不再查詢關聯實體
		return entity;
	}

	 // DTO 轉換方法 - 加入 Null 檢查和 Log
    private ChatMessageDTO convertToDTO(ChatMessageEntity entity) {
        if (entity == null) {
            log.error("convertToDTO 收到 null Entity!");
            // 根據情況返回 null 或拋出異常
            throw new IllegalArgumentException("無法轉換 null 的 ChatMessageEntity");
        }
        // Log Entity 的基本資訊
        log.debug("準備將 Entity (ID: {}) 轉換為 DTO...", entity.getMessageId());

        ChatMessageDTO dto = new ChatMessageDTO();
        dto.setMessageId(entity.getMessageId());
        dto.setContent(entity.getContent());
        dto.setTimestamp(entity.getTimestamp());
        dto.setRead(entity.getIsRead());

        // 安全地設置 ChatRoomId
        if (entity.getChatRoomEntity() != null) {
             dto.setChatRoomId(entity.getChatRoomEntity().getChatRoomId());
        } else {
            log.warn("Entity ID {} 的 ChatRoomEntity 為 null!", entity.getMessageId());
        }

        // **安全地填充 sender 資訊**
        User senderEntity = entity.getSender(); // 從 Entity 獲取關聯的 User
        // *** 加入 Log 觀察從 Entity 取出的 Sender ***
        log.debug("Entity ID {} 的 Sender Entity (來自 entity.getSender()): {}", entity.getMessageId(), senderEntity);

        if (senderEntity != null) {
            UserDTO senderDTO = new UserDTO();
            Integer senderUserId = senderEntity.getUserId(); // 獲取 ID
            String senderUsername = senderEntity.getUsername(); // 獲取 Username (確保方法名正確)

            // *** 加入 Log 觀察從 User Entity 取出的值 ***
            log.debug("從 Sender Entity 獲取到 User ID: {}, Username: {}", senderUserId, senderUsername);

            senderDTO.setUserId(senderUserId);
            senderDTO.setUserName(senderUsername); // 假設 UserDTO 有 setUserName
            dto.setSender(senderDTO); // 設置巢狀 Sender DTO

            // 確保頂層 senderName 也被設定 (優先用 Entity 上的，其次用 User 的)
            dto.setSenderName(entity.getSenderName() != null ? entity.getSenderName() : senderUsername);
            // **可選: 也設置頂層 UserId (如果 DTO 有此欄位)**
            // dto.setUserId(senderUserId);

        } else {
            // *** 如果 senderEntity 是 null，明確記錄錯誤 ***
            log.error("Entity ID {} 的 Sender Entity 為 null! 無法填充 Sender DTO。", entity.getMessageId());
            // 保留 dto.sender 為 null, dto.senderName 可設為預設值或來自 entity.getSenderName()
             dto.setSenderName(entity.getSenderName() != null ? entity.getSenderName() : "未知用戶(無Sender實體)");
        }

        log.debug("轉換完成的 ChatMessageDTO: {}", dto); // Log 最終的 DTO
        return dto;
    }


	// ChatService.java 新增檢查端點邏輯
	@Transactional
	public Optional<Integer> findExistingChatRoom(Integer buyerId, Integer shopId) {
		Optional<User> buyerOpt = userRepository.findById(buyerId);
		Optional<Shop> shopOpt = sellerShopRepository.findById(shopId);
		if (buyerOpt.isEmpty() || shopOpt.isEmpty()) {
			return Optional.empty(); // 或者根據您的需求返回其他值
		}
		User buyer = buyerOpt.get();
		Shop shop = shopOpt.get();
		return chatRoomRepository.findByBuyerAndSeller_Shops(buyer, shop)
				.map(ChatRoomEntity::getChatRoomId);
	}

	 // --- >>> 新增方法：根據參與者 ID 精確查找聊天室 <<< ---
    @Transactional(readOnly = true) // 查詢操作，設為 readOnly
    public Optional<ChatRoomEntity> findRoomByParticipants(Integer userId1, Integer userId2) {
        log.info("嘗試查找參與者 ID {} 和 {} 之間的聊天室", userId1, userId2);
        Optional<ChatRoomEntity> roomOpt = chatRoomRepository.findByParticipantIds(userId1, userId2);
        if (roomOpt.isPresent()) {
            log.info("找到聊天室，ID: {}", roomOpt.get().getChatRoomId());
        } else {
            log.warn("未找到參與者 ID {} 和 {} 之間的聊天室", userId1, userId2);
        }
        return roomOpt;
    }
    // --- >>> 新增方法結束 <<< ---
    
    // --- >>> 獲取賣家對話列表的核心方法 <<< ---
    @Transactional
    public List<ConversationDTO> getSellerConversations(Integer sellerId) {
        log.info("開始獲取賣家 ID: {} 的對話列表", sellerId);

        // 1. 獲取聊天室及關聯數據 (已按 lastActiveAt 排序)
        List<ChatRoomEntity> rooms = chatRoomRepository.findBySellerUserIdWithDetailsOrderByLastActiveDesc(sellerId);
        log.debug("為賣家 {} 找到 {} 個聊天室", sellerId, rooms.size());

        if (rooms.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 批量獲取未讀數
        List<UnreadCountProjection> unreadCountsRaw = chatMessageRepository.countUnreadMessagesPerRoomForSeller(sellerId);
        Map<Integer, Integer> unreadCountsMap = unreadCountsRaw.stream()
                .collect(Collectors.toMap(
                        UnreadCountProjection::getChatRoomId,
                        p -> p.getUnreadCount().intValue()
                ));
        log.debug("獲取到未讀訊息數 Map: {}", unreadCountsMap);

        // 3. 轉換為 ConversationDTO 列表，並獲取最後一條訊息
        List<ConversationDTO> conversations = rooms.stream()
            .map(room -> {
            	
            	 // --- >>> 在這裡加入詳細日誌 <<< ---
                log.info(">>> Mapping Room Entity: ID={}, BuyerID={}, SellerID={}, LastActive={}",
                         room.getChatRoomId(),
                         room.getBuyer() != null ? room.getBuyer().getUserId() : "null",
                         room.getSeller() != null ? room.getSeller().getUserId() : "null",
                         room.getLastActiveAt());
                // --- >>> ---
            	
                ConversationDTO dto = new ConversationDTO();
                dto.setChatRoomId(room.getChatRoomId());
                dto.setSellerId(sellerId);
                dto.setLastActiveAt(room.getLastActiveAt()); // 使用 ChatRoom 的最後活躍時間

                // 買家信息
                if (room.getBuyer() != null) {
                    dto.setBuyerId(room.getBuyer().getUserId());
                    dto.setBuyerName(room.getBuyer().getUsername());
                } else {
                    dto.setBuyerName("未知買家");
                }

                // 商店信息
                if (room.getSeller() != null && room.getSeller().getShop() != null) {
                    dto.setShopId(room.getSeller().getShop().getShopId());
                    dto.setShopName(room.getSeller().getShop().getShopName());
                } else {
                    dto.setShopName("未知商店");
                }

                // 未讀數
                dto.setUnreadCount(unreadCountsMap.getOrDefault(room.getChatRoomId(), 0));

                // 獲取最後一條訊息 (可能 N+1，但對於列表通常可接受，或後續優化)
                Optional<ChatMessageEntity> lastMessageOpt = chatMessageRepository.findTopByChatRoomEntityOrderByTimestampDesc(room);
                if (lastMessageOpt.isPresent()) {
                    ChatMessageEntity lastMessage = lastMessageOpt.get();
                    // 截斷預覽內容，避免過長
                    String preview = lastMessage.getContent();
                    if (preview != null && preview.length() > 30) { // 限制長度
                        preview = preview.substring(0, 30) + "...";
                    }
                    dto.setLastMessageContentPreview(preview);
                    dto.setLastMessageSenderName(lastMessage.getSenderName()); // 使用 Message 上的 SenderName
                    dto.setLastMessageTimestamp(lastMessage.getTimestamp());
                } else {
                    // 如果沒有訊息，可以使用聊天室創建時間或 null
                    dto.setLastMessageTimestamp(room.getCreatedAt());
                    dto.setLastMessageContentPreview("尚無訊息");
                }

                return dto;
            })
            // 可以根據 lastMessageTimestamp 再次排序，確保精確
            .sorted(Comparator.comparing(ConversationDTO::getLastMessageTimestamp, Comparator.nullsLast(Comparator.reverseOrder())))
            .collect(Collectors.toList());

        log.info("為賣家 ID: {} 生成了 {} 個對話 DTO", sellerId, conversations.size());
        return conversations;
    }
    // --- >>> 方法結束 <<< ---
    
    // --- >>> 新增方法：獲取買家的對話列表 <<< ---
    @Transactional // 查詢操作
    public List<ConversationDTO> getBuyerConversations(Integer buyerId) {
        log.info("開始獲取買家 ID: {} 的對話列表", buyerId);

        // 1. 獲取該買家所有的聊天室 (包含預加載的關聯數據)
        List<ChatRoomEntity> rooms = chatRoomRepository.findByBuyerUserIdWithDetailsOrderByLastActiveDesc(buyerId);
        log.debug("為買家 {} 找到 {} 個聊天室", buyerId, rooms.size());

        if (rooms.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 批量獲取所有相關聊天室中買家的未讀訊息數
        List<UnreadCountProjection> unreadCountsRaw = chatMessageRepository.countUnreadMessagesPerRoomForBuyer(buyerId);
        Map<Integer, Integer> unreadCountsMap = unreadCountsRaw.stream()
                .collect(Collectors.toMap(
                        UnreadCountProjection::getChatRoomId,
                        p -> p.getUnreadCount().intValue()
                ));
        log.debug("獲取到買家未讀訊息數 Map: {}", unreadCountsMap);

        // 3. 轉換為 ConversationDTO 列表，並獲取最後一條訊息
        List<ConversationDTO> conversations = rooms.stream()
            .map(room -> {
                ConversationDTO dto = new ConversationDTO();
                dto.setChatRoomId(room.getChatRoomId());
                dto.setBuyerId(buyerId); // 買家 ID 已知

                // 安全地獲取賣家和商店信息
                if (room.getSeller() != null) {
                    dto.setSellerId(room.getSeller().getUserId());
                    dto.setBuyerName(room.getSeller().getUsername()); // 對於買家，顯示的是賣家名稱
                    if (room.getSeller().getShop() != null) {
                        dto.setShopId(room.getSeller().getShop().getShopId());
                        dto.setShopName(room.getSeller().getShop().getShopName());
                    } else {
                         dto.setShopName("未知商店");
                    }
                } else {
                    log.warn("聊天室 ID: {} 的賣家信息為 null", room.getChatRoomId());
                    dto.setBuyerName("未知賣家"); // 顯示對方是未知賣家
                    dto.setShopName("未知商店");
                }

                // 從 Map 中獲取買家的未讀數
                dto.setUnreadCount(unreadCountsMap.getOrDefault(room.getChatRoomId(), 0));

                // 使用 ChatRoom 的 lastActiveAt
                dto.setLastActiveAt(room.getLastActiveAt());

                // 獲取最後一條訊息
                Optional<ChatMessageEntity> lastMessageOpt = chatMessageRepository.findTopByChatRoomEntityOrderByTimestampDesc(room);
                if (lastMessageOpt.isPresent()) {
                    ChatMessageEntity lastMessage = lastMessageOpt.get();
                    String preview = lastMessage.getContent();
                    if (preview != null && preview.length() > 30) {
                        preview = preview.substring(0, 30) + "...";
                    }
                    dto.setLastMessageContentPreview(preview);
                    dto.setLastMessageSenderName(lastMessage.getSenderName());
                    dto.setLastMessageTimestamp(lastMessage.getTimestamp());
                } else {
                    dto.setLastMessageTimestamp(room.getCreatedAt());
                    dto.setLastMessageContentPreview("尚無訊息");
                }

                return dto;
            })
            // 根據最後訊息時間排序
            .sorted(Comparator.comparing(ConversationDTO::getLastMessageTimestamp, Comparator.nullsLast(Comparator.reverseOrder())))
            .collect(Collectors.toList());

        log.info("為買家 ID: {} 生成了 {} 個對話 DTO", buyerId, conversations.size());
        return conversations;
    }
    // --- >>> 方法結束 <<< ---

}