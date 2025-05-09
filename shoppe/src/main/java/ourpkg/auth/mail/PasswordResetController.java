package ourpkg.auth.mail;

import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ourpkg.auth.SignUpResponse;
import ourpkg.jwt.JsonWebTokenUtility;
import ourpkg.user_role_permission.user.User;
import ourpkg.user_role_permission.user.service.UserService;

@RestController
@RequestMapping("/api/auth")
public class PasswordResetController {

	//Gmail:MyShopGroupOne@gmail.com
	//ShopeeDB
	
	//應用程式密碼:zyhd qlaq qxdv wsks
	
	@Autowired
	private JsonWebTokenUtility jsonWebTokenUtility;

	@Autowired
    private EmailService emailService;
	
	@Autowired
	private UserService userService;
	
	@Autowired
	private PasswordEncoder passwordEncoder;
	
	@PostMapping("/forgot-password")
	public ResponseEntity<SignUpResponse> forgotPassword(@RequestBody Map<String, String> request) {
		String email = request.get("email");
		Optional<User> optional = userService.findAnyByEmail(email);

		if (!optional.isPresent()) {
			return ResponseEntity.badRequest().body(new SignUpResponse(false, "此Email尚未註冊"));
		}
		
		User user = optional.get();
		String username = user.getUserName();
		
		String resetToken = jsonWebTokenUtility.createResetPasswordToken(email);
		emailService.sendResetPasswordEmail(email, resetToken,username); // 寄送 email 給用戶
		return ResponseEntity.ok().body(new SignUpResponse(true, "您將在幾分鐘後收到一封電子郵件，內有重新設定密碼的步驟說明。"));
	}
	
	@PostMapping("/reset-password")
    public ResponseEntity<SignUpResponse> resetPassword(@RequestBody Map<String, String> request) {
        String token = request.get("token");
        String newPassword = request.get("newPassword");

        if (token == null || newPassword == null) {
        	return ResponseEntity.badRequest().body(new SignUpResponse(false, "缺少 token 或新密碼"));
        }

        // 驗證 Token
        if (!jsonWebTokenUtility.isResetPasswordTokenValid(token)) {
        	return ResponseEntity.badRequest().body(new SignUpResponse(false, "無效或過期的 Token"));
        }

        // 解析 Token 取得 Email
        var claims = jsonWebTokenUtility.validateToken(token);
        String email = claims.getSubject();

        // 查找用戶
        Optional<User> optional = userService.findAnyByEmail(email);
        if (optional.isEmpty()) {
            return ResponseEntity.badRequest().body(new SignUpResponse(false, "用戶不存在"));
        }

        // 檢查密碼格式
        if (!newPassword.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$")) {
            return ResponseEntity.badRequest().body(new SignUpResponse(false, "密碼必須至少 8 碼，包含大小寫與數字"));
        }

        // 更新密碼
        User user = optional.get();
        user.setPassword(passwordEncoder.encode(newPassword));
        userService.resetPassword(user);

        return ResponseEntity.ok().body(new SignUpResponse(true, "密碼重設成功，請重新登入"));
    }

}
