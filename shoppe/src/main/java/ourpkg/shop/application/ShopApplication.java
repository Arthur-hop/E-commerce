package ourpkg.shop.application;

import java.time.LocalDateTime;
import java.util.Date;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ourpkg.shop.application.ShopApplication.ApplicationStatus;
import ourpkg.user_role_permission.user.User;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "Shop_Application")
public class ShopApplication {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="application_id")
    private Integer applicationId;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name="shop_category")
    private String shopCategory;

    @Column(name="return_city")
    private String returnCity;

    @Column(name="return_district")
    private String returnDistrict;
    
    @Column(name="return_zip_code")
    private String returnZipCode;
    
    @Column(name="return_street_etc")
    private String returnStreetEtc;
    
    @Column(name="return_recipient_name")
    private String returnRecipientName;
    
    @Column(name="return_recipient_phone")
    private String returnRecipientPhone;
    
    @Column(name="shop_name")
    private String shopName;
    
    @Column(name="description")
    private String description;

    @Enumerated(EnumType.STRING)
    private ApplicationStatus status = ApplicationStatus.PENDING;

    @Column(name="admin_comment")
    private String adminComment;
    
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") // MVC
	@Temporal(TemporalType.TIMESTAMP)
    @Column(name = "reviewed_at")
    private Date reviewedAt;
    
    @ManyToOne
    @JoinColumn(name = "reviewed_by")
    private User reviewer; // 審核人
    
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") // MVC
	@Column(name = "[created_at]", updatable = false) // 只在新增時設定，之後不允許更新
	private Date createdAt;
    
    @PrePersist  // 在保存之前執行
    protected void onCreate() {
        createdAt = new Date(); // 設定為現在時間
    }
    
    public enum ApplicationStatus {
        PENDING, APPROVED, REJECTED
    }
    

}
