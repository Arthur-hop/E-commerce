package ourpkg.user_role_permission;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserRequest {

	private Integer userId;

	private String userName;

	private String email;

//	private String password;

	private String phone;
	
}
