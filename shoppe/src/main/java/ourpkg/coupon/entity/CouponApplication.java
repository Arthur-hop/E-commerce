package ourpkg.coupon.entity;

import java.math.BigDecimal;
import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ourpkg.shop.Shop;
import ourpkg.user_role_permission.user.User;

@Entity
@Table(name = "CouponApplication")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CouponApplication {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "application_id")
	private Integer applicationId;

	@Column(name = "application_type", nullable = false, length = 20)
	private String applicationType; // 'CREATE', 'UPDATE', 'DELETE'

	@Column(name = "requested_by_seller_id", nullable = false)
	private Integer requestedBySellerId;

	@Column(name = "requested_shop_id", nullable = false)
	private Integer requestedShopId;

	@Column(name = "target_coupon_id")
	private Integer targetCouponId; // For UPDATE/DELETE

	// --- Proposed Values (Nullable) ---
	@Column(name = "coupon_name", length = 100)
	private String couponName;

	@Column(name = "coupon_code", length = 50)
	private String couponCode;

	@Column(name = "description", length = 255)
	private String description;

	@Column(name = "discount_type", length = 20)
	private String discountType;

	@Column(name = "discount_value", precision = 10, scale = 2)
	private BigDecimal discountValue;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "start_date")
	private Date startDate;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "end_date")
	private Date endDate;

	@Column(name = "usage_limit")
	private Integer usageLimit;

	@Column(name = "usage_per_user")
	private Integer usagePerUser;

	// --- Status and Tracking ---
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "application_date", nullable = false, insertable = false, updatable = false, columnDefinition = "DATETIME DEFAULT GETDATE()")
	private Date applicationDate;

	@Column(name = "status", nullable = false, length = 20, columnDefinition = "VARCHAR(20) DEFAULT 'PENDING'") // DB
																												// default,
																												// Java
																												// default
																												// below
	private String status = "PENDING"; // Default needs adjustment based on type in service layer

	@Column(name = "admin_notes", length = 500)
	private String adminNotes;

	@Column(name = "resulting_coupon_id")
	private Integer resultingCouponId; // For approved CREATE

	@ManyToOne(fetch = FetchType.LAZY)

	@JoinColumn(name = "requested_by_seller_id", insertable = false, updatable = false)
	private User requestedBySeller;

	@ManyToOne(fetch = FetchType.LAZY)

	@JoinColumn(name = "requested_shop_id", insertable = false, updatable = false)
	private Shop requestedShop;

	// Relation to the coupon being modified or deleted

	@ManyToOne(fetch = FetchType.LAZY)

	@JoinColumn(name = "target_coupon_id", insertable = false, updatable = false)
	private Coupon targetCoupon;

	// Relation to the coupon created after approval

	@OneToOne(fetch = FetchType.LAZY)

	@JoinColumn(name = "resulting_coupon_id", insertable = false, updatable = false)
	private Coupon resultingCoupon;

}
