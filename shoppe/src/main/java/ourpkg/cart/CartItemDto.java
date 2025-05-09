package ourpkg.cart;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CartItemDto {
    private int cartId;
    private int skuId;
    private String name;
    private int quantity;
    private BigDecimal price;  // 修正為 BigDecimal


    



    public int getSkuId() { return skuId; }
    public void setSkuId(int skuId) { this.skuId = skuId; }
}
