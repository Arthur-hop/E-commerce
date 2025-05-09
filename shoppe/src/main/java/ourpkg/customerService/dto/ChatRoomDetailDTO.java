package ourpkg.customerService.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ourpkg.user_role_permission.user.dto.UserDTO;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomDetailDTO {
	 private Integer chatRoomId;
	    private UserDTO buyer;
	    private UserDTO seller;
	    private ShopDTO shop;
}
