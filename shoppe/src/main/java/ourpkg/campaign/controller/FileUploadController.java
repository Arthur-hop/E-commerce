package ourpkg.campaign.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import ourpkg.campaign.ApiResponse;
import ourpkg.campaign.service.FileStorageService;

@RestController
@RequestMapping("/api/files")
public class FileUploadController {

    @Autowired
    private FileStorageService fileStorageService;

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse(false, "請選擇一個檔案上傳"));
            }

            // 檢查檔案類型
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse(false, "只允許上傳圖片檔案"));
            }

            String fileName = fileStorageService.storeFile(file);
            return ResponseEntity.ok(
                    new ApiResponse(true, "檔案上傳成功", fileName));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "檔案上傳失敗: " + e.getMessage()));
        }
    }
}