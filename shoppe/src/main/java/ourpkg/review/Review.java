package ourpkg.review;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.format.annotation.DateTimeFormat;

import com.fasterxml.jackson.annotation.JsonIgnore;

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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ourpkg.order.OrderItem;
import ourpkg.product.Product;
import ourpkg.shop.Shop;
import ourpkg.user_role_permission.user.User;

@Entity
@Table(name = "Review")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Review {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "review_id")
	private Integer reviewId;

	@ManyToOne
	@JoinColumn(name = "user_id", nullable = false)
	@JsonIgnore
	private User user;

	@ManyToOne
	@JoinColumn(name = "shop_id", nullable = false)
	@JsonIgnore
	private Shop shop;

	@ManyToOne
	@JoinColumn(name = "product_id", nullable = false)
	@JsonIgnore
	private Product product;

	@ManyToOne
	@JoinColumn(name = "item_id", referencedColumnName = "item_id")
	@JsonIgnore
	private OrderItem orderItem;

	@Column(name = "product_name", nullable = false)
	private String productName;

	@Column(name = "review_content", columnDefinition = "TEXT", nullable = false)
	private String reviewContent;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", length = 20, nullable = false)
	private Status status = Status.APPROVED;

	@Column(name = "rating", precision = 2, scale = 1, nullable = false)
	private BigDecimal rating;

	@Column(name = "created_at", updatable = false)
	@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime createdAt;

	@Column(name = "updated_at")
	@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime updatedAt;

	@Column(name = "order_item_id", nullable = false)
	private Integer orderItemId;

	@PrePersist
	protected void onCreate() {
		this.createdAt = LocalDateTime.now();
		this.updatedAt = LocalDateTime.now();
	}

	@PreUpdate
	protected void onUpdate() {
		this.updatedAt = LocalDateTime.now();
	}
    
    @Column(name = "reply_content", columnDefinition = "TEXT")
    private String replyContent;

    @Column(name = "reply_time")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime replyTime;

	public enum Status {
		APPROVED,
		BLOCKED
	}
}