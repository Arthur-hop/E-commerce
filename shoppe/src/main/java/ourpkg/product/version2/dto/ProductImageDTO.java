package ourpkg.product.version2.dto;

import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ourpkg.product.version2.dto.ProductResDTO.PriceRangeDTO;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductImageDTO {

private Integer imageId;
    
    private Integer productId;
    
    private String imagePath;
    
    private Boolean isPrimary;
    
    private Integer displayOrder;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "UTC+8")
    private Date createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "UTC+8")
    private Date updatedAt;
}
