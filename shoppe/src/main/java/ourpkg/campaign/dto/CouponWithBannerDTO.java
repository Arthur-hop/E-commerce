package ourpkg.campaign.dto;

import java.math.BigDecimal;
import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CouponWithBannerDTO {
	  private Integer couponId;
	    private String couponName;
	    private String couponCode;
	    private String discountType;
	    private BigDecimal discountValue;
	    private Date endDate;
	    private String description;
	    private String status;       // ACTIVE / USED / EXPIRED
	    private String bannerImage;  // 來自活動活動圖片
}