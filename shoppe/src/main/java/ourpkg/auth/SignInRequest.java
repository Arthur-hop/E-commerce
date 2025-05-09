package ourpkg.auth;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignInRequest {
	
	private String username;
	private String password;
	 private String recaptchaResponse;
}