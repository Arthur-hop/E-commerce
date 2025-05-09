package ourpkg.order;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ourpkg.product.Product;
import ourpkg.shop.Shop;
import ourpkg.sku.Sku;


@Entity
@Table(name = "OrderItem")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "item_id")
	private Integer itemId;

	@ManyToOne
	@JoinColumn(name = "order_id", nullable = false)
	@JsonBackReference
	@JsonIgnore
	private Order order;

	@ManyToOne
	@JoinColumn(name = "sku_id", nullable = false)
	@JsonIgnore
	private Sku sku;

	@ManyToOne
	@JoinColumn(name = "shop_id", nullable = false)
	@JsonIgnore
	private Shop shop;

	@Column(name = "unit_price")
	private BigDecimal unitPrice;

	@Column(name = "quantity")
	private Integer quantity;
	
//	 @Id
//	    @GeneratedValue(strategy = GenerationType.IDENTITY)
//	    @Column(name = "order_item_id")
//	    private Integer orderItemId;

//	    @ManyToOne
//	    @JoinColumn(name = "product_id")
//	    private Product product;

	 public String getProductName() {
	        return sku != null && sku.getProduct() != null ? sku.getProduct().getProductName() : "未知商品";
	    }

	

}
