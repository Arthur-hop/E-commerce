package ourpkg.coupon.entity;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ourpkg.shop.Shop;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "Coupon")
public class Coupon {
	

	

	// 判斷是否已被領取
	public boolean isRedeemed() {
		return redeemed;
	}

	// 設定已領取
	public void setRedeemed(boolean redeemed) {
		this.redeemed = redeemed;
	}

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "coupon_id")
	private Integer couponId;

	@ManyToOne
	@JsonBackReference("shop-coupon")
	@JoinColumn(name = "coupon_shop_id", nullable = false)
	private Shop shop;
	
	@Column(name = "coupon_code", nullable = false)
	private String couponCode;

	@Column(name = "coupon_name", nullable = false)
	private String couponName;

	@Column(name = "discount_type", nullable = false)
	private String discountType;

	@Column(name = "discount_value", nullable = false)
	private BigDecimal discountValue;

	@Column(name = "start_date", nullable = false)
	private java.util.Date startDate;

	@Column(name = "end_date", nullable = false)
	private java.util.Date endDate;

	@Column(name = "usage_limit")
	private Integer usageLimit;

	@Column(name = "usage_per_user")
	private Integer usagePerUser;

	@Column(name = "created_at")
	private java.util.Date createdAt;

	@Column(name = "updated_at")
	private java.util.Date updatedAt;

	@Column(name = "description")
	private String description;

	@Column(name = "redeemed", nullable = false)
	private boolean redeemed = false;

}
