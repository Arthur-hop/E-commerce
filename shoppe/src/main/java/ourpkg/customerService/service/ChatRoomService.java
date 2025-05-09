package ourpkg.customerService.service;


import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ourpkg.customerService.entity.ChatRoomEntity;
import ourpkg.customerService.repository.ChatRoomRepository;
import ourpkg.shop.SellerShopRepository;
import ourpkg.shop.Shop;
import ourpkg.user_role_permission.user.User;
import ourpkg.user_role_permission.user.UserRepository;

@Service
public class ChatRoomService {

	@Autowired
	private ChatRoomRepository chatRoomRepository;

	@Autowired
	private SellerShopRepository shopRepository;

	@Autowired
	private UserRepository userRepository;


	 /**
	  * 檢查是否已存在 (buyerId, shopId) 的聊天室
	  */
	 public boolean exists(Integer buyerId, Integer shopId) {
		 Optional<User> buyerOpt = userRepository.findById(buyerId);
		 Optional<Shop> shopOpt = shopRepository.findById(shopId);
		 if (buyerOpt.isEmpty() || shopOpt.isEmpty()) {
			 return false;
		 }
		 User buyer = buyerOpt.get();
		 Shop shop = shopOpt.get();
		 User seller = shop.getUser(); // 從 Shop 取得 Seller
		 return chatRoomRepository.findByBuyerAndSeller_Shops(buyer, shop).isPresent();
	 }

	 /**
	  * 取得已存在的 (buyer, shop) 聊天室
	  */
	 public ChatRoomEntity getByBuyerAndShop(Integer buyerId, Integer shopId) {
		 Optional<User> buyerOpt = userRepository.findById(buyerId);
		 Optional<Shop> shopOpt = shopRepository.findById(shopId);
		 if (buyerOpt.isEmpty() || shopOpt.isEmpty()) {
			 return null;
		 }
		 User buyer = buyerOpt.get();
		 Shop shop = shopOpt.get();
		 User seller = shop.getUser(); // 從 Shop 取得 Seller
		 return chatRoomRepository.findByBuyerAndSeller_Shops(buyer, shop).orElse(null);
	 }


    
	 /**
	  * 建立新的聊天室
	  */
	 public ChatRoomEntity createChatRoom(Integer buyerId, Integer shopId) {
		 User buyer = userRepository.findById(buyerId)
				 .orElseThrow(() -> new RuntimeException("Buyer not found"));
		 Shop sellerShop = shopRepository.findById(shopId)
				 .orElseThrow(() -> new RuntimeException("Shop not found"));
		 User seller = sellerShop.getUser(); // 從 Shop 實體中獲取賣家 User

		 ChatRoomEntity room = new ChatRoomEntity();
		 room.setBuyer(buyer);      // 買家 (User)
		 room.setSeller(seller); // 賣家 (User)
		 room.setCreatedAt(LocalDateTime.now()); // 記得設定創建時間
		 return chatRoomRepository.save(room);
	 }
	 
	 /**
	  * 驗證某位發送者 (senderId) 是否屬於此聊天室 (要嘛是買家, 要嘛是賣家)
	  */
	 public boolean validateSender(Integer chatRoomId, Integer senderId) {
		 Optional<ChatRoomEntity> roomOpt = chatRoomRepository.findById(chatRoomId);
		 if (roomOpt.isEmpty()) return false;

		 ChatRoomEntity room = roomOpt.get();
		 // 買家 ID
		 Integer buyerId = room.getBuyer().getUserId();
		 // 賣家 ID
		 Integer sellerId = room.getSeller().getUserId();
		 // 只要 senderId == buyerId 或 == 賣家 ID 即可
		 return (senderId.equals(buyerId) || senderId.equals(sellerId));

	 }
}
