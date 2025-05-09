package ourpkg.product;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;

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
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ProductImage")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductImage {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "[image_id]")
	private Integer imageId;

	@Column(name = "[image_path]", nullable = false, columnDefinition = "NVARCHAR(MAX)")
	private String imagePath;

	@Column(name = "[is_primary]", nullable = false)
	private Boolean isPrimary = false;

	@Column(name = "[display_order]", nullable = false)
	private Integer displayOrder = 0;

//	@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") // MVC
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "UTC+8") // 前後端分離
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "[created_at]", updatable = false) // 只在新增時設定，之後不允許更新
	private Date createdAt;

	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "UTC+8") // 前後端分離
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

	// 關聯關係
	@ManyToOne
	@JoinColumn(name = "[product_id]")
	private Product product;
}
