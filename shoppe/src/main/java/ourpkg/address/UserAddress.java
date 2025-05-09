package ourpkg.address;

import java.util.Date;

import org.springframework.format.annotation.DateTimeFormat;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ourpkg.user_role_permission.user.User;

@Entity
@Table(name = "[UserAddress]", uniqueConstraints = @UniqueConstraint(columnNames = { "[user_id]", "[address_type_id]",
		"[is_default]" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserAddress {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "[user_address_id]")
	private Integer userAddressId;

	@ManyToOne
	@JoinColumn(name = "[user_id]", nullable = false)
	@JsonIgnore
	@JsonManagedReference("user-address")
	private User user;

	@ManyToOne
	@JoinColumn(name = "[address_type_id]",nullable = false)
	@JsonBackReference(value = "address-type-user")
	private AddressTypeCorrespond addressTypeCorrespond;

	@Column(name = "[is_default]")
	private Boolean isDefault;

	@Column(name = "[city]")
	private String city;

	@Column(name = "[district]")
	private String district;

	@Column(name = "[zip_code]")
	private String zipCode;

	@Column(name = "[street_etc]")
	private String streetEtc;

	@Column(name = "[recipient_name]")
	private String recipientName;

	@Column(name = "[recipient_phone]")
	private String recipientPhone;

//	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "UTC+8") // 前後端分離
	@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") // MVC
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "[created_at]", updatable = false) // 只在新增時設定，之後不允許更新
	private Date createdAt;

	@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "[updated_at]")
	private Date updatedAt;

	@PrePersist // 新增一筆 Entity 到資料庫 之前調用
	protected void onCreate() {
		Date now = new Date();
		this.createdAt = now;
		this.updatedAt = now;
	}

	@PreUpdate // 更新一筆 Entity 到資料庫 之前調用
	protected void onUpdate() {
		this.updatedAt = new Date();
	}

}
