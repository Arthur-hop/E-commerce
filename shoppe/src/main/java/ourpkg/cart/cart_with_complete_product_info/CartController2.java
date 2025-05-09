package ourpkg.cart.cart_with_complete_product_info;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.PostConstruct;
import ourpkg.cart.CartRequest;

@RestController
@RequestMapping("/api/cart")
@CrossOrigin(origins = "http://localhost:5173") // 確保與前端相符
public class CartController2 {
	@Autowired
	private CartService2 cartService2;

	// ✅ 啟動時檢查
	@PostConstruct
	public void init() {
		System.out.println("✅ CartController 已被 Spring 掃描");
	}

	// ✅ 測試 Controller 是否運行
	@GetMapping("/test")
	public String testController() {
		return "✅ CartController 正常運行";
	}

	// ✅ 加入購物車
	@PreAuthorize("hasAnyRole('USER', 'SELLER')")
	@PostMapping("/add")
	public ResponseEntity<?> addToCart(@RequestBody CartRequest request) {
		try {
			CartItemDTO2 cartItem = cartService2.addToCart(request.getUserId(), request.getSkuId(),
					request.getQuantity());
			return ResponseEntity.ok(cartItem);
		} catch (RuntimeException e) {
			return ResponseEntity.badRequest().body(e.getMessage());
		}
	}

	// ✅ 更新購物車商品數量
	@PutMapping("/{cartId}")
	public ResponseEntity<?> updateCart(@PathVariable int cartId, @RequestBody Map<String, Integer> body) {
		try {
			if (!body.containsKey("quantity")) {
				return ResponseEntity.badRequest().body("缺少 quantity 參數");
			}
			int quantity = body.get("quantity");
			CartItemDTO2 cartItem = cartService2.updateCartItem(cartId, quantity);
			return ResponseEntity.ok(cartItem);
		} catch (RuntimeException e) {
			return ResponseEntity.badRequest().body(e.getMessage());
		}
	}

	// ✅ 刪除購物車內的商品
	@DeleteMapping("/remove")
	public ResponseEntity<?> removeCart(@RequestParam int userId, @RequestParam int skuId) {
		try {
			cartService2.removeCartItem(userId, skuId);
			return ResponseEntity.ok("購物車商品已刪除");
		} catch (RuntimeException e) {
			return ResponseEntity.badRequest().body(e.getMessage());
		}
	}

	// ✅ 取得使用者的購物車
	@GetMapping("/{userId}")
	public ResponseEntity<?> getCartByUserId(@PathVariable int userId) {
		try {
			List<CartItemDTO2> cartItems = cartService2.getCartByUserId(userId);
			return ResponseEntity.ok(cartItems);
		} catch (RuntimeException e) {
			return ResponseEntity.badRequest().body(e.getMessage());
		}
	}
}
