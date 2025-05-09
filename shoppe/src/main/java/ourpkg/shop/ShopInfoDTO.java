package ourpkg.shop;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShopInfoDTO {
	private Integer shopId;
    private String shopName;
    private Integer userId;   // 賣家 User ID
    private String userName; // 賣家 User Name
    private String shopCategory; // *** 新增商店分類欄位 ***
}
