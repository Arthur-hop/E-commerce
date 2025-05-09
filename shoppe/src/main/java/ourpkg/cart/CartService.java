package ourpkg.cart;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ourpkg.sku.Sku;
import ourpkg.sku.SkuRepository;
import ourpkg.user_role_permission.user.User;
import ourpkg.user_role_permission.user.UserRepository;

@Service
public class CartService {
    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private SkuRepository skuRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public CartItemDto addToCart(int userId, int skuId, int quantity) {
        System.out.println("✅ 進入 addToCart 方法");

        Sku sku = skuRepository.findById(skuId)
                .orElseThrow(() -> new RuntimeException("❌ 商品不存在 skuId=" + skuId));

        if (sku.getStock() < quantity) {
            throw new RuntimeException("❌ 庫存不足，剩下：" + sku.getStock());
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("❌ 使用者不存在 userId=" + userId));

        Optional<Cart> existingCartItem = cartRepository.findByUser_UserIdAndSku_skuId(userId, skuId);
        Cart cartItem = existingCartItem.orElseGet(() -> {
            System.out.println("🆕 購物車內沒有這個商品，新增一筆 cart");
            Cart newCart = new Cart();
            newCart.setUser(user);
            newCart.setSku(sku);
            newCart.setQuantity(0);
            return newCart;
        });

        cartItem.setQuantity(cartItem.getQuantity() + quantity);
        Cart saved = cartRepository.save(cartItem);
        cartRepository.flush(); // 強制觸發 insert/update SQL

        System.out.println("💾 已儲存到資料庫，cartId=" + saved.getCartId());

        return new CartItemDto(
                saved.getCartId(),
                saved.getSku().getSkuId(),
                saved.getSku().getProduct() != null ? saved.getSku().getProduct().getProductName() : "未知商品",
                saved.getQuantity(),
                saved.getSku().getPrice()
        );
    }



    public CartItemDto updateCartItem(int cartId, int quantity) {
        if (quantity <= 0) {
            throw new RuntimeException("購物車數量必須大於 0");
        }

        Cart cartItem = cartRepository.findById(cartId)
                .orElseThrow(() -> new RuntimeException("購物車內無此商品"));

        Sku sku = skuRepository.findById(cartItem.getSku().getSkuId())
                .orElseThrow(() -> new RuntimeException("商品已下架"));

        if (sku.getStock() < quantity) {
            throw new RuntimeException("庫存不足，僅剩 " + sku.getStock() + " 件");
        }

        cartItem.setQuantity(quantity);
        cartRepository.save(cartItem);

        return new CartItemDto(
                cartItem.getCartId(),
                cartItem.getSku().getSkuId(),
                cartItem.getSku().getProduct() != null ? cartItem.getSku().getProduct().getProductName() : "未知商品",
                cartItem.getQuantity(),
                cartItem.getSku().getPrice()
        );
    }

    public void removeCartItem(int userId, int skuId) {
        Cart cartItem = cartRepository.findByUser_UserIdAndSku_skuId(userId, skuId)
                .orElseThrow(() -> new RuntimeException("購物車內無此商品"));

        cartRepository.delete(cartItem);
        cartRepository.flush(); // 確保變更即時提交
    }

    public List<CartItemDto> getCartByUserId(int userId) {
        List<Cart> cartItems = cartRepository.findByUser_UserId(userId);

        if (cartItems.isEmpty()) {
            throw new RuntimeException("購物車內沒有商品");
        }

        return cartItems.stream().map(cart -> new CartItemDto(
                cart.getCartId(),
                cart.getSku().getSkuId(),
                cart.getSku().getProduct() != null ? cart.getSku().getProduct().getProductName() : "未知商品",
                cart.getQuantity(),
                cart.getSku().getPrice()
        )).collect(Collectors.toList());
    }
}
