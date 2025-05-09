package ourpkg.campaign.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UserCouponDTO {
	private Integer userCouponId;
	private String couponName;
	private String discountType;
	private BigDecimal  discountValue;
	private LocalDate startDate;
	private LocalDate endDate;

	public UserCouponDTO(Integer userCouponId, String couponName, String discountType, BigDecimal discountValue,
			LocalDate startDate, LocalDate endDate) {
		this.userCouponId = userCouponId;
		this.couponName = couponName;
		this.discountType = discountType;
		this.discountValue = discountValue;
		this.startDate = startDate;
		this.endDate = endDate;
	}
}
