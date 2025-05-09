package ourpkg.customerService.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import ourpkg.customerService.entity.ChatRoomEntity;
import ourpkg.shop.Shop;
import ourpkg.user_role_permission.user.User;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoomEntity, Integer> {
	@Query("SELECT cr FROM ChatRoomEntity cr WHERE cr.buyer = :buyer AND cr.seller.shop = :shop")
	Optional<ChatRoomEntity> findByBuyerAndSeller_Shops(@Param("buyer") User buyer, @Param("shop") Shop shop);
	
	// 加入 JOIN FETCH 的完整關聯查詢
	@Query("SELECT cr FROM ChatRoomEntity cr " +
	        "LEFT JOIN FETCH cr.buyer " +
	        "LEFT JOIN FETCH cr.seller " +
	        "WHERE cr.chatRoomId = :chatRoomId")
	Optional<ChatRoomEntity> findWithAssociations(@Param("chatRoomId") Integer chatRoomId);
    
    
	List<ChatRoomEntity> findBySeller_UserId(Integer sellerId);

	@Query("SELECT c FROM ChatRoomEntity c WHERE c.seller.shop.shopId IN (SELECT s.user.userId FROM Shop s WHERE s.shopId = :shopId)")
	List<ChatRoomEntity> findByShop_ShopId(@Param("shopId") Integer shopId);

	@Query("SELECT c FROM ChatRoomEntity c WHERE c.seller.shop.shopId IN (SELECT s.user.userId FROM Shop s WHERE s.shopId = :shopId) ORDER BY c.createdAt DESC")
	Optional<ChatRoomEntity> findTopByShop_ShopIdOrderByCreatedAtDesc(@Param("shopId") Integer shopId);

	@Query("SELECT c FROM ChatRoomEntity c " +
		       "JOIN c.seller seller " +
		       "JOIN seller.shop shop " +
		       "WHERE shop.shopId = :shopId " +
		       // "AND c.lastActiveAt > :cutoff " + // <-- 完全移除這行
		       "ORDER BY c.lastActiveAt DESC " +
		       "LIMIT 1")
		Optional<ChatRoomEntity> findLatestByShopId(@Param("shopId") Integer shopId); // 新方法，無 cutoff 參數
	
	@Query("SELECT cr FROM ChatRoomEntity cr " +
	        "JOIN FETCH cr.messages m " +
	        "WHERE cr.seller.shop.shopId IN (SELECT s.user.userId FROM Shop s WHERE s.shopId = ?1) " +
	        "AND m.isRead = false " +
	        "AND m.sender.userId <> ?2")
	List<ChatRoomEntity> findByShop_ShopIdWithUnreadMessages(Integer shopId, Integer sellerId);
    
	// --- >>> 新增方法：根據參與者 ID 查找聊天室 <<< ---
    @Query("SELECT cr FROM ChatRoomEntity cr " +
           "WHERE (cr.buyer.userId = :userId1 AND cr.seller.userId = :userId2) " +
           "   OR (cr.buyer.userId = :userId2 AND cr.seller.userId = :userId1)")
    Optional<ChatRoomEntity> findByParticipantIds(@Param("userId1") Integer userId1, @Param("userId2") Integer userId2);
    // --- >>> 新增方法結束 <<< ---
    
    // --- 確認此方法存在 (或使用您現有的等效方法) ---
    @Query("SELECT cr FROM ChatRoomEntity cr " +
           "LEFT JOIN FETCH cr.buyer b " +
           "LEFT JOIN FETCH cr.seller s " +
           "LEFT JOIN FETCH s.shop sh " + // **重要：確認 User 實體確實有關聯到 Shop 的 'shop' 字段**
           "WHERE s.userId = :sellerId " +
           "ORDER BY cr.lastActiveAt DESC NULLS LAST, cr.createdAt DESC") // 添加 createdAt 作為次要排序
    List<ChatRoomEntity> findBySellerUserIdWithDetailsOrderByLastActiveDesc(@Param("sellerId") Integer sellerId);
    // --- ---

 // --- >>> 新增方法：獲取買家聊天室並預先加載關聯數據 <<< ---
    @Query("SELECT cr FROM ChatRoomEntity cr " +
           "LEFT JOIN FETCH cr.buyer b " +       // 預加載買家 (雖然已知，但可能需要其他信息)
           "LEFT JOIN FETCH cr.seller s " +      // 預加載賣家
           "LEFT JOIN FETCH s.shop sh " +        // 預加載賣家的商店
           "WHERE b.userId = :buyerId " +        // <-- 篩選買家 ID
           "ORDER BY cr.lastActiveAt DESC NULLS LAST, cr.createdAt DESC")
    List<ChatRoomEntity> findByBuyerUserIdWithDetailsOrderByLastActiveDesc(@Param("buyerId") Integer buyerId);
    // --- >>> 新增方法結束 <<< ---
}