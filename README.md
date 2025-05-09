## 📊 系統架構圖

### 認證流程圖

```
┌──────────────┐    1. 登入請求     ┌──────────────┐
│              │───────────────────>│              │
│    客戶端    │                    │ AuthController│
│              │<───────────────────│              │
└──────────────┘    2. JWT令牌      └──────────────┘
        │                                  │
        │ 3. 附帶JWT                       │ 驗證憑證
        │    後續請求                      ↓
        │                           ┌──────────────┐
        │                           │  AuthService  │
        ↓                           └──────────────┘
┌──────────────┐    4. 攔截請求     ┌──────────────┐
│              │───────────────────>│  JWT過濾器   │
│受保護的API   │                    │(Interceptor) │
│              │<───────────────────│              │
└──────────────┘    5. 認證結果     └──────────────┘
                                            │
                                            │ 解析與驗證
                                            ↓
                                   ┌──────────────┐
                                   │ JWT工具類    │
                                   │(Utility)     │
                                   └──────────────┘
```

### OAuth2流程圖

```
┌──────────────┐    1. 點擊Google登入   ┌──────────────┐       ┌──────────────┐
│              │──────────────────────> │              │──────>│              │
│    客戶端    │                        │ Spring OAuth │       │  Google認證  │
│              │<────────────────────── │              │<──────│              │
└──────────────┘   4. 重定向 + 臨時令牌  └──────────────┘       └──────────────┘
        │                                     2. 授權請求與回調
        │ 5. 完成註冊/綁定
        │    發送臨時令牌
        ↓                                        
┌──────────────┐                        ┌──────────────┐       ┌──────────────┐
│              │                        │ GoogleOAuth2 │       │              │
│AuthController│<─────────────────────> │SuccessHandler│<──────│ User服務層   │
│              │                        │              │       │              │
└──────────────┘                        └──────────────┘       └──────────────┘
        │                                     
        │ 6. 返回正式JWT令牌
        ↓
┌──────────────┐                        
│    客戶端    │                        
│(已認證狀態)  │                       
└──────────────┘                        
```

### 權限控制架構圖

```
┌──────────────┐                      ┌──────────────┐
│              │   HTTP請求 + JWT     │              │
│    客戶端    │─────────────────────>│  JWT過濾器   │
│              │                      │(Interceptor) │
└──────────────┘                      └──────────────┘
                                             │
                                             │ 提取認證信息
                                             ↓
┌──────────────┐                      ┌──────────────┐
│   Spring     │                      │ UserDetails  │
│  Security    │<─────────────────────│   Service    │
│  上下文      │                      │              │
└──────────────┘                      └──────────────┘
        │                                    │
        │                                    │ 加載用戶
        │                                    ↓
        │                            ┌──────────────┐
        │                            │   數據庫     │
        │                            │   User+Role  │
        │                            └──────────────┘
        │
        │  檢查權限
        ↓
┌──────────────┐                      ┌──────────────┐
│  URL路徑級   │                      │   方法級     │
│  權限過濾器  │─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─>│  權限注解    │
│              │                      │ @PreAuthorize│
└──────────────┘                      └──────────────┘
        │                                    │
        │                                    │
        ↓                                    ↓
┌──────────────────────────────────────────────────┐
│                                                  │
│              受保護的API資源和方法               │
│                                                  │
└──────────────────────────────────────────────────┘
```## 🔐 方法級權限控制實例

系統使用Spring Security的`@PreAuthorize`注解實現精細化的方法級權限控制，以下是`AdminUserController`中的實際應用範例：

### 角色分層與權限隔離

系統實現了嚴格的角色分層控制，主要分為：

1. **SUPER_ADMIN**：超級管理員，擁有系統全部權限
2. **ADMIN**：普通管理員，擁有用戶管理權限，但無法管理其他管理員
3. **SELLER**：賣家角色，擁有商品和店鋪管理權限
4. **USER**：普通用戶角色，擁有購買和個人資料管理權限

以下是權限控制的實際實現：

