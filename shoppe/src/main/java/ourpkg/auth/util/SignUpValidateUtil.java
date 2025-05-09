package ourpkg.auth.util;

import java.util.regex.Pattern;

public class SignUpValidateUtil {
	
	
	//電話驗證
	public static boolean isValidTaiwanPhone(String phone) {
		return phone != null && phone.matches("^09[0-9]{8}$");
	}

	//名稱驗證
	private static final String USERNAME_PATTERN = "^[a-zA-Z0-9_\u4e00-\u9fa5-]{3,20}$";
	public static boolean isValidUsername(String username) {
	    return username != null && username.matches(USERNAME_PATTERN);
	}
	//密碼驗證
	private static final String PASSWORD_PATTERN = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$"; // 密碼必須至少 8 碼，包含大小寫與數字
    public static boolean isValidPassword(String password) {
        return password != null && password.matches(PASSWORD_PATTERN);
    }
    
    //Email驗證(通用Email)
    private static final String EMAIL_REGEX = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
    private static final Pattern EMAIL_PATTERN = Pattern.compile(EMAIL_REGEX);

    public static boolean isValidEmail(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email).matches();
    }
}
