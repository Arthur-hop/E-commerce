package ourpkg.cart;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CartRequest {
    private int userId;
    private int skuId;
    private int quantity;
}
