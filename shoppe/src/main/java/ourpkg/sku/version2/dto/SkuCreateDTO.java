package ourpkg.sku.version2.dto;

import java.math.BigDecimal;
import java.util.Map;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SkuCreateDTO {
    
    @NotNull(message = "庫存不能為空")
    @Min(value = 0, message = "庫存不能小於0")
    private Integer stock;
    
    @NotNull(message = "價格不能為空")
    @Min(value = 0, message = "價格不能小於0")
    private BigDecimal price;
    
    /**
     * 規格鍵值對，如 {"顏色": "紅色", "尺寸": "M"}
     */
    private Map<String, String> specPairs;
}
