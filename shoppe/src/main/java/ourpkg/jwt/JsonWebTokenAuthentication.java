package ourpkg.jwt;


import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

public class JsonWebTokenAuthentication extends AbstractAuthenticationToken {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final UserDetails principal; // 使用者資訊
    private final String token; // JWT Token

    public JsonWebTokenAuthentication(UserDetails principal, String token) {
        super(principal.getAuthorities());
        this.principal = principal;
        this.token = token;
        setAuthenticated(true); // 設為已驗證
    }

    @Override
    public Object getCredentials() {
        return token;
    }

    @Override
    public Object getPrincipal() {
        return principal;
    }
}
