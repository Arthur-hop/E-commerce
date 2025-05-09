
//package ourpkg.exception;
//
//import java.util.stream.Collectors;
//
//import org.springframework.http.HttpStatus;

package ourpkg.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import jakarta.servlet.http.HttpServletRequest;
import ourpkg.config.ApiResponse;

/**
 * 全局異常處理器
 * 處理應用程序中的各種異常並提供統一的回應格式
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 處理授權相關的異常
     * 當用戶嘗試訪問沒有權限的資源時觸發
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(AccessDeniedException ex) {
        System.err.println("權限錯誤: " + ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiResponse<>(403, "error", "您沒有權限訪問此資源", null));
    }

    /**
     * 處理一般業務邏輯異常
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleRuntimeException(RuntimeException ex) {
        System.err.println("業務邏輯錯誤: " + ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse<>(400, "error", ex.getMessage(), null));
    }

    /**
     * 處理所有未預期的異常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGenericException(Exception ex, HttpServletRequest request) {
        String uri = request.getRequestURI();

        if (uri.contains("/payment/notify")) {
            System.err.println("處理 ECPay 通知時發生未預期錯誤: " + ex.getMessage());

            return ResponseEntity
                    .status(HttpStatus.OK)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("0|Error: " + ex.getMessage());
        }

        // 其他路徑 → 回傳 JSON
        System.err.println("發生未處理異常: " + ex.getMessage());
        ex.printStackTrace();

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_JSON) // 強制 JSON 回應格式
                .body(new ApiResponse<>(500, "error", "處理請求時出現系統錯誤: " + ex.getMessage(), null));
    }


}