```java
@RestController
@RequestMapping("/api/admin")
public class AdminUserController {
    // 超級管理員專用API - 獲取所有管理員
    @PreAuthorize("hasAnyRole('SUPER_ADMIN')")
    @GetMapping("/any/sa")
    public Page<AdminUserDTO> getUsersByRoleAndName(
            @RequestParam String roleName,
            @RequestParam(required = false) String userName,
            Pageable pageable) {
        return userService.getUsersByRoleAndName(roleName, userName, pageable);
    }
    
    // 超級管理員專用API - 創建管理員
    @PreAuthorize("hasAnyRole('SUPER_ADMIN')")
    @PostMapping("/any")
    public ResponseEntity<SignUpResponse> createAdmin(@RequestBody AdminCreateRequest request) {
        // 創建管理員邏輯
    }
    
    // 超級管理員和管理員共用API - 更新用戶角色
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    @PutMapping("/role/{userId}")
    public ResponseEntity<SignUpResponse> updateUserRoles(@PathVariable Integer userId, 
                                                         @RequestBody RoleUpdateRequest request) {
        // 更新用戶角色邏輯
    }
    
    // 超級管理員專用API - 更新管理員角色
    @PreAuthorize("hasAnyRole('SUPER_ADMIN')")
    @PutMapping("/role/sa/{userId}")
    public ResponseEntity<SignUpResponse> updateAdminRoles(@PathVariable Integer userId, 
                                                          @RequestBody RoleUpdateRequest request) {
        // 更新管理員角色邏輯
    }
    
    // 禁用用戶帳號 - 所有管理員可訪問
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    @PutMapping("/user/status/{id}")
    public ResponseEntity<UserResponse> updateUserStatus(@PathVariable Integer id, 
                                                        @RequestBody StatusUpdateRequest request) {
        // 更新用戶狀態邏輯
    }
    
    // 禁用管理員帳號 - 僅超級管理員可訪問
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PutMapping("/user/sa/status/{id}")
    public ResponseEntity<UserResponse> updateAdminStatus(@PathVariable Integer id, 
                                                         @RequestBody StatusUpdateRequest request) {
        // 額外的業務邏輯檢查 - 防止禁用其他超級管理員
        Optional<User> userOpt = userService.findById(id);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            boolean isSuperAdmin = user.getRole().stream()
                .anyMatch(role -> role.getRoleName().equals("SUPER_ADMIN"));
                
            if (isSuperAdmin) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new UserResponse(false, "不可變更超級管理員的帳號狀態", null));
            }
        }
        // 更新管理員狀態邏輯
    }
}
```

### 權限控制特點

1. **精細化權限範圍**
   - 超級管理員擁有對所有用戶和管理員的操作權限
   - 普通管理員只能操作普通用戶，無法管理其他管理員
   - 特定API僅對超級管理員開放（如創建管理員、管理管理員角色）

2. **業務邏輯內的安全檢查**
   - 除了注解級別的權限控制外，還在方法內部實現額外的安全檢查
   - 例如：防止操作超級管理員帳號狀態的業務邏輯檢查

3. **REST風格的安全資源設計**
   - URL路徑設計反映了權限邊界（如`/api/admin/any/sa`與`/api/admin/any`）
   - HTTP狀態碼正確反映授權問題（403 Forbidden、401 Unauthorized）

4. **多層級角色檢查**
   - 使用`hasAnyRole('ROLE1','ROLE2')`支持多角色權限
   - 使用`hasRole('ROLE')`限制單一角色訪問# C2C專案認證與權限系統

## 📋 系統概述

認證與權限系統是本C2C電商平台的核心安全基礎設施，負責用戶身份驗證、授權控制和安全管理。系統實現了多重認證方式，包括傳統的用戶名密碼認證以及Google OAuth社交媒體登入，並基於JWT（JSON Web Token）提供安全且可擴展的身份驗證機制。

## 🔐 主要功能

### 1. JWT身份驗證

系統採用JWT（JSON Web Token）實現無狀態的身份驗證機制：

- **標準JWT實現**：使用nimbus-jose-jwt庫實現JWT令牌的生成、驗證和解析
- **權限與角色封裝**：JWT令牌中包含用戶角色（roles）和權限（permissions）信息
- **令牌安全機制**：
  - 使用HS512算法簽名
  - 可配置的過期時間控制
  - 令牌類型區分（auth、reset_password、google_auth、account_link等）
- **用戶識別**：令牌中嵌入用戶ID和相關信息

