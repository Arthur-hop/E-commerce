package ourpkg.auth;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nimbusds.jwt.JWTClaimsSet;

import ourpkg.auth.recaptcha.RecaptchaService;
import ourpkg.auth.util.SignUpValidateUtil;
import ourpkg.jwt.JsonWebTokenUtility;
import ourpkg.user_role_permission.Role;
import ourpkg.user_role_permission.user.User;
import ourpkg.user_role_permission.user.UserRepository;

@RequestMapping("/api/auth")
@RestController
@Transactional
public class AuthController {
	private final JsonWebTokenUtility jwtUtil;
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	@Autowired
	public AuthController(JsonWebTokenUtility jwtUtil, UserRepository userRepository, PasswordEncoder passwordEncoder) {
		this.jwtUtil = jwtUtil;
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Autowired
	private AuthService authService;
	
	@Autowired
	private RecaptchaService recaptchaService;

	@PostMapping("/register")
	public ResponseEntity<?> registerUser(@RequestBody SignUpRequest entity) {
		
		 if (!recaptchaService.verifyRecaptcha(entity.getRecaptchaResponse())) {
		        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
		            .body(new SignInResponse(false, "請勾選「我不是機器人」以證明您不是自動程式", null));
		    }
		
		// 原有的註冊邏輯...
		String username = entity.getUsername();
		String password = entity.getPassword();
		String email = entity.getEmail();
		String phone = entity.getPhone();

		if (username == null || username.isBlank()) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new SignUpResponse(false, "請填寫使用者名稱"));
		}

