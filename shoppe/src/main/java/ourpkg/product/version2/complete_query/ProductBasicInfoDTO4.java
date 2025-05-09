package ourpkg.product.version2.complete_query;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductBasicInfoDTO4 {
	private Integer productId;
    private String productName;
    private String description;
    private Boolean active;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updatedAt;
    
    // 分类信息
    private Integer category1Id;
    private String category1Name;
    private Integer category2Id;
    private String category2Name;
    
    // 价格范围
    private PriceRangeDTO4 priceRange;
}
