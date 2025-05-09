package ourpkg.customerService.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.fasterxml.jackson.annotation.JsonBackReference;
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
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ourpkg.user_role_permission.user.User;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ChatMessage")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"}) // 避免 JSON 解析錯誤
public class ChatMessageEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "chatMessage_id")
	private Integer messageId;

	// 一條消息屬於一個聊天室（ManyToOne）
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    @JsonBackReference(value = "chatRoom-messages") // 確保唯一名稱
    private ChatRoomEntity chatRoomEntity;

    // 一條消息由一個用戶發送（ManyToOne）
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    @JsonBackReference(value = "user-messages") // 確保唯一名稱
    private User sender;
    
   

    @Column(columnDefinition = "TEXT")
    private String content;

    @CreationTimestamp
    private LocalDateTime timestamp;
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    @Column(name = "sender_name")  // 新增 senderName 欄位
    private String senderName;
    

    // 确保在保存时自动填充 senderName
    @PrePersist
    public void prePersist() {
        if (sender != null) {
            this.senderName = sender.getUsername();
        }
    }
    
    @Column(name = "is_read")
    private Boolean isRead = false;
}
