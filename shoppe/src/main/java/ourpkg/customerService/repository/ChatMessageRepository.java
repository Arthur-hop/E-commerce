package ourpkg.customerService.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import ourpkg.customerService.entity.ChatMessageEntity;
import ourpkg.customerService.entity.ChatRoomEntity;
import ourpkg.user_role_permission.user.User;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, Integer > {
	 // 根據 messageId 查詢消息並按 timestamp 排序
    List<ChatMessageEntity> findByMessageIdOrderByTimestampAsc(Integer messageId);

    // 根據關聯的聊天室的主鍵（假設為 chatRoomId）查詢消息並按 timestamp 排序
    List<ChatMessageEntity> findByChatRoomEntityChatRoomIdOrderByTimestampAsc(Integer chatRoomId);

    // 使用 chatRoomEntity.chatRoomId 作为查询条件
    Integer countByChatRoomEntityChatRoomIdAndIsReadFalseAndSenderUserIdNot(
            Integer chatRoomId, 
            Integer senderId
        );
    
    @Query("SELECT m FROM ChatMessageEntity m " +
    	       "WHERE m.chatRoomEntity.chatRoomId = ?1 " +
    	       "AND m.isRead = false " +
    	       "AND m.sender.userId <> ?2")
    	List<ChatMessageEntity> findUnreadMessages(Integer chatRoomId, Integer userId);

    @Query("SELECT COUNT(m) FROM ChatMessageEntity m WHERE m.chatRoomEntity = :chatRoom")
    Integer countByChatRoomEntity(@Param("chatRoom") ChatRoomEntity chatRoom);

    @Query("SELECT COUNT(m) FROM ChatMessageEntity m " +
           "WHERE m.chatRoomEntity = :chatRoom " +
           "AND m.sender <> :excludeUser")
   Integer countByChatRoomEntityAndSenderNot(
        @Param("chatRoom") ChatRoomEntity chatRoom,
        @Param("excludeUser") User excludeUser
    );
    
    @Query("SELECT s.shopId, COUNT(cm) FROM ChatMessageEntity cm " +
            "JOIN cm.chatRoomEntity cr " +
            "JOIN cr.seller u " +
            "JOIN u.shop s " + // 假設 User 實體有關聯到 Shop 的 shop 屬性
            "WHERE s.shopId IN :shopIds " +
            "AND cm.isRead = false " +
            "AND cm.sender.userId != :sellerId " +
            "GROUP BY s.shopId")
    List<Object[]> countUnreadByShopsAndSeller(@Param("shopIds") List<Integer> shopIds, @Param("sellerId") Integer sellerId);
    

    // --- 確認此方法存在 ---
    @Query("SELECT cr.chatRoomId as chatRoomId, COUNT(m.messageId) as unreadCount " +
           "FROM ChatMessageEntity m JOIN m.chatRoomEntity cr " +
           "WHERE cr.seller.userId = :sellerId " +
           "  AND m.isRead = false " +
           "  AND m.sender.userId != :sellerId " +
           "GROUP BY cr.chatRoomId")
    List<UnreadCountProjection> countUnreadMessagesPerRoomForSeller(@Param("sellerId") Integer sellerId);

    interface UnreadCountProjection {
        Integer getChatRoomId();
        Long getUnreadCount();
    }
    // --- ---

    // --- 確認此方法存在 (或添加): 獲取指定聊天室的最後一條訊息 ---
    Optional<ChatMessageEntity> findTopByChatRoomEntityOrderByTimestampDesc(ChatRoomEntity chatRoomEntity);
    // --- ---
    
 // --- >>> 新增方法：按聊天室分組計算買家的未讀訊息數 <<< ---
    @Query("SELECT cr.chatRoomId as chatRoomId, COUNT(m.messageId) as unreadCount " +
           "FROM ChatMessageEntity m JOIN m.chatRoomEntity cr " +
           "WHERE cr.buyer.userId = :buyerId " +   // <-- 篩選買家
           "  AND m.isRead = false " +           // 訊息未讀
           "  AND m.sender.userId != :buyerId " + // 訊息不是買家自己發的 (即賣家發的)
           "GROUP BY cr.chatRoomId")
    List<UnreadCountProjection> countUnreadMessagesPerRoomForBuyer(@Param("buyerId") Integer buyerId);
    // --- >>> 新增方法結束 <<< ---
}
