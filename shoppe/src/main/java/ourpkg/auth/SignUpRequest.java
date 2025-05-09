package ourpkg.auth;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignUpRequest {

	private String username;
	private String email;
	private String phone;
	private String password;
	 private String recaptchaResponse;
	
}
