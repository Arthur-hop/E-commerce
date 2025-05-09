package ourpkg.user_role_permission;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ourpkg.user_role_permission.user.dto.UserDTO;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

	private boolean success;
	
	private String message;
	
	private UserDTO userDTO;
	
}
