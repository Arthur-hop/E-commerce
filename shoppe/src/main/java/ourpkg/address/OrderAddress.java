package ourpkg.address;

import java.util.Date;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ourpkg.order.Order;

@Entity
@Table(name = "OrderAddress")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderAddress {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "[order_address_id]")
	private Integer orderAddressId;

	@ManyToOne
	@JoinColumn(name = "[address_type_id]", nullable = false)
	@JsonBackReference(value = "address-type-order")
	private AddressTypeCorrespond addressTypeCorrespond;

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

	@OneToMany(mappedBy = "orderAddressBilling", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	private List<Order> orderBilling;
	
	@OneToMany(mappedBy = "orderAddressShipping", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	private List<Order> orderShipping;

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

	public void setReceiverAddress(String receiverAddress) {
	    this.streetEtc = receiverAddress;
	}

}
