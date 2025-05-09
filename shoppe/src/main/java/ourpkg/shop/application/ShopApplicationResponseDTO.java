package ourpkg.shop.application;

import java.time.LocalDateTime;
import java.util.Date;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ShopApplicationResponseDTO {
    private Integer applicationId;  // 申請單 ID
    private Integer userId;         // 申請人 ID
    private String userName;        // 申請人名稱
    private String shopName;        // 商店名稱
    private String shopCategory;    // 商店類別
    private String status;          // 申請狀態
    private Date createdAt;  // 申請時間
    private Date reviewedAt; // 審核時間（可能為 null）
    private String adminComment;    // 管理員備註（可能為 null）
    private String description;
    private String reviewer;

    public static ShopApplicationResponseDTO fromEntity(ShopApplication app) {
        ShopApplicationResponseDTO dto = new ShopApplicationResponseDTO();
        dto.setApplicationId(app.getApplicationId());
        dto.setUserId(app.getUser().getUserId());
        dto.setUserName(app.getUser().getUsername());  // 假設 `User` 有 `getUsername()`
        dto.setShopName(app.getShopName());
        dto.setShopCategory(app.getShopCategory());
        dto.setStatus(app.getStatus().name()); // ENUM 轉字串
        dto.setCreatedAt(app.getCreatedAt());
        dto.setReviewedAt(app.getReviewedAt());
        dto.setAdminComment(app.getAdminComment());
        dto.setDescription(app.getDescription());
        return dto;
    }
    
    public static ShopApplicationResponseDTO fromEntityWithReviewer(ShopApplication app) {
        ShopApplicationResponseDTO dto = new ShopApplicationResponseDTO();
        dto.setApplicationId(app.getApplicationId());
        dto.setUserId(app.getUser().getUserId());
        dto.setUserName(app.getUser().getUsername());  // 假設 `User` 有 `getUsername()`
        dto.setShopName(app.getShopName());
        dto.setShopCategory(app.getShopCategory());
        dto.setStatus(app.getStatus().name()); // ENUM 轉字串
        dto.setCreatedAt(app.getCreatedAt());
        dto.setReviewedAt(app.getReviewedAt());
        dto.setAdminComment(app.getAdminComment());
        dto.setDescription(app.getDescription());
        dto.setReviewer(app.getReviewer().getUserName());
        return dto;
    }
}