```java
// JWT令牌生成示例
String token = jwtUtil.createToken(user.getUsername(), roles, permissions, user.getUserId());

// JWT驗證過濾器截取
String token = authHeader.substring(7); // 移除 "Bearer " 前綴
JWTClaimsSet claimsSet = jsonWebTokenUtility.validateToken(token);
if (claimsSet != null && SecurityContextHolder.getContext().getAuthentication() == null) {
    String username = claimsSet.getSubject();
    // 權限解析和認證流程...
}
```

### 2. Google OAuth 登入整合

系統實現了與Google OAuth 2.0的完整整合，支持社交媒體賬號登入：

- **OAuth流程處理**：使用Spring Security OAuth2 Client處理認證流程
- **多場景登入策略**：根據用戶情況智能處理不同登入場景
  - 新用戶首次登入：創建臨時令牌並導向完善信息頁面
  - 現有Google用戶：直接生成JWT令牌完成登入
  - 電子郵件已存在的用戶：提供賬號綁定選項

```java
// Google OAuth成功處理流程
@Override
public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                  Authentication authentication) throws IOException {
    OAuth2User oauth2User = ((OAuth2AuthenticationToken) authentication).getPrincipal();
    String googleId = (String) oauth2User.getAttributes().get("sub");
    String email = (String) oauth2User.getAttributes().get("email");
    
    User existingUser = userRepository.findByGoogleId(googleId);
    if (existingUser != null) {
        // 已存在Google用戶，生成JWT令牌
    } else {
        Optional<User> emailUser = userRepository.findByEmail(email);
        if (emailUser.isPresent()) {
            // 電子郵件已存在，生成綁定令牌
            String token = jwtUtility.createTemporaryLinkToken(googleId, email, name, userByEmail.getUserId());
        } else {
            // 全新用戶，生成註冊令牌
            String token = jwtUtility.createTemporaryGoogleToken(googleId, email, name);
        }
    }
}
```

### 3. 權限控制系統

基於RBAC（Role-Based Access Control）模型的權限控制系統：

- **雙重權限驗證機制**：
  - 主要方法：從數據庫加載完整的用戶權限
  - 備用方法：從JWT令牌中提取權限信息
- **Spring Security整合**：使用`GrantedAuthority`管理權限和角色
- **權限對象**：角色（ROLE_XXX）和細粒度權限（Permission）的組合

```java
// JWT認證對象創建
JsonWebTokenAuthentication authentication = new JsonWebTokenAuthentication(userDetails, token);
SecurityContextHolder.getContext().setAuthentication(authentication);

// 權限檢查示例 - 管理員登入
if (!roles.contains("ADMIN") && !roles.contains("SUPER_ADMIN")) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(new SignInResponse(false, "權限不足", null));
}
```

### 4. 安全增強功能

系統實現了多種安全增強機制：

- **reCAPTCHA整合**：防止機器人自動註冊和登入攻擊
- **密碼安全策略**：強制實施密碼複雜度要求
- **賬號狀態管理**：支持賬號禁用、激活等狀態控制
- **密碼重設機制**：安全的基於電子郵件的密碼重設流程

```java
// reCAPTCHA驗證示例
if (!recaptchaService.verifyRecaptcha(entity.getRecaptchaResponse())) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(new SignInResponse(false, "請勾選「我不是機器人」以證明您不是自動程式", null));
}

// 密碼格式驗證
if (!SignUpValidateUtil.isValidPassword(password)) {
    return ResponseEntity.badRequest()
        .body(new SignUpResponse(false, "密碼必須至少 8 碼，包含大小寫與數字"));
}
```

### 5. 電子郵件通知系統

集成的電子郵件服務用於用戶通知和驗證：

- **密碼重設電子郵件**：包含臨時訪問令牌的安全鏈接
- **商店申請結果通知**：自定義HTML模板的電子郵件
- **商品審核通知**：富文本電子郵件，支持嵌入圖片

```java
// 發送密碼重設郵件
String resetToken = jsonWebTokenUtility.createResetPasswordToken(email);
emailService.sendResetPasswordEmail(email, resetToken, username);
```

## 🔧 技術實現詳解

### JWT實現（JsonWebTokenUtility）

