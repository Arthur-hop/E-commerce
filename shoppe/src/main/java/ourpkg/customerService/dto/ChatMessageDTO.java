package ourpkg.customerService.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ourpkg.user_role_permission.user.dto.UserDTO;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageDTO {
	private Integer userId;
	private Integer chatRoomId;
	private String content;
	private LocalDateTime timestamp;
	private UserDTO sender;
	private String senderName;
	private Integer messageId;
	private boolean isRead;
	private String tempId;

		
	
}