		if (password == null || password.isBlank()) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new SignUpResponse(false, "請填寫密碼"));
		}

		if (email == null || email.isBlank()) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new SignUpResponse(false, "請填寫E-mail"));
		}

		if (phone == null || phone.isBlank()) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new SignUpResponse(false, "請填寫電話號碼"));
		}

		if (!SignUpValidateUtil.isValidUsername(username)) {
			return ResponseEntity.badRequest().body(new SignUpResponse(false, "名稱長度需介於3-20字，且不得含有非法字元"));
		}

		if (!SignUpValidateUtil.isValidPassword(password)) {
			return ResponseEntity.badRequest().body(new SignUpResponse(false, "密碼必須至少 8 碼，包含大小寫與數字"));
		}

		if (!SignUpValidateUtil.isValidEmail(email)) {
			return ResponseEntity.badRequest().body(new SignUpResponse(false, "請輸入正確的E-mail"));
		}

		if (!SignUpValidateUtil.isValidTaiwanPhone(phone)) {
			return ResponseEntity.badRequest().body(new SignUpResponse(false, "請輸入正確電話號碼"));
		}

		SignUpResponse response = authService.insertUser(username, email, password, phone);
		if (response.isSuccess()) {
			return ResponseEntity.status(HttpStatus.CREATED) // 201 Created
					.body(response);
		} else {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST) // 或 其他適當的錯誤碼
					.body(response);
		}
	}

	// 這是一般user的登入
	@PostMapping("/login/withoutReCaptcha")
	public ResponseEntity<?> loginWithoutReCaptcha(@RequestBody SignInRequest entity) {
	    // 原有的登入邏輯...
	    String username = entity.getUsername();
	    String password = entity.getPassword();

	    if (username == null || username.isBlank() || password == null || password.isBlank()) {
	        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
	                .body(new SignInResponse(false, "請輸入帳號與密碼以便執行登入", null));
	    }

	    // 使用新的 AuthResult 進行登入
	    AuthResult authResult = authService.loginUserWithResult(username, password);
	    
	    if (!authResult.isSuccess()) {
	        // 根據不同錯誤碼返回不同的錯誤訊息
	        switch (authResult.getErrorCode()) {
	            case "ACCOUNT_BANNED":
	                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
	                        .body(new SignInResponse(false, "您的帳號已被禁用，請聯繫客服", null));
	            case "INVALID_CREDENTIALS":
	                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
	                        .body(new SignInResponse(false, "登入失敗，您的帳號或密碼錯誤", null));
	            default:
	                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	                        .body(new SignInResponse(false, "登入過程中發生錯誤，請稍後再試", null));
	        }
	    }
	    
	    User user = authResult.getUser();
	    
	    // 生成 JWT token
	    List<String> roles = user.getRole().stream().map(Role::getRoleName).collect(Collectors.toList());
	    List<String> permissions = user.getPermissions();
	    String token = jwtUtil.createToken(user.getUsername(), roles, permissions, user.getUserId());

	    return ResponseEntity.ok(new SignInResponse(true, "登入成功", token));
	}

	// 這是一般user的登入
	@PostMapping("/login")
	public ResponseEntity<?> login(@RequestBody SignInRequest entity) {
	    if (!recaptchaService.verifyRecaptcha(entity.getRecaptchaResponse())) {
	        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
	            .body(new SignInResponse(false, "請勾選「我不是機器人」以證明您不是自動程式", null));
	    }
	    
	    // 原有的登入邏輯...
	    String username = entity.getUsername();
	    String password = entity.getPassword();

	    if (username == null || username.isBlank() || password == null || password.isBlank()) {
	        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
	                .body(new SignInResponse(false, "請輸入帳號與密碼以便執行登入", null));
	    }

	    // 使用新的 AuthResult 進行登入
	    AuthResult authResult = authService.loginUserWithResult(username, password);
	    
	    if (!authResult.isSuccess()) {
	        // 根據不同錯誤碼返回不同的錯誤訊息
	        switch (authResult.getErrorCode()) {
	            case "ACCOUNT_BANNED":
	                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
	                        .body(new SignInResponse(false, "您的帳號已被禁用，請聯繫客服", null));
	            case "INVALID_CREDENTIALS":
	                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
	                        .body(new SignInResponse(false, "登入失敗，您的帳號或密碼錯誤", null));
	            default:
	                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	                        .body(new SignInResponse(false, "登入過程中發生錯誤，請稍後再試", null));
	        }
	    }
	    
	    User user = authResult.getUser();
	    
	    // 生成 JWT token
	    List<String> roles = user.getRole().stream().map(Role::getRoleName).collect(Collectors.toList());
	    List<String> permissions = user.getPermissions();
	    String token = jwtUtil.createToken(user.getUsername(), roles, permissions, user.getUserId());

	    return ResponseEntity.ok(new SignInResponse(true, "登入成功", token));
	}

	// admin的登入
	@PostMapping("/admin/login")
	public ResponseEntity<?> adminLogin(@RequestBody SignInRequest entity) {
	    if (!recaptchaService.verifyRecaptcha(entity.getRecaptchaResponse())) {
	        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
	            .body(new SignInResponse(false, "請勾選「我不是機器人」以證明您不是自動程式", null));
	    }
	    
	    // 原有的管理員登入邏輯...
	    String username = entity.getUsername();
	    String password = entity.getPassword();

	    if (username == null || username.isBlank() || password == null || password.isBlank()) {
	        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
	                .body(new SignInResponse(false, "請輸入帳號與密碼以便執行登入", null));
	    }

	    // 使用新的 AuthResult 進行登入
	    AuthResult authResult = authService.loginUserWithResult(username, password);
	    
	    if (!authResult.isSuccess()) {
	        // 根據不同錯誤碼返回不同的錯誤訊息
	        switch (authResult.getErrorCode()) {
	            case "ACCOUNT_BANNED":
	                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
	                        .body(new SignInResponse(false, "您的帳號已被禁用，請聯繫客服", null));
	            case "INVALID_CREDENTIALS":
	                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
	                        .body(new SignInResponse(false, "登入失敗，您的帳號或密碼錯誤", null));
	            default:
	                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	                        .body(new SignInResponse(false, "登入過程中發生錯誤，請稍後再試", null));
	        }
	    }
	    
	    User user = authResult.getUser();
	    
	    List<String> roles = user.getRole().stream().map(Role::getRoleName).collect(Collectors.toList());
	    List<String> permissions = user.getPermissions();

	    // 檢查是否具有特定角色
	    if (!roles.contains("ADMIN") && !roles.contains("SUPER_ADMIN")) {
	        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new SignInResponse(false, "權限不足", null));
	    }

	    String token = jwtUtil.createToken(user.getUsername(), roles, permissions, user.getUserId());

	    return ResponseEntity.ok(new SignInResponse(true, "登入成功", token));
	}

	@PostMapping("/admin/login/withoutReCaptcha")
	public ResponseEntity<?> adminLoginWithoutReCaptcha(@RequestBody SignInRequest entity) {
	    // 原有的管理員登入邏輯...
	    String username = entity.getUsername();
	    String password = entity.getPassword();

	    if (username == null || username.isBlank() || password == null || password.isBlank()) {
	        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
	                .body(new SignInResponse(false, "請輸入帳號與密碼以便執行登入", null));
	    }

	    // 使用新的 AuthResult 進行登入
	    AuthResult authResult = authService.loginUserWithResult(username, password);
	    
	    if (!authResult.isSuccess()) {
	        // 根據不同錯誤碼返回不同的錯誤訊息
	        switch (authResult.getErrorCode()) {
	            case "ACCOUNT_BANNED":
	                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
	                        .body(new SignInResponse(false, "您的帳號已被禁用，請聯繫客服", null));
	            case "INVALID_CREDENTIALS":
	                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
	                        .body(new SignInResponse(false, "登入失敗，您的帳號或密碼錯誤", null));
	            default:
	                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	                        .body(new SignInResponse(false, "登入過程中發生錯誤，請稍後再試", null));
	        }
	    }
	    
	    User user = authResult.getUser();
	    
	    List<String> roles = user.getRole().stream().map(Role::getRoleName).collect(Collectors.toList());
	    List<String> permissions = user.getPermissions();

	    // 檢查是否具有特定角色
	    if (!roles.contains("ADMIN") && !roles.contains("SUPER_ADMIN")) {
	        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new SignInResponse(false, "權限不足", null));
	    }

	    String token = jwtUtil.createToken(user.getUsername(), roles, permissions, user.getUserId());

	    return ResponseEntity.ok(new SignInResponse(true, "登入成功", token));
	}

	@PostMapping("/complete-google-signup")
	@Transactional
	public ResponseEntity<?> completeGoogleSignup(@RequestBody GoogleSignupRequest request) {
		// 驗證臨時令牌
		JWTClaimsSet claims = jwtUtil.validateToken(request.getToken());
		if (claims == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new SignInResponse(false, "令牌無效或已過期", null));
		}

		try {
			// 檢查是否是 Google 認證令牌
			String tokenType;
			try {
				tokenType = claims.getStringClaim("type");
				if (!"google_auth".equals(tokenType)) {
					return ResponseEntity.badRequest().body(new SignInResponse(false, "非有效的 Google 認證令牌", null));
				}
			} catch (Exception e) {
				return ResponseEntity.badRequest().body(new SignInResponse(false, "令牌類型錯誤", null));
			}

			// 從令牌中提取 Google 資訊
			String googleId = (String) claims.getClaim("googleId");
			String email = (String) claims.getClaim("email");
			String name = (String) claims.getClaim("name");
			Boolean isNewUser = (Boolean) claims.getClaim("isNewUser");

			if (!Boolean.TRUE.equals(isNewUser)) {
				return ResponseEntity.badRequest().body(new SignInResponse(false, "不是新用戶註冊流程", null));
			}

			// 驗證請求
			String password = request.getPassword();
			String phone = request.getPhone();

			if (password == null || password.isBlank()) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new SignInResponse(false, "請填寫密碼", null));
			}

			if (phone == null || phone.isBlank()) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new SignInResponse(false, "請填寫電話號碼", null));
			}

			// 驗證密碼格式
			if (!SignUpValidateUtil.isValidPassword(password)) {
				return ResponseEntity.badRequest().body(new SignInResponse(false, "密碼須包含至少一個大寫字母，且長度界於6-12個字", null));
			}

			// 驗證電話格式
			if (!SignUpValidateUtil.isValidTaiwanPhone(phone)) {
				return ResponseEntity.badRequest().body(new SignInResponse(false, "請輸入正確電話號碼", null));
			}

			// 檢查 email 是否已經被使用
			Optional<User> existingUserOpt = authService.findUserByEmail(email);
			if (existingUserOpt.isPresent()) {
				return ResponseEntity.badRequest().body(new SignInResponse(false, "此 Email 已被註冊", null));
			}

			// 調用 authService 註冊新使用者
			SignUpResponse response = authService.insertGoogleUser(name, email, password, phone, googleId);

			if (!response.isSuccess()) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body(new SignInResponse(false, response.getMessage(), null));
			}

			// 獲取新建立的使用者
			Optional<User> newUserOpt = authService.findUserByEmail(email);
			if (!newUserOpt.isPresent()) {
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
						.body(new SignInResponse(false, "使用者建立失敗", null));
			}

			User newUser = newUserOpt.get();

			// 生成 JWT token
			List<String> roles = newUser.getRole().stream().map(Role::getRoleName).collect(Collectors.toList());
			List<String> permissions = newUser.getPermissions();

			String token = jwtUtil.createToken(newUser.getUsername(), roles, permissions, newUser.getUserId());

			// 返回 token
			return ResponseEntity.ok(new SignInResponse(true, "Google 註冊完成", token));

		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(new SignInResponse(false, "處理註冊時出錯", null));
		}
	}
	
	 /**
     * 解析臨時Token，獲取Google用戶信息和現有帳號信息
     */
    @GetMapping("/token-info")
    public ResponseEntity<?> getTokenInfo(@RequestHeader("Authorization") String authHeader) {
        // 從請求標頭提取令牌
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("success", false, "message", "缺少或格式不正確的令牌"));
        }
        
        String token = authHeader.substring(7); // 移除 "Bearer " 前綴
        
        // 驗證并解析令牌
        JWTClaimsSet claims = jwtUtil.validateToken(token);
        if (claims == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("success", false, "message", "令牌無效或已過期"));
        }
        
        try {
            // 檢查是否是帳號連結令牌
            String tokenType = claims.getStringClaim("type");
            if (!"account_link".equals(tokenType)) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "非有效的帳號連結令牌"));
            }
            
            // 從令牌中提取資訊
            String googleId = (String) claims.getClaim("googleId");
            String email = (String) claims.getClaim("email");
            String name = (String) claims.getClaim("name");
         // 安全地轉換為 Integer
            Object idObj = claims.getClaim("existingUserId");
            Integer existingUserId;

            if (idObj instanceof Integer) {
                existingUserId = (Integer) idObj;
            } else if (idObj instanceof Long) {
                existingUserId = ((Long) idObj).intValue();
            } else if (idObj instanceof Number) {
                existingUserId = ((Number) idObj).intValue();
            } else {
                throw new IllegalArgumentException("existingUserId 不是有效的數字類型");
            }
            
            // 檢查現有用戶是否存在
            Optional<User> existingUserOpt = userRepository.findById(existingUserId);
            if (existingUserOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("success", false, "message", "找不到關聯的帳號"));
            }
            
            User existingUser = existingUserOpt.get();
            
            // 準備響應資料
            Map<String, Object> googleInfo = new HashMap<>();
            googleInfo.put("email", email);
            googleInfo.put("name", name);
            
            Map<String, Object> existingAccountInfo = new HashMap<>();
            existingAccountInfo.put("username", existingUser.getUsername());
            existingAccountInfo.put("email", existingUser.getEmail());
            existingAccountInfo.put("phone", existingUser.getPhone());
            existingAccountInfo.put("createdAt", existingUser.getCreatedAt());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("googleInfo", googleInfo);
            response.put("existingAccount", existingAccountInfo);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("success", false, "message", "處理令牌時出錯"));
        }
    }

    /**
     * 處理綁定Google帳號到現有帳號的請求
     */
    @Transactional
    @PostMapping("/link-google-account")
    public ResponseEntity<?> linkGoogleAccount(@RequestHeader("Authorization") String authHeader) {
        // 從請求標頭提取令牌
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("success", false, "message", "缺少或格式不正確的令牌"));
        }
        
        String token = authHeader.substring(7); // 移除 "Bearer " 前綴
        
        // 驗證并解析令牌
        JWTClaimsSet claims = jwtUtil.validateToken(token);
        if (claims == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("success", false, "message", "令牌無效或已過期"));
        }
        
        try {
            // 檢查是否是帳號連結令牌
            String tokenType = claims.getStringClaim("type");
            if (!"account_link".equals(tokenType)) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "非有效的帳號連結令牌"));
            }
            
            // 從令牌中提取資訊
            String googleId = (String) claims.getClaim("googleId");
         // 安全地轉換為 Integer
            Object idObj = claims.getClaim("existingUserId");
            Integer existingUserId;

            if (idObj instanceof Integer) {
                existingUserId = (Integer) idObj;
            } else if (idObj instanceof Long) {
                existingUserId = ((Long) idObj).intValue();
            } else if (idObj instanceof Number) {
                existingUserId = ((Number) idObj).intValue();
            } else {
                throw new IllegalArgumentException("existingUserId 不是有效的數字類型");
            }
            
            // 檢查現有用戶是否存在
            Optional<User> existingUserOpt = userRepository.findById(existingUserId);
            if (existingUserOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("success", false, "message", "找不到關聯的帳號"));
            }
            
            User existingUser = existingUserOpt.get();
            
            // 檢查此 Google ID 是否已經被其他帳號使用
            User userWithGoogleId = userRepository.findByGoogleId(googleId);
            if (userWithGoogleId != null) {
                // 如果已經存在綁定此 Google ID 的帳號，且不是當前要綁定的帳號
                if (!userWithGoogleId.getUserId().equals(existingUserId)) {
                    return ResponseEntity.badRequest().body(Map.of("success", false, "message", "此 Google 帳號已綁定到其他帳號"));
                }
            }
            
            // 更新現有用戶的 Google ID
            existingUser.setGoogleId(googleId);
            userRepository.save(existingUser);
            
            // 生成標準的 JWT token 用於登入
            List<String> roles = existingUser.getRole().stream().map(Role::getRoleName).collect(Collectors.toList());
            List<String> permissions = existingUser.getPermissions();
            
            String newToken = jwtUtil.createToken(existingUser.getUsername(), roles, permissions, existingUser.getUserId());
            
            // 返回成功響應和新的 JWT token
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Google 帳號已成功綁定到您的帳號");
            response.put("token", newToken);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("success", false, "message", "處理綁定請求時出錯"));
        }
    }

// Google 註冊請求 DTO
	// 修改為 public static 類
	public static class GoogleSignupRequest {
		private String token;
		private String password;
		private String phone;

		// Getters and setters
		public String getToken() {
			return token;
		}

		public void setToken(String token) {
			this.token = token;
		}

		public String getPassword() {
			return password;
		}

		public void setPassword(String password) {
			this.password = password;
		}

		public String getPhone() {
			return phone;
		}

		public void setPhone(String phone) {
			this.phone = phone;
		}
	}
}