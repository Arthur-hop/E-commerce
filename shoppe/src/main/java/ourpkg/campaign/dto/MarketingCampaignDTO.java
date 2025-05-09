package ourpkg.campaign.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MarketingCampaignDTO {
    private Integer campaignId;
    private Integer shopId;
    private String shopName; // 用於前端顯示
    private String campaignName;
    private String description;
    private String bannerImage;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String status; // ACTIVE, INACTIVE, SCHEDULED, ENDED
    private Integer createdById;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer couponCount; // 該活動中的優惠券數量
}