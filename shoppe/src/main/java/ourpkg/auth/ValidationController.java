//package ourpkg.auth;
//
//import java.util.HashMap;
//
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RequestParam;
//import org.springframework.web.bind.annotation.RestController;
//
//import ourpkg.auth.util.SignUpValidateUtil;
//
//@RestController
//@RequestMapping("/api/validate")
//public class ValidationController {
//
//    @GetMapping("/post")
//    public ResponseEntity<?> validateUsername(@RequestParam String username) {
//
//        if (username == null || username.isBlank()) {
//            response.put("valid", false);
//            response.put("message", "使用者名稱不能為空");
//        } else if (!SignUpValidateUtil.isValidUsername(username)) {
//            response.put("valid", false);
//            response.put("message", "使用者名稱只能包含英文字母、數字、底線 (_) 和減號 (-)，長度須為 6-20 字");
//        } else {
//            response.put("valid", true);
//            response.put("message", "使用者名稱可使用");
//        }
//
//        return ResponseEntity.ok(response);
//    }
//}