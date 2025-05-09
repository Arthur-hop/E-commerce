package ourpkg.customerService.config;

import java.text.ParseException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import ourpkg.jwt.JsonWebTokenUtility;

@Component 
public class AuthChannelInterceptor implements ChannelInterceptor{
	private static final Logger log = LoggerFactory.getLogger(AuthChannelInterceptor.class);



    private final JsonWebTokenUtility jsonWebTokenUtility; // <--- 注入 JsonWebTokenUtility
    private final UserDetailsService userDetailsService; // <--- 注入 UserDetailsService

    // 使用建構函數注入依賴
    public AuthChannelInterceptor(JsonWebTokenUtility jsonWebTokenUtility, UserDetailsService userDetailsService) {
        this.jsonWebTokenUtility = jsonWebTokenUtility;
        this.userDetailsService = userDetailsService;
        log.info(">>> AuthChannelInterceptor Bean CREATED <<<");
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
    	 log.error("!!!!!!!! AuthChannelInterceptor preSend CALLED !!!!!!!!"); // 使用 ERROR 級別確保輸出
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        // 只處理 CONNECT 指令
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {

            // --- 開始詳細日誌記錄 ---
            String sessionId = accessor.getSessionId();
            log.error("========== [CONNECT-{}] START Interceptor Processing ==========", sessionId);
            log.error(">>> [CONNECT-{}] WebSocket CONNECT 指令收到。", sessionId);

            String authHeader = accessor.getFirstNativeHeader("Authorization");
            log.info(">>> [CONNECT-{}] Received Authorization Header: {}", sessionId, authHeader); // 使用 INFO
            String jwt = null;

            if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
                jwt = authHeader.substring(7);
                log.info(">>> [CONNECT-{}] Extracted JWT from Header.", sessionId); // 使用 INFO

                // --- 嘗試解析 JWT Payload ---
                try {
                    // **假設使用 nimbus-jose-jwt 庫**
                    SignedJWT signedJWT = SignedJWT.parse(jwt);
                    JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
                    Map<String, Object> claimsMap = claims.getClaims();
                    log.info(">>> [CONNECT-{}] DECODED JWT Payload Claims: {}", sessionId, claimsMap); // 記錄所有 Claims
                    Object userIdClaim = claims.getClaim("userId");
                    String subjectClaim = claims.getSubject();
                    log.info(">>> [CONNECT-{}] DECODED JWT userId Claim: {} (Type: {})", sessionId, userIdClaim, (userIdClaim != null ? userIdClaim.getClass().getName() : "N/A"));
                    log.info(">>> [CONNECT-{}] DECODED JWT Subject (username) Claim: {}", sessionId, subjectClaim);
                } catch (ParseException e) {
                    log.error(">>> [CONNECT-{}] FAILED to parse JWT: {}", sessionId, e.getMessage());
                } catch (Exception e) {
                     log.error(">>> [CONNECT-{}] UNEXPECTED error during JWT decoding: {}", sessionId, e.getMessage(), e);
                }
                // --- 解析記錄結束 ---

            } else {
                log.warn(">>> [CONNECT-{}] WebSocket CONNECT missing Authorization Bearer Token header.", sessionId);
            }

            if (StringUtils.hasText(jwt)) {
                Integer authenticatedUserId = null;
                String username = null;

                try {
                    // 1. 驗證 Token
                    log.debug(">>> [CONNECT-{}] Validating JWT with JsonWebTokenUtility...", sessionId);
                    JWTClaimsSet claimsSet = jsonWebTokenUtility.validateToken(jwt);

                    // 2. 檢查驗證結果
                    if (claimsSet != null) {
                        log.info(">>> [CONNECT-{}] JWT validation successful.", sessionId);
                        log.debug(">>> [CONNECT-{}] Validated ClaimsSet: {}", sessionId, claimsSet.getClaims());

                        // 3. 提取 userId
                        try {
                            // **再次確認 JWT 中 userId 的 Claim 名稱和類型**
                            authenticatedUserId = claimsSet.getIntegerClaim("userId"); // 假設是 Integer
                            log.info(">>> [CONNECT-{}] Extracted userId from ClaimsSet: {}", sessionId, authenticatedUserId); // 使用 INFO
                        } catch (Exception e) {
                            log.error(">>> [CONNECT-{}] FAILED to extract 'userId' (Integer) from ClaimsSet: {}", sessionId, e.getMessage());
                        }

                        if (authenticatedUserId != null) {
                            // 4. **【關鍵】設置 Session Attribute**
                            accessor.getSessionAttributes().put("userId", authenticatedUserId);
                            // ** 立刻讀回來確認 **
                            Object userIdInSession = accessor.getSessionAttributes().get("userId");
                            log.info(">>> [CONNECT-{}] *** Set session attribute 'userId' to: {}. Read back value: {} ***", sessionId, authenticatedUserId, userIdInSession); // 使用 INFO

                            // 5. 設置 Principal
                            try {
                                username = claimsSet.getSubject();
                                log.debug(">>> [CONNECT-{}] Attempting to set Principal using Subject (username): {}", sessionId, username);
                                if (username != null) {
                                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                                    Authentication authentication = new UsernamePasswordAuthenticationToken(
                                        userDetails, null, userDetails.getAuthorities());
                                    accessor.setUser(authentication);
                                    log.info(">>> [CONNECT-{}] *** Set Spring Security Principal for user: '{}' (ID: {}) ***", sessionId, authentication.getName(), authenticatedUserId); // 使用 INFO
                                } else {
                                    log.warn(">>> [CONNECT-{}] Username (subject) not found in JWT claims, cannot set Principal.", sessionId);
                                }
                            } catch (Exception ex) {
                                log.error(">>> [CONNECT-{}] Error loading UserDetails or setting Principal for username '{}': {}", sessionId, username, ex.getMessage(), ex);
                            }

                        } else { // authenticatedUserId == null
                            log.error(">>> [CONNECT-{}] Could not extract valid 'userId' claim from token. Rejecting connection.", sessionId);
                            return null;
                        }

                    } else { // claimsSet == null
                        log.warn(">>> [CONNECT-{}] Invalid JWT received (validation failed or expired). Rejecting connection.", sessionId);
                        return null;
                    }
                } catch (Exception e) {
                    log.error(">>> [CONNECT-{}] Error processing JWT: {}", sessionId, e.getMessage(), e);
                    return null;
                }
            } else { // jwt 為空
                log.info(">>> [CONNECT-{}] No JWT found. Handling as anonymous or rejecting.", sessionId);
                // return null; // 如果不允許匿名
            }
            log.info("========== [CONNECT-{}] END Interceptor Processing ==========", sessionId);
        }
        return message;
    }
}
