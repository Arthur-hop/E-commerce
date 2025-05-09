package ourpkg.campaign.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CampaignCouponDTO {
    private Integer campaignCouponId;
    private Integer campaignId;
    private Integer couponId;
    
    // 優惠券相關信息
    private String couponCode;
    private String couponName;
    private String description;
    private String discountType;
    private BigDecimal discountValue;
    private Date startDate;
    private Date endDate;
    
    // 活動中該優惠券的數量信息
    private Integer totalQuantity;
    private Integer remainingQuantity;
    
    // 是否已被當前用戶領取
    private boolean redeemedByCurrentUser;
}