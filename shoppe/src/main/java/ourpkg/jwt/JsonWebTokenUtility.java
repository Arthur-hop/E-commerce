package ourpkg.jwt;

import java.security.SecureRandom;
import java.text.ParseException;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import jakarta.annotation.PostConstruct;

@Component
public class JsonWebTokenUtility {
    @Value("${jwt.token.expire}")
    private long expire;
    
    @Value("${jwt.reset-token.expire}") // 新增一個專門給 Reset Password Token 用的過期時間
    private long resetExpire;
    
    private String issuer = "shopee";
    private byte[] sharedKey; // 用在簽章
    
    
    @Value("${jwt.secret}") // 從 application.properties 讀取固定金鑰
    private String secretKey;
    
    @PostConstruct
    public void init() {
        // 使用 application.properties 內的金鑰，確保程式重啟時不會變動
        sharedKey = secretKey.getBytes();
    }

//    @PostConstruct
//    public void init() {
//        // 需要長度是512-bit的金鑰以便使用HS512演算法
//        sharedKey = new byte[64];
//
//        // 使用安全隨機數產生金鑰
//        SecureRandom secureRandom = new SecureRandom();
//        secureRandom.nextBytes(sharedKey);
//    }

    /**
     * 產生一般登入 Token
     */
    public String createToken(String username, List<String> roles, List<String> permissions, Integer userId) {
        return generateToken(username, userId, roles, permissions, expire, "auth");
    }

    /**
     * 產生 Reset Password Token
     */
    public String createResetPasswordToken(String email) {
        return generateToken(email, null, null, null, resetExpire, "reset_password");
    }

    /**
     * 產生 JWT
     */
    private String generateToken(String subject, Integer userId, List<String> roles, List<String> permissions, long expiryMinutes, String type) {
        Instant now = Instant.now();
        Instant expireTime = now.plusSeconds(expiryMinutes * 60);
        try {
            // 建立HMAC signer
            JWSSigner signer = new MACSigner(sharedKey);

            // 準備 JWT 主體
            JWTClaimsSet.Builder claimsBuilder = new JWTClaimsSet.Builder()
                    .issuer(issuer)
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(expireTime))
                    .subject(subject)
                    .claim("type", type); // 加入 token 類型標記

            if (userId != null) {
                claimsBuilder.claim("userId", userId);
            }
            if (roles != null) {
                claimsBuilder.claim("roles", roles);
            }
            if (permissions != null) {
                claimsBuilder.claim("permissions", permissions);
            }

            // 建立 HMAC 保護的 JWT
            SignedJWT signedJWT = new SignedJWT(
                    new JWSHeader(JWSAlgorithm.HS512),
                    claimsBuilder.build());
            signedJWT.sign(signer);

            return signedJWT.serialize();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 驗證 JWT
     */
    public JWTClaimsSet validateToken(String token) {
        try {
            JWSVerifier verifier = new MACVerifier(sharedKey);
            SignedJWT signedJWT = SignedJWT.parse(token);
            JWTClaimsSet claimsSet = signedJWT.getJWTClaimsSet();

            if (signedJWT.verify(verifier) && new Date().before(claimsSet.getExpirationTime())) {
                return claimsSet;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 檢查是否為有效的 Reset Password Token
     */
    public boolean isResetPasswordTokenValid(String token) {
        JWTClaimsSet claims = validateToken(token);
        try {
			return claims != null && "reset_password".equals(claims.getStringClaim("type"));
		} catch (ParseException e) {
			e.printStackTrace();
			return false;
		}
    }
    
    
    /**
     * 產生臨時令牌用於 Google 登入流程
     */
    public String createTemporaryGoogleToken(String googleId, String email, String name) {
        // 使用較短的過期時間，例如 30 分鐘
        long googleTokenExpire = 30; // 30 分鐘
        
        // 使用 Map 來儲存 Google 用戶資訊
        java.util.Map<String, Object> googleInfo = new java.util.HashMap<>();
        googleInfo.put("googleId", googleId);
        googleInfo.put("email", email);
        googleInfo.put("name", name);
        googleInfo.put("isNewUser", true);
        
        // 呼叫現有的 generateToken 方法
        return generateToken(email, null, null, null, googleTokenExpire, "google_auth", googleInfo);
    }

    /**
     * 產生 JWT (擴展版本，支援額外的 claims)
     */
    private String generateToken(String subject, Integer userId, List<String> roles, List<String> permissions, 
                                 long expiryMinutes, String type, java.util.Map<String, Object> additionalClaims) {
        Instant now = Instant.now();
        Instant expireTime = now.plusSeconds(expiryMinutes * 60);
        try {
            // 建立HMAC signer
            JWSSigner signer = new MACSigner(sharedKey);

            // 準備 JWT 主體
            JWTClaimsSet.Builder claimsBuilder = new JWTClaimsSet.Builder()
                    .issuer(issuer)
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(expireTime))
                    .subject(subject)
                    .claim("type", type); // 加入 token 類型標記

            if (userId != null) {
                claimsBuilder.claim("userId", userId);
            }
            if (roles != null) {
                claimsBuilder.claim("roles", roles);
            }
            if (permissions != null) {
                claimsBuilder.claim("permissions", permissions);
            }
            
            // 加入額外的 claims
            if (additionalClaims != null) {
                for (java.util.Map.Entry<String, Object> entry : additionalClaims.entrySet()) {
                    claimsBuilder.claim(entry.getKey(), entry.getValue());
                }
            }

            // 建立 HMAC 保護的 JWT
            SignedJWT signedJWT = new SignedJWT(
                    new JWSHeader(JWSAlgorithm.HS512),
                    claimsBuilder.build());
            signedJWT.sign(signer);

            return signedJWT.serialize();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    
    /**
     * 產生臨時令牌用於帳號連結流程
     * 當使用者用Google登入，但系統發現有相同Email的既有帳號時使用
     */
    public String createTemporaryLinkToken(String googleId, String email, String name, Integer existingUserId) {
        // 使用較短的過期時間，例如 30 分鐘
        long linkTokenExpire = 30; // 30 分鐘
        
        // 使用 Map 來儲存連結資訊
        java.util.Map<String, Object> linkInfo = new java.util.HashMap<>();
        linkInfo.put("googleId", googleId);
        linkInfo.put("email", email);
        linkInfo.put("name", name);
        linkInfo.put("existingUserId", existingUserId);
        linkInfo.put("isTemporaryLinkToken", true);
        
        // 呼叫現有的 generateToken 方法
        return generateToken(email, null, null, null, linkTokenExpire, "account_link", linkInfo);
    }
}
