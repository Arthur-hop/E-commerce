package ourpkg.campaign.entity;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ourpkg.coupon.entity.Coupon;
import ourpkg.order.Order;
import ourpkg.user_role_permission.user.User;

@Entity
@Table(name = "UserCoupon", 
       uniqueConstraints = @UniqueConstraint(
           name = "UQ_User_Coupon", 
           columnNames = {"user_id", "coupon_id"}
       )
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserCoupon {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_coupon_id")
    private Integer userCouponId;
    
    @Column(name = "user_id", nullable = false)
    private Integer userId;
    
    @Column(name = "coupon_id", nullable = false)
    private Integer couponId;
    
    @Column(name = "campaign_id")
    private Integer campaignId;
    
    @Column(name = "acquired_date", columnDefinition = "DATETIME DEFAULT GETDATE()")
    private LocalDateTime acquiredDate;
    
    @Column(name = "used_date")
    private LocalDateTime usedDate;
    
    @Column(name = "status", length = 20, columnDefinition = "VARCHAR(20) DEFAULT 'ACTIVE'")
    private String status = "ACTIVE"; // ACTIVE, USED, EXPIRED
    
    @Column(name = "order_id")
    private Integer orderId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    @JsonIgnoreProperties({"cart", "shop", "password"})
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id", insertable = false, updatable = false)
    @JsonIgnoreProperties({"shop"})
    private Coupon coupon;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", insertable = false, updatable = false)
    @JsonIgnoreProperties({"someFields"})
    private MarketingCampaign campaign;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", insertable = false, updatable = false)
    @JsonIgnoreProperties({"user", "orderItems"})
    private Order order;
    
    @PrePersist
    protected void onCreate() {
        acquiredDate = LocalDateTime.now();
    }
}