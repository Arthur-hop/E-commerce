package ourpkg.auth;

import java.util.HashSet;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ourpkg.user_role_permission.Role;
import ourpkg.user_role_permission.RoleService;
import ourpkg.user_role_permission.user.User;
import ourpkg.user_role_permission.user.UserRepository;

@Service
@Transactional
public class AuthService {
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepo;
    private final RoleService roleService;
    private final AuthenticationManager authenticationManager;
    
    @Autowired
    public AuthService(PasswordEncoder passwordEncoder, UserRepository userRepo, 
                      RoleService roleService, AuthenticationManager authenticationManager) {
        this.passwordEncoder = passwordEncoder;
        this.userRepo = userRepo;
        this.roleService = roleService;
        this.authenticationManager = authenticationManager;
    }

    public SignUpResponse insertUser(String username, String email, String password, String phone) {
        System.out.println(username);
        System.out.println(email);
        System.out.println(password);
        System.out.println(phone);
        
        // 檢查 email 是否已被註冊
        if (userRepo.findByEmail(email).isPresent()) {
            return new SignUpResponse(false,"Email已被註冊");
        }
        // 檢查 userName 是否已被使用
        if (userRepo.findByUserName(username).isPresent()) {
            return new SignUpResponse(false,"此用戶名稱已被使用");
        }
        // 檢查 phone 是否已被使用
        if (userRepo.findByPhone(phone).isPresent()) {
            return new SignUpResponse(false,"此電話號碼已被使用");
        }
        User newUser = new User();
        newUser.setUserName(username);
        newUser.setEmail(email);
        newUser.setPhone(phone);
        String encodedPassword = passwordEncoder.encode(password);
        newUser.setPassword(encodedPassword);
        newUser.setStatus(User.UserStatus.ACTIVE); // 確保用戶被激活
        
        Role defaultRole = roleService.findByRoleName("USER");
        if (defaultRole == null) {
            return new SignUpResponse(false,"註冊失敗，請聯絡客服");
        }
        // 初始化role值避免null
        if (newUser.getRole() == null) {
            newUser.setRole(new HashSet<>());
        }
        newUser.getRole().add(defaultRole);
        userRepo.save(newUser);
        return new SignUpResponse(true,"註冊成功");
    }
    
    public AuthResult loginUserWithResult(String username, String password) {
        try {
            // 先查詢用戶是否存在，不進行身份驗證
            Optional<User> userOpt = userRepo.findByUserName(username);
            if (userOpt.isEmpty()) {
                return AuthResult.error("INVALID_CREDENTIALS");
            }

            // 檢查帳號狀態
            User user = userOpt.get();
            if (user.getStatus() == User.UserStatus.BANNED) {
                return AuthResult.error("ACCOUNT_BANNED");
            }

            // 使用 Spring Security 的 AuthenticationManager 進行認證
            try {
                Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password)
                );
                
                if (authentication.isAuthenticated()) {
                    return AuthResult.success((User) authentication.getPrincipal());
                } else {
                    return AuthResult.error("INVALID_CREDENTIALS");
                }
            } catch (BadCredentialsException e) {
                return AuthResult.error("INVALID_CREDENTIALS");
            } catch (AuthenticationException e) {
                // 其他認證異常
                System.err.println("認證失敗: " + e.getMessage());
                return AuthResult.error("AUTH_FAILED");
            }
        } catch (Exception e) {
            System.err.println("登入過程中發生錯誤: " + e.getMessage());
            return AuthResult.error("SYSTEM_ERROR");
        }
    }

    // 保留原有方法以維持兼容性
    public User loginUser(String username, String password) {
        AuthResult result = loginUserWithResult(username, password);
        return result.isSuccess() ? result.getUser() : null;
    }
    
    /**
     * 通過電子郵件查找使用者
     */
    public Optional<User> findUserByEmail(String email) {
        return userRepo.findByEmail(email);
    }
    
    public boolean existsByEmail(String email) {
         Optional<User> optional = userRepo.findByEmail(email);
         if(optional.isPresent()) {
            return true;
         }
         
         return false;
    }
    
    public boolean existsByUsername(String UserName) {
        Optional<User> optional = userRepo.findByUserName(UserName);
         if(optional.isPresent()) {
            return true;
         }
         
         return false;
    }
    
    public boolean existsByPhone(String phone) {
        Optional<User> optional = userRepo.findByPhone(phone);
         if(optional.isPresent()) {
            return true;
         }
         
         return false;
    }
    
    /**
     * 插入一個新的 Google 用戶
     */
    public SignUpResponse insertGoogleUser(String username, String email, String password, String phone, String googleId) {
        // 檢查用戶名和電子郵件是否已存在
        if (userRepo.findByUserName(username).isPresent()) {
            return new SignUpResponse(false, "使用者名稱已被使用");
        }
        
        if (userRepo.findByEmail(email).isPresent()) {
            return new SignUpResponse(false, "電子郵件已被使用");
        }
        
        // 創建新的用戶
        User user = new User();
        user.setUserName(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setPhone(phone);
        user.setGoogleId(googleId);
        user.setStatus(User.UserStatus.ACTIVE); // 確保用戶被激活
        
        // 設置角色為 USER
        Role userRole = roleService.findByRoleName("USER");
        if (userRole == null) {
            return new SignUpResponse(false, "註冊失敗，請聯絡客服");
        }
        
        // 初始化role集合
        if (user.getRole() == null) {
            user.setRole(new HashSet<>());
        }
        user.getRole().add(userRole);
        
        // 保存用戶
        userRepo.save(user);
        
        return new SignUpResponse(true, "Google 用戶註冊成功");
    }
}