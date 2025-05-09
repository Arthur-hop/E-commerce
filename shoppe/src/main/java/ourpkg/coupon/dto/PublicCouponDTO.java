package ourpkg.coupon.dto;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class PublicCouponDTO {
	private Integer couponId;
	private String couponName;
	private String description;
	private String discountType;
	private BigDecimal discountValue;
	private String startDate; // 使用 String 格式化日期
	private String endDate; // 使用 String 格式化日期
	private Integer shopId; // 可選：如果需要顯示商店資訊
	private String shopName; // 可選
}
