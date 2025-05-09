package ourpkg.campaign.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ourpkg.coupon.entity.Coupon;

@Entity
@Table(name = "CampaignCoupon", 
       uniqueConstraints = @UniqueConstraint(
           name = "UQ_Campaign_Coupon", 
           columnNames = {"campaign_id", "coupon_id"}
       )
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CampaignCoupon {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "campaign_coupon_id")
    private Integer campaignCouponId;
    
    @Column(name = "campaign_id", nullable = false)
    private Integer campaignId;
    
    @Column(name = "coupon_id", nullable = false)
    private Integer couponId;
    
    @Column(name = "total_quantity", nullable = false)
    private Integer totalQuantity;
    
    @Column(name = "remaining_quantity", nullable = false)
    private Integer remainingQuantity;
    
    @Column(name = "created_at", columnDefinition = "DATETIME DEFAULT GETDATE()")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", columnDefinition = "DATETIME DEFAULT GETDATE()")
    private LocalDateTime updatedAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", insertable = false, updatable = false)
    private MarketingCampaign campaign;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id", insertable = false, updatable = false)
    private Coupon coupon;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}