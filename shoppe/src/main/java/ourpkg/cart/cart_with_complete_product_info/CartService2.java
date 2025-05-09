package ourpkg.cart.cart_with_complete_product_info;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ourpkg.cart.Cart;
import ourpkg.cart.CartRepository;
import ourpkg.product.Product;
import ourpkg.product.ProductImage;
import ourpkg.product.ProductImageRepository;
import ourpkg.sku.Sku;
import ourpkg.sku.SkuRepository;
import ourpkg.user_role_permission.user.User;
import ourpkg.user_role_permission.user.UserRepository;

@Service
public class CartService2 {

	@Autowired
	private CartRepository cartRepository;

	@Autowired
	private SkuRepository skuRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ProductImageRepository productImageRepository; // 新增注入商品圖片倉庫

	@Transactional
	public CartItemDTO2 addToCart(int userId, int skuId, int quantity) {
		System.out.println("✅ 進入 addToCart 方法");

		Sku sku = skuRepository.findById(skuId).orElseThrow(() -> new RuntimeException("❌ 商品不存在 skuId=" + skuId));

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

		// 返回增強的 CartItemDTO2
		return createEnhancedCartItemDTO2(saved);
	}

	public CartItemDTO2 updateCartItem(int cartId, int quantity) {
		if (quantity <= 0) {
			throw new RuntimeException("購物車數量必須大於 0");
		}

		Cart cartItem = cartRepository.findById(cartId).orElseThrow(() -> new RuntimeException("購物車內無此商品"));

		Sku sku = skuRepository.findById(cartItem.getSku().getSkuId()).orElseThrow(() -> new RuntimeException("商品已下架"));

		if (sku.getStock() < quantity) {
			throw new RuntimeException("庫存不足，僅剩 " + sku.getStock() + " 件");
		}

		cartItem.setQuantity(quantity);
		cartRepository.save(cartItem);

		// 返回增強的 CartItemDTO2
		return createEnhancedCartItemDTO2(cartItem);
	}

	public void removeCartItem(int userId, int skuId) {
		Cart cartItem = cartRepository.findByUser_UserIdAndSku_skuId(userId, skuId)
				.orElseThrow(() -> new RuntimeException("購物車內無此商品"));

		cartRepository.delete(cartItem);
		cartRepository.flush(); // 確保變更即時提交
	}

	public List<CartItemDTO2> getCartByUserId(int userId) {
		List<Cart> cartItems = cartRepository.findByUser_UserId(userId);

		if (cartItems.isEmpty()) {
			throw new RuntimeException("購物車內沒有商品");
		}

		// 使用新的方法創建增強的 CartItemDTO2
		return cartItems.stream().map(this::createEnhancedCartItemDTO2).collect(Collectors.toList());
	}

	/**
	 * 創建增強版的 CartItemDTO2，包含商品圖片和規格信息
	 */
	private CartItemDTO2 createEnhancedCartItemDTO2(Cart cart) {
    Sku sku = cart.getSku();
    Product product = sku.getProduct();
    
    // 獲取商品名稱
    String productName = (product != null) ? product.getProductName() : "未知商品";
    
    // 獲取商品主圖 - 使用您現有的方法
//    String imageUrl = "/img/default-product.jpg"; // 默認圖片
//    if (product != null) {
//        // 使用您的 findFirstImageByProduct_ProductId 方法，這是最優的選擇
//        Optional<String> firstImage = productImageRepository.findFirstImageByProduct_ProductId(product.getProductId());
//        if (firstImage.isPresent()) {
//            imageUrl = firstImage.get();
//        }
//    }
 // 獲取商品主圖
    String imageUrl = "/img/default-product.jpg"; // 默認圖片
    if (product != null) {
        Optional<String> firstImage = productImageRepository.findFirstImageByProduct_ProductId(product.getProductId());
        if (firstImage.isPresent()) {
            String imagePath = firstImage.get();
            // 確保路徑格式正確 - 前綴必須是 /uploads/
            if (imagePath != null && !imagePath.isEmpty()) {
                // 如果路徑不是以 / 開頭，加上 /
                if (!imagePath.startsWith("/")) {
                    imagePath = "/" + imagePath;
                }
                imageUrl = imagePath;
                
            }
         // 在 CartService2.java 的 createEnhancedCartItemDTO2 方法中添加
            if (firstImage.isPresent()) {
                imageUrl = firstImage.get();
                System.out.println("產品ID: " + product.getProductId() + " 圖片路徑: " + imageUrl);
            }
        }
    }
    
    // 獲取規格信息
    Map<String, String> specInfo = new HashMap<>();
    if (sku != null && sku.getSpecPairs() != null && !sku.getSpecPairs().isEmpty()) {
        specInfo = sku.getSpecPairsAsMap();
    }
    
    // 獲取店鋪信息
    String shopName = "未知店鋪";
    Integer shopId = null;
    if (product != null && product.getShop() != null) {
        shopName = product.getShop().getShopName();
        shopId = product.getShop().getShopId();
    }
    
    // 獲取商品ID
    Integer productId = (product != null) ? product.getProductId() : null;
    
    // 獲取庫存
    Integer stock = (sku != null) ? sku.getStock() : 0;
    
    // 創建並返回增強的 CartItemDTO2
    return new CartItemDTO2(
        cart.getCartId(),
        sku.getSkuId(),
        productName,
        cart.getQuantity(),
        sku.getPrice(),
        imageUrl,
        specInfo,
        productId,
        shopName,
        shopId,
        stock
    );
}
	
}
