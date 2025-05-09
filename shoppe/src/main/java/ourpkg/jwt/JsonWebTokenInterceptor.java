package ourpkg.jwt;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.nimbusds.jwt.JWTClaimsSet;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import ourpkg.user_role_permission.user.User;
import ourpkg.user_role_permission.user.UserDetailsServiceImpl;

@Component
public class JsonWebTokenInterceptor extends OncePerRequestFilter {
    private final JsonWebTokenUtility jsonWebTokenUtility;
    private final UserDetailsServiceImpl userDetailsService;

    public JsonWebTokenInterceptor(JsonWebTokenUtility jsonWebTokenUtility, UserDetailsServiceImpl userDetailsService) {
        this.jsonWebTokenUtility = jsonWebTokenUtility;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
    	
    	 String uri = request.getRequestURI();

    	// ✅ 放行 ECPay 的回傳門市資料（避免被 JWT 攔住）
    	 if (uri.startsWith("/api/ecpay/cvs-reply")) {
    	     chain.doFilter(request, response);
    	     return;
    	 }

        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                chain.doFilter(request, response);
                return;
            }
            
            String token = authHeader.substring(7);
            JWTClaimsSet claimsSet = jsonWebTokenUtility.validateToken(token);
            
            if (claimsSet != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                String username = claimsSet.getSubject();
                
                // 嘗試從數據庫加載用戶（方法二）
                try {
                    // 使用 UserDetailsService 載入完整的用戶資訊 (在@Transactional內執行)
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                    
                    // 建立認證物件，使用從資料庫載入的UserDetails，包含最新的權限
                    JsonWebTokenAuthentication authentication = new JsonWebTokenAuthentication(userDetails, token);
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    
                    System.out.println("JWT 認證成功 (方法二)，用戶: " + username + ", 權限: " + userDetails.getAuthorities());
                } catch (Exception ex) {
                    // 如果數據庫加載失敗，回退到從JWT中提取權限（方法一）
                    System.out.println("方法二失敗，切換到方法一: " + ex.getMessage());
                    
                    // 從 JWT Claims 直接獲取角色和權限
                    List<String> roles = claimsSet.getStringListClaim("roles");
                    List<String> permissions = claimsSet.getStringListClaim("permissions");
                    
                    // 轉換為 Spring Security 的 Authority 格式
                    List<GrantedAuthority> authorities = new ArrayList<>();
                    
                    if (roles != null) {
                        for (String role : roles) {
                            authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
                        }
                    }
                    
                    if (permissions != null) {
                        for (String permission : permissions) {
                            authorities.add(new SimpleGrantedAuthority(permission));
                        }
                    }
                    
                    // 创建一個簡單的 UserDetails 物件
                    org.springframework.security.core.userdetails.User userDetails = 
                        new org.springframework.security.core.userdetails.User(
                            username, "", authorities);
                    
                    // 創建認證物件
                    JsonWebTokenAuthentication authentication = 
                        new JsonWebTokenAuthentication(userDetails, token);
                    
                    authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request));
                    
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    
                    System.out.println("JWT 認證成功 (方法一)，用戶: " + username);
                }
            }
            
            chain.doFilter(request, response);
        } catch (Exception e) {
            System.err.println("JWT 認證過程中發生錯誤: " + e.getMessage());
            e.printStackTrace();
            chain.doFilter(request, response);
        }
    }
}