JWT令牌的生成與驗證核心類，處理各種類型的令牌：

```java
@Component
public class JsonWebTokenUtility {
    // 配置項
    @Value("${jwt.token.expire}")
    private long expire;
    
    @Value("${jwt.reset-token.expire}")
    private long resetExpire;
    
    @Value("${jwt.secret}")
    private String secretKey;
    
    // 令牌生成方法
    public String createToken(String username, List<String> roles, List<String> permissions, Integer userId) {
        return generateToken(username, userId, roles, permissions, expire, "auth");
    }
    
    public String createResetPasswordToken(String email) {
        return generateToken(email, null, null, null, resetExpire, "reset_password");
    }
    
    public String createTemporaryGoogleToken(String googleId, String email, String name) {
        // Google用戶臨時令牌生成邏輯
    }
    
    // 令牌驗證方法
    public JWTClaimsSet validateToken(String token) {
        try {
            JWSVerifier verifier = new MACVerifier(sharedKey);
            SignedJWT signedJWT = SignedJWT.parse(token);
            // 驗證簽名和過期時間...
        } catch (Exception e) {
            // 處理驗證異常...
        }
        return null;
    }
}
```

### JWT認證過濾器（JsonWebTokenInterceptor）

請求過濾器，負責從HTTP請求中提取JWT令牌並建立認證上下文：

```java
@Component
public class JsonWebTokenInterceptor extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) {
        // 提取Authorization頭
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            JWTClaimsSet claimsSet = jsonWebTokenUtility.validateToken(token);
            
            if (claimsSet != null) {
                // 嘗試從數據庫加載用戶信息（主要方法）
                try {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                    // 使用數據庫最新權限建立認證...
                } catch (Exception ex) {
                    // 回退到從JWT提取權限（備用方法）
                    List<String> roles = claimsSet.getStringListClaim("roles");
                    List<String> permissions = claimsSet.getStringListClaim("permissions");
                    // 建立認證對象...
                }
            }
        }
        chain.doFilter(request, response);
    }
}
```

### Google OAuth成功處理器（GoogleOAuth2SuccessHandler）

處理Google OAuth認證成功後的業務邏輯：

```java
@Component
public class GoogleOAuth2SuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {
    @Transactional
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                      Authentication authentication) {
        // 提取Google用戶信息
        OAuth2User oauth2User = ((OAuth2AuthenticationToken) authentication).getPrincipal();
        Map<String, Object> attributes = oauth2User.getAttributes();
        
        // 根據用戶情況決定後續流程
        User existingUser = userRepository.findByGoogleId(googleId);
        if (existingUser != null) {
            // 已存在用戶，檢查狀態
            if (existingUser.getStatus() == User.UserStatus.BANNED) {
                // 用戶被禁用，重定向到錯誤頁面
            } else {
                // 正常用戶，生成JWT令牌
                List<String> roles = getUserRoles(existingUser);
                List<String> permissions = getUserPermissions(existingUser);
                String token = jwtUtility.createToken(...);
                // 重定向到成功頁面
            }
        } else {
            // 新用戶處理流程...
        }
    }
}
```

### 認證控制器（AuthController）

處理所有認證相關HTTP請求的REST控制器：

```java
@RequestMapping("/api/auth")
@RestController
@Transactional
public class AuthController {
    // 用戶註冊
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody SignUpRequest entity) {
        // 驗證reCAPTCHA
        // 驗證輸入格式
        // 創建用戶
    }
    
    // 用戶登入
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody SignInRequest entity) {
        // 驗證reCAPTCHA
        // 驗證憑證
        // 生成JWT令牌
    }
    
    // 完成Google註冊
    @PostMapping("/complete-google-signup")
    public ResponseEntity<?> completeGoogleSignup(@RequestBody GoogleSignupRequest request) {
        // 驗證臨時令牌
        // 完善用戶信息
        // 創建正式用戶
    }
    
    // 綁定Google賬號
    @PostMapping("/link-google-account")
    public ResponseEntity<?> linkGoogleAccount(@RequestHeader("Authorization") String authHeader) {
        // 驗證臨時令牌
        // 綁定Google ID到現有賬號
    }
}
```

## 🔄 認證流程

### 傳統用戶名密碼登入流程

