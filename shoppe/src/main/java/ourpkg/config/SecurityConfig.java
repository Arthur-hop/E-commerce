package ourpkg.config;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import ourpkg.auth.google.GoogleOAuth2SuccessHandler;
import ourpkg.jwt.JsonWebTokenInterceptor;
import ourpkg.user_role_permission.user.UserDetailsServiceImpl;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    private final JsonWebTokenInterceptor jwtInterceptor;
    private final GoogleOAuth2SuccessHandler googleOAuth2SuccessHandler;
    private final UserDetailsServiceImpl userDetailsService;
    private final PasswordEncoder passwordEncoder;

    public SecurityConfig(JsonWebTokenInterceptor jwtInterceptor,
            GoogleOAuth2SuccessHandler googleOAuth2SuccessHandler,
            UserDetailsServiceImpl userDetailsService,
            PasswordEncoder passwordEncoder) {
        this.jwtInterceptor = jwtInterceptor;
        this.googleOAuth2SuccessHandler = googleOAuth2SuccessHandler;
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // ✅ 開放給所有人訪問的路徑
                        .requestMatchers(
                                "/api/auth/**",
                                "/ws/**",
                                "/api/shop/**",
                                "/fill-phone",
                                "/login/oauth2/success",
                                "/api/validate/**",
                                "/oauth2/**",
                                "/uploads/**",
                                "/api/category1/**",
                                "/api/category2/**",
                                "/api/product/**", // 不需要重複寫
                                "/api/payment/**",
                                "/api/payment/orders/**",
                                "/api/payment/redirect/**",
                                "/api/payment/notify/**",
                                "/api/orders/**",
                                "/api/orders/check-payment/**",
                                "/", "/index.html", "/favicon.ico",
                                "/css/**", "/js/**", "/images/**", "/assets/**",
                                "/api/payment/orders/actions/create", "/api/ecpay/**", "/api/review/**",
                                "/api/user/address/**"
                                )
                        .permitAll()

                        // ✅ 需要授權的路徑
                        .requestMatchers("/api/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                        .requestMatchers("/api/user/**").hasAnyRole("USER", "ADMIN", "SUPER_ADMIN")
                        .requestMatchers("/api/seller/**").hasAnyRole("SELLER", "ADMIN", "SUPER_ADMIN")
                        .requestMatchers("/api/orders/seller/**").hasAnyRole("SELLER", "ADMIN", "SUPER_ADMIN")
                        .requestMatchers("/api/orders/user/**").hasAnyRole("USER", "ADMIN", "SUPER_ADMIN")
                        .requestMatchers("/api/cart/**").hasAnyRole("USER", "ADMIN", "SUPER_ADMIN")

                        // ✅ 其他所有請求都要驗證
                        .anyRequest().authenticated())
                .oauth2Login(oauth2 -> oauth2.successHandler(googleOAuth2SuccessHandler))
                .logout(logout -> logout.logoutSuccessUrl("/").permitAll())
                .headers(headers -> headers.frameOptions(frame -> frame.disable()).contentSecurityPolicy(csp -> csp
                        .policyDirectives("default-src 'self'; " +
                                "script-src 'self' 'unsafe-inline' 'unsafe-eval' " +
                                "https://payment-stage.ecpay.com.tw https://payment.ecpay.com.tw " +
                                "https://gpayment-stage.ecpay.com.tw https://*.ecpay.com.tw " +
                                "https://*.googletagmanager.com https://www.googleadservices.com " +
                                "https://googleads.g.doubleclick.net https://www.google.com.tw " +
                                "https://*.google-analytics.com https://analytics.google.com " +
                                "https://payments.developers.google.com https://connect.facebook.net " +
                                "https://*.clarity.ms https://*.bing.com; " +
                                "frame-src 'self' https://payment-stage.ecpay.com.tw https://payment.ecpay.com.tw " +
                                "https://gpayment-stage.ecpay.com.tw; " +
                                "form-action 'self' https://payment-stage.ecpay.com.tw https://payment.ecpay.com.tw " +
                                "https://gpayment-stage.ecpay.com.tw https://*.ecpay.com.tw; " +
                                "connect-src 'self' https://payment-stage.ecpay.com.tw https://gpayment-stage.ecpay.com.tw "
                                +
                                "https://payment.ecpay.com.tw https://*.ecpay.com.tw; " +
                                "img-src 'self' data: https://*.ecpay.com.tw; " +
                                "style-src 'self' 'unsafe-inline';")))
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtInterceptor, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:5173", // 允許的前端網址
                "https://logistics-stage.ecpay.com.tw"));// ✅ 綠界的地圖頁 Origin
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true); // ✅ 允許攜帶 Cookie（像是 JWT）
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}