package ourpkg.user_role_permission;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ourpkg.auth.SignUpResponse;
import ourpkg.auth.util.SignUpValidateUtil;
import ourpkg.user_role_permission.user.User;
import ourpkg.user_role_permission.user.service.UserService;

@RestController
@RequestMapping("/api/admin/profile")
public class AdminProfileController {

    @Autowired
    private UserService userService;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    // 獲取當前管理員的個人資料
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    @GetMapping
    public ResponseEntity<?> getAdminProfile() {
        // 獲取當前登入的用戶
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        User currentUser = userService.findByUserName(username)
            .orElseThrow(() -> new RuntimeException("找不到目前登入的管理員"));

        // 建立回應的 DTO
        AdminProfileDTO profileDTO = new AdminProfileDTO();
        profileDTO.setUserId(currentUser.getUserId());
        profileDTO.setUserName(currentUser.getUserName());
        profileDTO.setEmail(currentUser.getEmail());
        profileDTO.setPhone(currentUser.getPhone());
        
        // 設置頭像URL，如果沒有則使用管理員預設頭像
        profileDTO.setProfilePhotoUrl(AdminPhotoUtil.getValidPhotoUrl(currentUser.getProfilePhotoUrl()));
        
        // 獲取並設置用戶角色
        List<String> roles = currentUser.getRole().stream()
            .map(Role::getRoleName)
            .collect(Collectors.toList());
        profileDTO.setRoles(roles);

        return ResponseEntity.ok(profileDTO);
    }
    
    // 更新當前管理員的個人資料
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    @PutMapping
    public ResponseEntity<SignUpResponse> updateAdminProfile(@RequestBody AdminProfileUpdateRequest request) {
        // 獲取當前登入的用戶
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        
        User currentUser = userService.findByUserName(username)
            .orElseThrow(() -> new RuntimeException("找不到目前登入的管理員"));
        
        // 驗證輸入
        if (request.getUserName() == null || request.getUserName().isBlank()) {
            return ResponseEntity.badRequest().body(new SignUpResponse(false, "請填寫使用者名稱"));
        }
        
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            return ResponseEntity.badRequest().body(new SignUpResponse(false, "請填寫Email"));
        }
        
        if (request.getPhone() == null || request.getPhone().isBlank()) {
            return ResponseEntity.badRequest().body(new SignUpResponse(false, "請填寫電話號碼"));
        }
        
        if (!SignUpValidateUtil.isValidUsername(request.getUserName())) {
            return ResponseEntity.badRequest().body(new SignUpResponse(false, "名稱長度需介於6-20字，且不得含有非法字元"));
        }
        
        if (!SignUpValidateUtil.isValidEmail(request.getEmail())) {
            return ResponseEntity.badRequest().body(new SignUpResponse(false, "請輸入正確的Email"));
        }
        
        if (!SignUpValidateUtil.isValidTaiwanPhone(request.getPhone())) {
            return ResponseEntity.badRequest().body(new SignUpResponse(false, "請輸入正確電話號碼"));
        }
        
        try {
            // 檢查郵箱是否已被其他用戶使用
            if (!request.getEmail().equals(currentUser.getEmail())) {
                if (userService.existsByEmail(request.getEmail())) {
                    return ResponseEntity.badRequest().body(new SignUpResponse(false, "此Email已被使用"));
                }
            }
            
            // 更新用戶資料
            currentUser.setUserName(request.getUserName());
            currentUser.setEmail(request.getEmail());
            currentUser.setPhone(request.getPhone());
            
            // 如果請求中包含頭像URL，則更新頭像 (排除默認頭像的情況)
            if (request.getProfilePhotoUrl() != null && 
                !AdminPhotoUtil.isDefaultPhoto(request.getProfilePhotoUrl())) {
                currentUser.setProfilePhotoUrl(request.getProfilePhotoUrl());
            }
            
            userService.updateAdmin(currentUser);
            
            return ResponseEntity.ok(new SignUpResponse(true, "個人資料更新成功"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new SignUpResponse(false, "系統錯誤：" + e.getMessage()));
        }
    }
    
    // 更新當前管理員的密碼
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    @PutMapping("/password")
    public ResponseEntity<SignUpResponse> updateAdminPassword(@RequestBody AdminPasswordUpdateRequest request) {
        // 獲取當前登入的用戶
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        
        User currentUser = userService.findByUserName(username)
            .orElseThrow(() -> new RuntimeException("找不到目前登入的管理員"));
        
        // 驗證輸入
        if (request.getCurrentPassword() == null || request.getCurrentPassword().isBlank()) {
            return ResponseEntity.badRequest().body(new SignUpResponse(false, "請輸入當前密碼"));
        }
        
        if (request.getNewPassword() == null || request.getNewPassword().isBlank()) {
            return ResponseEntity.badRequest().body(new SignUpResponse(false, "請輸入新密碼"));
        }
        
        if (!SignUpValidateUtil.isValidPassword(request.getNewPassword())) {
            return ResponseEntity.badRequest().body(new SignUpResponse(false, "密碼須包含至少一個大小寫字母，且長度大於8個字"));
        }
        
        // 檢查當前密碼是否正確
        if (!passwordEncoder.matches(request.getCurrentPassword(), currentUser.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new SignUpResponse(false, "當前密碼不正確"));
        }
        
        try {
            // 更新密碼
            currentUser.setPassword(passwordEncoder.encode(request.getNewPassword()));
            userService.updateAdmin(currentUser);
            
            return ResponseEntity.ok(new SignUpResponse(true, "密碼更新成功"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new SignUpResponse(false, "系統錯誤：" + e.getMessage()));
        }
    }
    
    // 只更新管理員頭像
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    @PutMapping("/photo")
    public ResponseEntity<SignUpResponse> updateAdminProfilePhoto(@RequestBody AdminProfilePhotoUpdateRequest request) {
        // 獲取當前登入的用戶
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        
        User currentUser = userService.findByUserName(username)
            .orElseThrow(() -> new RuntimeException("找不到目前登入的管理員"));
        
        try {
            // 更新頭像URL (如果是空值，則設為預設頭像)
            String photoUrl = request.getProfilePhotoUrl();
            if (photoUrl == null || photoUrl.trim().isEmpty()) {
                photoUrl = AdminPhotoUtil.DEFAULT_ADMIN_PHOTO;
            }
            
            currentUser.setProfilePhotoUrl(photoUrl);
            userService.updateAdmin(currentUser);
            
            return ResponseEntity.ok(new SignUpResponse(true, "頭像更新成功"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new SignUpResponse(false, "系統錯誤：" + e.getMessage()));
        }
    }
}