package ourpkg.customerService.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;

import com.fasterxml.jackson.annotation.JsonBackReference;
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
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ourpkg.shop.Shop;
import ourpkg.user_role_permission.user.User;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ChatRoom")
public class ChatRoomEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "chat_room_id")
	private Integer chatRoomId;

	// 一個聊天室對應一個買家（ManyToOne）
	@JsonBackReference("buyerReference")
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "buyer_id", nullable = false)
	private User buyer;

	// 一個聊天室對應一個賣家（ManyToOne）
	@JsonBackReference("sellerReference")
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "seller_id", nullable = false)
	private User seller;

	// 一個聊天室有多條消息（OneToMany）
	@OneToMany(mappedBy = "chatRoomEntity", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JsonManagedReference(value = "chatRoom-messages") // 確保唯一名稱
	private List<ChatMessageEntity> messages = new ArrayList<>();

	@CreationTimestamp
	@Column(name = "created_at")
	private LocalDateTime createdAt;

	@Column(name = "last_active_at")
	private LocalDateTime lastActiveAt;

	// 添加更新方法
	public void updateLastActive() {
	    this.lastActiveAt = LocalDateTime.now();
	}
}
