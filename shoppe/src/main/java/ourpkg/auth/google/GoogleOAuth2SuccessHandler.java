package ourpkg.auth.google;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import ourpkg.jwt.JsonWebTokenUtility;
import ourpkg.user_role_permission.user.User;
import ourpkg.user_role_permission.user.UserRepository;

@Component
public class GoogleOAuth2SuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {
    private final UserRepository userRepository;
    private final JsonWebTokenUtility jwtUtility;
    
    // 前端應用URL
    private static final String FRONTEND_URL = "http://localhost:5173";

    @Autowired
    public GoogleOAuth2SuccessHandler(UserRepository userRepository, JsonWebTokenUtility jwtUtility) {
        this.userRepository = userRepository;
        this.jwtUtility = jwtUtility;
    }

    @Transactional
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        
        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        OAuth2User oauth2User = oauthToken.getPrincipal();
        
        // 從 Google 取得使用者資訊
        Map<String, Object> attributes = oauth2User.getAttributes();
        String googleId = (String) attributes.get("sub");
        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");
        String picture = (String) attributes.get("picture"); // 獲取使用者頭像URL
        
        // 確保頭像URL正確編碼，防止URL參數問題
        String encodedPicture = "";
        if (picture != null && !picture.isEmpty()) {
            encodedPicture = URLEncoder.encode(picture, StandardCharsets.UTF_8);
        }
        
        User existingUser = userRepository.findByGoogleId(googleId);
        String redirectUrl;

        if (existingUser != null) {
            // 檢查用戶是否被禁用
            if (existingUser.getStatus() == User.UserStatus.BANNED) {
                // 用戶被禁用，重定向到錯誤頁面
                redirectUrl = FRONTEND_URL + "/user/login?error=account_banned";
            } else {
                // 使用者已存在且狀態正常，生成 JWT token
                // 從使用者獲取角色和權限
                List<String> roles = getUserRoles(existingUser);
                List<String> permissions = getUserPermissions(existingUser);
                
                // 將頭像URL添加到token中或更新用戶資料
                if (picture != null && !picture.isEmpty()) {
                    // 如果User實體中有avatarUrl屬性，可以更新它
                    // existingUser.setAvatarUrl(picture);
                    // userRepository.save(existingUser);
                }
                
                String token = jwtUtility.createToken(existingUser.getUsername(), roles, permissions, existingUser.getUserId());
                // 在重定向URL中添加頭像參數，確保正確編碼
                redirectUrl = FRONTEND_URL + "/login/oauth2/success?token=" + token;
                if (!encodedPicture.isEmpty()) {
                    redirectUrl += "&picture=" + encodedPicture;
                }
            }
        } else {
            // 檢查電子郵件是否已存在
            Optional<User> op = userRepository.findByEmail(email);
            
            if (op.isPresent()) {
                User userByEmail = op.get();
                
                // 檢查該郵箱對應的用戶是否被禁用
                if (userByEmail.getStatus() == User.UserStatus.BANNED) {
                    // 用戶被禁用，重定向到錯誤頁面
                    redirectUrl = FRONTEND_URL + "/login?error=account_banned";
                } else {
                    // 電子郵件已存在且狀態正常，生成臨時令牌並重定向到帳號綁定頁面
                    String token = jwtUtility.createTemporaryLinkToken(googleId, email, name, userByEmail.getUserId());
                    // 在重定向URL中添加頭像參數，確保正確編碼
                    redirectUrl = FRONTEND_URL + "/link-account?token=" + token;
                    if (!encodedPicture.isEmpty()) {
                        redirectUrl += "&picture=" + encodedPicture;
                    }
                }
            } else {
                // 第一次登入，重定向到填寫電話號碼的頁面
                String token = jwtUtility.createTemporaryGoogleToken(googleId, email, name);
                // 在重定向URL中添加頭像參數，確保正確編碼
                redirectUrl = FRONTEND_URL + "/fill-phone?token=" + token;
                if (!encodedPicture.isEmpty()) {
                    redirectUrl += "&picture=" + encodedPicture;
                }
            }
        }
        
        // 重定向到前端應用
        response.sendRedirect(redirectUrl);
    }
    
    // 獲取使用者角色列表
    private List<String> getUserRoles(User user) {
        List<String> roles = new ArrayList<>();
        user.getRole().forEach(role -> roles.add(role.getRoleName()));
        return roles;
    }
    
    // 獲取使用者權限列表
    private List<String> getUserPermissions(User user) {
        return user.getPermissions();
    }
}