package ourpkg.shop;

import java.util.Date;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ourpkg.coupon.entity.Coupon;
import ourpkg.customerService.entity.ChatRoomEntity;
import ourpkg.order.OrderItem;
import ourpkg.product.Product;
import ourpkg.user_role_permission.user.User;

@Entity
@Table(name = "[Shop]")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Shop {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "[shop_id]")
	private Integer shopId;

	@OneToOne
	@JoinColumn(name = "[user_id]", nullable = false, unique = true) // 確保一個使用者只能有一個店舖
//	@JsonIgnore
	@JsonBackReference("user-shop") // <== 加這個解決 JSON 遞迴 
//	@JsonManagedReference("user-shop")//這個不知道要不要拿掉 先留者~
	private User user;

	@Column(name = "[return_city]")
	private String returnCity;

	@Column(name = "shop_category")
	private String shopCategory;

	@Column(name = "[return_district]")
	private String returnDistrict;

	@Column(name = "[return_zip_code]")
	private String returnZipCode;

	@Column(name = "[return_street_etc]")
	private String returnStreetEtc;

	@Column(name = "[return_recipient_name]")
	private String returnRecipientName;

	@Column(name = "[return_recipient_phone]")
	private String returnRecipientPhone;

	@Column(name = "[shop_name]", nullable = false)
	private String shopName;

	@Column(name = "[description]")
	private String description;
	
//  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "UTC+8") // 前後端分離
	@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") // MVC
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "[created_at]", updatable = false) // 只在新增實設定 之後不允許更新
	private Date createdAt;

	@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "[updated_at]")
	private Date updatedAt;

	@OneToMany(mappedBy = "shop", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JsonIgnore
	private List<Product> products;

	@OneToMany(mappedBy = "shop", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JsonIgnore
	private List<OrderItem> orderItems;

//	@OneToMany(mappedBy = "shop", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
//	private List<OrderItem> orderItem;

	@PrePersist // 新增一筆Entity 到資料庫 之前調用
	protected void onCreate() {
		Date now = new Date();
		this.createdAt = now;
		this.updatedAt = now;
	}

	@PreUpdate // 更新一筆Entity 到資料庫 之前調用
	protected void onUpdate() {
		this.updatedAt = new Date();
	}

	@Column(name = "is_active", nullable = false, columnDefinition = "BIT DEFAULT 1")
	private Boolean isActive = true;

	
	@OneToMany(mappedBy = "shop")
	@JsonManagedReference("shop-coupon")
	private List<Coupon> coupon;

	
	
}
