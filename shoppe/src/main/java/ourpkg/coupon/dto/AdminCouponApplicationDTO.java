package ourpkg.coupon.dto;

import lombok.Data;

@Data
public class AdminCouponApplicationDTO {
	private Integer applicationId;
	private String applicationType; // CREATE, UPDATE, DELETE
	private Integer requestedBySellerId;
	private String requestedBySellerName; // Include seller name
	private Integer requestedShopId;
	private String requestedShopName; // Include shop name
	private Integer targetCouponId;
	private String status; // PENDING_CREATE, etc.
	private String applicationDate; // Format as String
	private String adminNotes;
	private Integer resultingCouponId;
	private String couponName;
	private String couponCode;
}
