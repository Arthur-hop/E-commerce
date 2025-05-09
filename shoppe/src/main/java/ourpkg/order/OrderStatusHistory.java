package ourpkg.order;

import java.util.Date;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

@Entity
@Table(name = "[OrderStatusHistory]")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusHistory {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "[history_id]")
	private Integer historyId;
	
	@ManyToOne
	@JoinColumn(name = "[order_id]", nullable = false)
	private Order order;
	
	@ManyToOne
	@JoinColumn(name = "[order_status_id]", nullable = false)
	private OrderStatusCorrespond orderStatusCorrespond;
	

//	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "UTC+8") // 前後端分離
	@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") // MVC
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "[created_at]", updatable = false) // 只在新增時設定，之後不允許更新
	private Date createdAt;
	

	@PrePersist // 新增一筆 Entity 到資料庫 之前調用
	protected void onCreate() {
		Date now = new Date();
		this.createdAt = now;
	}


	
}
