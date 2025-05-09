package ourpkg.shop;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ShopDTO {
    private Integer shopId;
    private String shopName;
    private Integer userId; // 賣家 ID
    private String userName; // 賣家 User Name
    private String description;
    private Integer countProducts;
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Asia/Taipei")
    private Date createdAt;
}
