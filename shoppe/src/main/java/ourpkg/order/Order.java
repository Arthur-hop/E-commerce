package ourpkg.order;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
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
import ourpkg.address.OrderAddress;
import ourpkg.payment.Payment;
import ourpkg.shipment.Shipment;
import ourpkg.user_role_permission.user.User;

@Entity
@Table(name = "[Order]")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "[order_id]")
    private Integer orderId;
    
    @ManyToOne
    @JoinColumn(name = "[user_id]", nullable = false)
    @JsonIgnore
    @JsonManagedReference("user-order")
    private User user;
    
    @ManyToOne
    @JoinColumn(name = "[order_address_id_billing]", nullable = false)
    private OrderAddress orderAddressBilling;
    
    @ManyToOne
    @JoinColumn(name = "[order_address_id_shipping]", nullable = false)
    private OrderAddress orderAddressShipping;
    
    @ManyToOne
    @JoinColumn(name = "[order_status_id]", nullable = false)
    private OrderStatusCorrespond orderStatusCorrespond;
    
    @Column(name = "[total_price]")
    private BigDecimal totalPrice;
    
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<OrderItem> orderItem;
    
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Payment> payment;
    
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Shipment> shipment;
    
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OrderStatusHistory> orderStatusHistory;
    
    // @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "UTC+8") // 前後端分離
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
    
    // 取得訂單的帳單地址
    public OrderAddress getBillingAddress() {
        return this.orderAddressBilling;
    }
    
    // 取得訂單的收件地址
    public OrderAddress getShippingAddress() {
        return this.orderAddressShipping;
    }
    
    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    
//    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
//    private List<OrderItem> orderItems;

//    public List<OrderItem> getOrderItems() {
//        return orderItems;
//    }
    public String getDescription() {
        // 返回訂單描述，可以根據需求自定義
        return "訂單 #" + orderId + " - " + (orderItem != null && !orderItem.isEmpty() ? orderItem.get(0).getProductName() : "無商品");
    }

}