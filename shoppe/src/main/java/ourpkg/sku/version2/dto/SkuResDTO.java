package ourpkg.sku.version2.dto;

import java.math.BigDecimal;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SkuResDTO {
    
    private Integer skuId;
    
    private Integer productId;
    
    private String productName;
    
    private Integer stock;
    
    private BigDecimal price;
    
    /**
     * 規格鍵值對，如 {"顏色": "紅色", "尺寸": "M"}
     */
    private Map<String, String> specPairs;
    
    /**
     * 規格描述，用於顯示，如 "紅色/M"
     */
    private String specDescription;
    
    private Boolean isDeleted;
}
