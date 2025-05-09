//package ourpkg.product.oldEntityAndRepo;
//
//import java.util.Date;
//import java.util.List;
//
//import org.springframework.format.annotation.DateTimeFormat;
//
//import com.fasterxml.jackson.annotation.JsonFormat;
//
//import jakarta.persistence.CascadeType;
//import jakarta.persistence.Column;
//import jakarta.persistence.Entity;
//import jakarta.persistence.FetchType;
//import jakarta.persistence.GeneratedValue;
//import jakarta.persistence.GenerationType;
//import jakarta.persistence.Id;
//import jakarta.persistence.JoinColumn;
//import jakarta.persistence.JoinTable;
//import jakarta.persistence.ManyToMany;
//import jakarta.persistence.ManyToOne;
//import jakarta.persistence.OneToMany;
//import jakarta.persistence.PrePersist;
//import jakarta.persistence.PreUpdate;
//import jakarta.persistence.Table;
//import jakarta.persistence.Temporal;
//import jakarta.persistence.TemporalType;
//import lombok.AllArgsConstructor;
//import lombok.Builder;
//import lombok.Getter;
//import lombok.NoArgsConstructor;
//import lombok.Setter;
//import ourpkg.attribute.AttributeValue;
//import ourpkg.category.Category1;
//import ourpkg.category.Category2;
//import ourpkg.shop.Shop;
//import ourpkg.sku.oldEntityAndRepo.Sku;
//
//@Entity
//@Table(name = "[Product]")
//@Getter
//@Setter
//@NoArgsConstructor
//@AllArgsConstructor
//public class Product {
//
//	@Id
//	@GeneratedValue(strategy = GenerationType.IDENTITY)
//	@Column(name = "[product_id]")
//	private Integer productId;
//
//	@ManyToOne
//	@JoinColumn(name = "[shop_id]", nullable = false)
//	private Shop shop;
//
//	@ManyToOne
//	@JoinColumn(name = "[c1_id]", nullable = false)
//	private Category1 category1;
//
//	@ManyToOne
//	@JoinColumn(name = "[c2_id]", nullable = false)
//	private Category2 category2;
//
//	@OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
//	private List<Sku> skuList;
//
//	@ManyToMany
//	@JoinTable(name = "[ProductAttributeValue]", joinColumns = @JoinColumn(name = "[product_id]"), inverseJoinColumns = @JoinColumn(name = "[attribute_value_id]"))
//	private List<AttributeValue> attributeValueList;
//
//	@Column(name = "[product_name]")
//	private String productName;
//
//	@Column(name = "[description]")
//	private String description;
//
//	@Column(name = "[image]")
//	private String image;
//
//	@Column(name = "[active]")
//	private Boolean active = false; // 賣家控制上下架(預設儲存不上架0)
//	
//	@Column(name = "[review_status]")
//	private Boolean reviewStatus = true; // 管理者控制上下架(預設審核通過1)(如果審核不通過，值0，並把active設為0)
//	
//	@Column(name = "[review_comment]")
//	private String reviewComment; // 審核意見
//	
//	public String getName() {
//		return this.productName; // 返回productName屬性
//	}
//
////	@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") // MVC
//	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "UTC+8") // 前後端分離
//	@Temporal(TemporalType.TIMESTAMP)
//	@Column(name = "[created_at]", updatable = false) // 只在新增時設定，之後不允許更新
//	private Date createdAt;
//
//	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "UTC+8") // 前後端分離
//	@Temporal(TemporalType.TIMESTAMP)
//	@Column(name = "[updated_at]")
//	private Date updatedAt;
//
//	@PrePersist // 新增一筆 Entity 到資料庫 之前調用
//	protected void onCreate() {
//		Date now = new Date();
//		this.createdAt = now;
//		this.updatedAt = now;
//	}
//
//	@PreUpdate // 更新一筆 Entity 到資料庫 之前調用
//	protected void onUpdate() {
//		this.updatedAt = new Date();
//	}
//}