1. 用戶提交用戶名和密碼到`/api/auth/login`端點
2. 系統驗證reCAPTCHA（如啟用）
3. 使用`AuthService.loginUserWithResult`驗證憑證
4. 檢查用戶狀態（正常/禁用）
5. 提取用戶角色和權限
6. 生成JWT令牌並返回給客戶端
7. 客戶端存儲令牌，後續請求帶上Bearer Token

### Google OAuth登入流程

1. 用戶點擊"使用Google登入"按鈕，前端重定向到OAuth授權URL
2. 用戶在Google頁面授權後，Google重定向回應用的OAuth回調URL
3. `GoogleOAuth2SuccessHandler`處理回調：
   - 檢查`googleId`是否已存在關聯用戶
     - 如存在，檢查賬號狀態並生成JWT令牌
     - 如不存在，檢查電子郵件是否已註冊：
       - 如已註冊，生成臨時綁定令牌，重定向到綁定頁面
       - 如未註冊，生成臨時註冊令牌，重定向到資料完善頁面
4. 用戶完成註冊或綁定流程後，獲取正式JWT令牌
5. 客戶端存儲令牌，後續請求帶上Bearer Token

### 密碼重設流程

1. 用戶請求密碼重設，提供電子郵件
2. 系統生成`reset_password`類型的臨時JWT令牌
3. 系統發送包含重設連結的電子郵件
4. 用戶點擊電子郵件中的連結，進入重設頁面
5. 用戶輸入新密碼，系統驗證令牌和密碼格式
6. 成功更新密碼，用戶可使用新密碼登入

## 🛠️ API端點

### 用戶認證API

| 端點 | 方法 | 功能描述 | 請求參數 |
|------|------|----------|---------|
| `/api/auth/register` | POST | 註冊新用戶 | username, password, email, phone, recaptchaResponse |
| `/api/auth/login` | POST | 用戶登入（含reCAPTCHA） | username, password, recaptchaResponse |
| `/api/auth/login/withoutReCaptcha` | POST | 用戶登入（無reCAPTCHA） | username, password |
| `/api/auth/admin/login` | POST | 管理員登入（含reCAPTCHA） | username, password, recaptchaResponse |
| `/api/auth/admin/login/withoutReCaptcha` | POST | 管理員登入（無reCAPTCHA） | username, password |
| `/api/auth/forgot-password` | POST | 請求密碼重設 | email |
| `/api/auth/reset-password` | POST | 設置新密碼 | token, newPassword |

### Google OAuth相關API

| 端點 | 方法 | 功能描述 | 請求參數 |
|------|------|----------|---------|
| `/api/auth/complete-google-signup` | POST | 完成Google用戶註冊 | token, password, phone |
| `/api/auth/token-info` | GET | 獲取臨時令牌信息 | Authorization頭（Bearer Token） |
| `/api/auth/link-google-account` | POST | 將Google賬號綁定到現有賬號 | Authorization頭（Bearer Token） |

## ⚙️ 配置說明

### JWT配置

JWT令牌相關配置項：

```properties
# JWT配置
jwt.secret=your_very_secure_secret_key    # JWT簽名密鑰
jwt.token.expire=1440                     # 一般令牌過期時間（分鐘）
jwt.reset-token.expire=1440               # 密碼重設令牌過期時間（分鐘）
```

### Google OAuth配置

Google OAuth相關配置項：

```properties
# Google OAuth配置
spring.security.oauth2.client.registration.google.client-id=your_client_id
spring.security.oauth2.client.registration.google.client-secret=your_client_secret
spring.security.oauth2.client.registration.google.scope=email,profile

# 前端應用URL（用於重定向）
frontend.url=http://localhost:5173
```

### 電子郵件配置

電子郵件服務相關配置：

```properties
# JavaMail配置
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your_email@gmail.com
spring.mail.password=your_app_password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true

# 密碼重設頁面URL
frontend.reset-password-url=http://localhost:5173/reset-password
```

## 📋 Spring Security配置

