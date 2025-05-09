package ourpkg.product.version2.controller_service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * 圖片文件 儲存&刪除 輔助類私有方法
 */
@Service
public class ImageFileService {
    
    private static final Logger logger = Logger.getLogger(ImageFileService.class.getName());
    private final String uploadDir = "./uploads/"; // 圖片儲存路徑
    private static final List<String> ALLOWED_IMAGE_TYPES = Arrays.asList("image/jpeg", "image/png", "image/jpg");

    // 文件儲存類方法 開始========================================================

    /**
     * 儲存單張圖片
     * 
     * @param file 上傳的圖片文件
     * @return 圖片的訪問路徑
     */
    public String saveImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return "/uploads/default-product-image.jpg"; // 提供預設圖片~
        }

        validateImageType(file);

        try {
            ensureUploadDirectoryExists();

            String filename = generateUniqueFilename(file);
            Path filePath = Paths.get(uploadDir, filename);
            
            // 儲存原始圖片 - 修改這裡，使用CREATE而不是CREATE_NEW
            Files.write(filePath, file.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            
            logger.info("成功保存圖片: " + filePath.toString());
            return "/uploads/" + filename;
        } catch (IOException e) {
            logger.severe("圖片上傳失敗: " + e.getMessage());
            throw new RuntimeException("圖片上傳失敗", e);
        } catch (Exception e) {
            logger.severe("處理圖片時發生未預期的錯誤: " + e.getMessage());
            throw new RuntimeException("圖片處理失敗", e);
        }
    }

    /**
     * 批量儲存圖片
     * 
     * @param files 多個上傳的圖片文件
     * @return 圖片訪問路徑列表
     */
    public List<String> saveImages(List<MultipartFile> files) {
        List<String> imagePaths = new ArrayList<>();

        if (files == null || files.isEmpty()) {
            imagePaths.add("/uploads/default-product-image.jpg");
            return imagePaths;
        }

        for (MultipartFile file : files) {
            if (file != null && !file.isEmpty()) {
                String path = saveImage(file);
                imagePaths.add(path);
            }
        }

        return imagePaths;
    }

    // 文件儲存類方法 結束========================================================
    // 文件刪除類方法 開始========================================================

    /**
     * 刪除圖片文件
     * 
     * @param imagePath 圖片路徑
     */
    public void deleteImageFile(String imagePath) {
        if (imagePath == null || !imagePath.startsWith("/uploads/")
                || imagePath.equals("/uploads/default-product-image.jpg")) {
            return; // 排除默認圖片和非法路徑
        }

        Path imageFilePath = Paths.get(uploadDir, new File(imagePath).getName());
        try {
            boolean deleted = Files.deleteIfExists(imageFilePath);
            if (!deleted) {
                logger.warning("圖片文件不存在：" + imagePath);
            }

        } catch (IOException e) {
            logger.severe("刪除圖片失敗: " + imagePath + ", " + e.getMessage());
            throw new RuntimeException("刪除圖片失敗: " + imagePath, e);
        }
    }

    /**
     * 批量刪除圖片
     * 
     * @param imagePaths 圖片路徑列表
     */
    public void deleteImageFiles(List<String> imagePaths) {
        if (imagePaths == null || imagePaths.isEmpty()) {
            return;
        }

        for (String path : imagePaths) {
            deleteImageFile(path);
        }
    }

    // 文件刪除類方法 結束========================================================
    // 輔助類私有方法 開始========================================================

    /**
     * 驗證圖片類型
     * 
     * @param file 上傳的文件
     */
    private void validateImageType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType)) {
            logger.warning("嘗試上傳不支援的圖片格式：" + contentType);
            throw new RuntimeException("不支援的圖片格式：" + contentType);
        }
    }

    /**
     * 確保上傳目錄存在
     */
    private void ensureUploadDirectoryExists() {
        File uploadFolder = new File(uploadDir);
        if (!uploadFolder.exists()) {
            boolean created = uploadFolder.mkdirs();
            if (!created) {
                logger.severe("無法創建上傳目錄：" + uploadDir);
                throw new RuntimeException("無法創建上傳目錄：" + uploadDir);
            }
            logger.info("成功創建上傳目錄：" + uploadDir);
        }
        
        // 檢查目錄是否可寫
        if (!uploadFolder.canWrite()) {
            logger.severe("上傳目錄沒有寫入權限：" + uploadDir);
            throw new RuntimeException("上傳目錄沒有寫入權限：" + uploadDir);
        }
    }

    /**
     * 生成唯一的文件名
     * 
     * @param file 上傳的文件
     * @return 唯一文件名
     */
    private String generateUniqueFilename(MultipartFile file) {
        String originalFilename = Optional.ofNullable(file.getOriginalFilename()).orElse("unknown.jpg");
        // 清理文件名，移除可能的路徑信息
        originalFilename = new File(originalFilename).getName();
        return UUID.randomUUID().toString() + "_" + originalFilename;
    }

    // 輔助類私有方法 結束========================================================
}