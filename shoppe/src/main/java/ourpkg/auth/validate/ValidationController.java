package ourpkg.auth.validate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ourpkg.auth.AuthService;
import ourpkg.auth.util.SignUpValidateUtil;

@RestController
@RequestMapping("/api/validate")
public class ValidationController {

    @Autowired
    private AuthService authService; // 假設您有一個用戶服務用於檢查是否存在

    /**
     * 驗證用戶名是否可用
     */
    @GetMapping("/username")
    public ResponseEntity<?> validateUsername(@RequestParam String username) {
        // 1. 檢查格式是否合法
        if (!SignUpValidateUtil.isValidUsername(username)) {
            return ResponseEntity.ok(new ValidationResponse(
                false, 
                "名稱長度需介於6-20字，且不得含有非法字元"
            ));
        }
        
        // 2. 檢查是否已存在
        boolean isAvailable = !authService.existsByUsername(username);
        
        return ResponseEntity.ok(new ValidationResponse(
            isAvailable,
            isAvailable ? "用戶名可用" : "此用戶名已被使用"
        ));
    }

    /**
     * 驗證電子郵件是否可用
     */
    @GetMapping("/email")
    public ResponseEntity<?> validateEmail(@RequestParam String email) {
        // 1. 檢查格式是否合法
        if (!SignUpValidateUtil.isValidEmail(email)) {
            return ResponseEntity.ok(new ValidationResponse(
                false, 
                "請輸入正確的E-mail"
            ));
        }
        
        // 2. 檢查是否已存在
        boolean isAvailable = !authService.existsByEmail(email);
        
        return ResponseEntity.ok(new ValidationResponse(
            isAvailable,
            isAvailable ? "電子郵件可用" : "此電子郵件已註冊"
        ));
    }

    /**
     * 驗證電話號碼是否可用
     */
    @GetMapping("/phone")
    public ResponseEntity<?> validatePhone(@RequestParam String phone) {
        // 1. 檢查格式是否合法
        if (!SignUpValidateUtil.isValidTaiwanPhone(phone)) {
            return ResponseEntity.ok(new ValidationResponse(
                false, 
                "請輸入正確電話號碼"
            ));
        }
        
        // 2. 檢查是否已存在
        boolean isAvailable = !authService.existsByPhone(phone);
        
        return ResponseEntity.ok(new ValidationResponse(
            isAvailable,
            isAvailable ? "手機號碼可用" : "此手機號碼已註冊"
        ));
    }
}