系統通過`SecurityConfig`類配置Spring Security，實現全面的應用安全策略：

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    // 配置依賴注入
    private final JsonWebTokenInterceptor jwtInterceptor;
    private final GoogleOAuth2SuccessHandler googleOAuth2SuccessHandler;
    private final UserDetailsServiceImpl userDetailsService;
    private final PasswordEncoder passwordEncoder;
    
    // 配置認證提供者
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }
    
    // 配置主要安全過濾鏈
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // 公開API端點配置
                .requestMatchers("/api/auth/**", "/ws/**", "/api/shop/**", /* 其他開放路徑 */).permitAll()
                // 基於角色的API訪問控制
                .requestMatchers("/api/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                .requestMatchers("/api/user/**").hasAnyRole("USER", "ADMIN", "SUPER_ADMIN")
                .requestMatchers("/api/seller/**").hasAnyRole("SELLER", "ADMIN", "SUPER_ADMIN")
                // 所有其他請求需要認證
                .anyRequest().authenticated())
            .oauth2Login(oauth2 -> oauth2.successHandler(googleOAuth2SuccessHandler))
            // 配置JWT過濾器
            .addFilterBefore(jwtInterceptor, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
}
```

### 核心安全特性

1. **無狀態認證**：
   - 使用`SessionCreationPolicy.STATELESS`配置無會話狀態
   - 所有認證通過JWT令牌處理，無需服務器端會話

2. **路徑級權限控制**：
   - 公開路徑（`/api/auth/**`等）允許未認證訪問
   - 管理員路徑（`/api/admin/**`）僅允許具有ADMIN或SUPER_ADMIN角色的用戶
   - 賣家路徑（`/api/seller/**`）僅允許具有SELLER角色的用戶
   - 買家路徑（`/api/user/**`）僅允許具有USER角色的用戶

3. **CORS配置**：
   - 精確定義可跨域訪問的前端源（`http://localhost:5173`等）
   - 配置允許的HTTP方法和頭信息
   - 啟用憑證傳輸，允許跨域請求攜帶JWT令牌

4. **內容安全策略(CSP)**：
   - 詳細定義腳本、框架、表單等資源的來源策略
   - 特別處理支付相關域名（如綠界支付網域）的安全配置

5. **OAuth2集成**：
   - 配置Google OAuth2登入流程
   - 自定義OAuth2成功處理器（`googleOAuth2SuccessHandler`）

### 用戶詳情服務實現

系統實現了自定義的`UserDetailsService`，高效加載用戶和權限數據：

```java
@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    private final UserRepository userRepository;
    
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 優化查詢：使用fetchJoin預先加載角色集合，避免N+1問題
        User user = userRepository.findByUserNameWithRoles(username)
                .orElseThrow(() -> new UsernameNotFoundException("找不到用戶: " + username));
        
        return user;
    }
}
```

關鍵特性：
- 使用`@Transactional`確保在事務內加載權限數據
- 優化查詢避免N+1問題，一次性加載用戶及其所有角色和權限
- 直接返回實現了`UserDetails`接口的`User`對象

## 📋 安全最佳實踐

1. **JWT令牌安全**：
   - 使用HS512強加密算法
   - 合理設置令牌過期時間
   - 為不同用途使用不同類型的令牌

2. **密碼安全**：
   - 使用BCrypt加密存儲密碼
   - 強制密碼複雜度要求
   - 提供安全的密碼重設機制

3. **機器人防護**：
   - 整合Google reCAPTCHA服務
   - 提供帶/不帶驗證碼的API版本

4. **權限隔離**：
   - 基於角色的訪問控制（RBAC）
   - 用戶與管理員獨立登入端點
   - 細粒度權限控制：URL路徑和方法級安全性

5. **輸入驗證**：
   - 全面的格式驗證（郵箱、電話、用戶名、密碼）
   - 防SQL注入和XSS攻擊的輸入清理

6. **安全標頭配置**：
   - 內容安全策略（CSP）嚴格定義資源來源
   - 禁用X-Frame-Options防止點擊劫持

## 🔍 潛在改進

1. **添加刷新令牌**：實現刷新令牌機制，延長用戶會話有效期
2. **雙因素認證**：集成短信或應用驗證碼的雙因素認證
3. **JWT黑名單**：實現退出登入後的JWT黑名單機制
4. **安全審計日誌**：記錄詳細的身份驗證和授權事件日誌
5. **OAuth擴展**：支持其他社交媒體平台（Facebook、Line等）的認證
