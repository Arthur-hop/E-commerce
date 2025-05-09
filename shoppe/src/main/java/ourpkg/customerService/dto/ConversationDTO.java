package ourpkg.customerService.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConversationDTO {
	  private Integer chatRoomId;
	    private Integer buyerId;
	    private String buyerName;
	    private Integer sellerId;
	    private Integer shopId;
	    private String shopName;
	    private Integer unreadCount; // 賣家在此對話中的未讀數
	    private LocalDateTime lastActiveAt; // 聊天室最後活躍時間 (來自 ChatRoomEntity)
	    private String lastMessageContentPreview; // 最後一條訊息預覽
	    private String lastMessageSenderName; // 最後一條訊息發送者
	    private LocalDateTime lastMessageTimestamp; // 最後一條訊息時間戳
}
