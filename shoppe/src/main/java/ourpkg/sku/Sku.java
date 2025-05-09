package ourpkg.sku;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;

import org.springframework.data.annotation.Transient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ourpkg.product.Product;

@Entity
@Table(name = "[SKU]")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Sku {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "[sku_id]")
	private Integer skuId;

	@ManyToOne
	@JoinColumn(name = "[product_id]", nullable = false)
	private Product product;

	@Column(name = "[stock]")
	private Integer stock;

	@Column(name = "[price]")
	private BigDecimal price;

	@Column(name = "[spec_pairs]", columnDefinition = "NVARCHAR(255)")
	private String specPairs; // 存 JSON 字串

	@Column(name = "[is_deleted]")
	private Boolean isDeleted = false; //預設0

	@Transient // 這個欄位不會存入資料庫(讓JPA不持久化它，純粹用於JAVA層的操作)
	private static final ObjectMapper objectMapper = new ObjectMapper();

	// 解析 JSON 字串為 Map<String, String> 接收前端 後端處理
	public Map<String, String> getSpecPairsAsMap() {
		if (specPairs == null || specPairs.isEmpty()) {
			return Collections.emptyMap();
		}
		try {
			return objectMapper.readValue(specPairs, new TypeReference<Map<String, String>>() {
			});
		} catch (JsonProcessingException e) {
			System.out.println(specPairs);
			throw new RuntimeException("Failed to parse specPairs JSON: " + specPairs, e);
		}
	}

	// 將 Map<String, String> 轉為 JSON 字串(序列化) 存入資料庫
	public void setSpecPairsFromMap(Map<String, String> specPairsMap) {
		if (specPairsMap == null) {
			this.specPairs = null;
			return;
		}
		try {
			this.specPairs = objectMapper.writeValueAsString(specPairsMap);
		} catch (JsonProcessingException e) {
			throw new RuntimeException("Failed to serialize specPairs map: " + specPairsMap, e);
		}
	}

}
