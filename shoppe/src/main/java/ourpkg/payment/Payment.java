package ourpkg.payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;

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
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ourpkg.order.Order;

@Entity
@Table(name = "[Payment]")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "[payment_id]")
    private Integer paymentId;

    @ManyToOne
    @JoinColumn(name = "[order_id]", nullable = false)
    private Order order;

    @ManyToOne
    @JoinColumn(name = "[payment_method_id]", nullable = false)
    private PaymentMethod paymentMethod;

    @ManyToOne
    @JoinColumn(name = "[payment_status_id]", nullable = false)
    private PaymentStatus paymentStatus;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ✅ 設定付款狀態 (透過 Repository 取得)
    public void setPaymentStatusById(int paymentStatusId, PaymentStatusRepository paymentStatusRepository) {
        this.paymentStatus = paymentStatusRepository.findById(paymentStatusId)
                .orElseThrow(() -> new RuntimeException("找不到 PaymentStatus ID: " + paymentStatusId));
    }

    // ✅ 設定付款方式 (透過 Repository 取得)
    public void setPaymentMethodById(int paymentMethodId, PaymentMethodRepository paymentMethodRepository) {
        this.paymentMethod = paymentMethodRepository.findById(paymentMethodId)
                .orElseThrow(() -> new RuntimeException("找不到 PaymentMethod ID: " + paymentMethodId));
    }



	
}
