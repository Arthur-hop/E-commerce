package ourpkg.product.version2.controller_service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.nimbusds.jwt.JWTClaimsSet;

import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import ourpkg.jwt.JsonWebTokenAuthentication;
import ourpkg.jwt.JsonWebTokenUtility;
import ourpkg.product.Product;
import ourpkg.product.ProductImage;
import ourpkg.product.ProductRepository;
import ourpkg.product.version2.dto.ProductDetailDTO;
import ourpkg.product.version2.dto.ProductPublicDTO;
import ourpkg.product.version2.dto.ProductResDTO;
import ourpkg.product.version2.dto.ProductUpdateDTO;
import ourpkg.user_role_permission.user.User;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

	private final ProductService productService;
	private final JsonWebTokenUtility jsonWebTokenUtility;
	private final ProductRepository productRepository;

	/**
	 * 獲取單一商品詳情
	 */
	@GetMapping("/{id}")
	public ResponseEntity<ProductResDTO> getProductById(@PathVariable Integer id) {
		return ResponseEntity.ok(productService.getById(id));
	}

	/**
	 * 獲取單一商品詳情：包含所有規格
	 */
	@GetMapping("/{id}/detail")
	public ResponseEntity<ProductDetailDTO> getProductDetail(@PathVariable Integer id) {
		return ResponseEntity.ok(productService.getProductDetail(id));
	}

	/**
	* 獲取單一商品詳情：僅包含活跃規格（不包括已软删除的SKU）
	*/
	@GetMapping("/{id}/active-detail")
	public ResponseEntity<ProductDetailDTO> getProductActiveDetail(@PathVariable Integer id) {
	    return ResponseEntity.ok(productService.getProductActiveDetail(id));
	}
	
	
	/**
	 * 商品查詢（支援多條件搜尋與分頁）
	 */
	@GetMapping
	public ResponseEntity<Page<ProductResDTO>> findProducts(@RequestParam(required = false) Integer shopId,
			@RequestParam(required = false) Integer category1Id, @RequestParam(required = false) Integer category2Id,
			@RequestParam(required = false) String nameKeyword, @RequestParam(required = false) String descKeyword,
			@RequestParam(required = false) BigDecimal minPrice, @RequestParam(required = false) BigDecimal maxPrice,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {

		Page<ProductResDTO> result = productService.findProducts(shopId, category1Id, category2Id, nameKeyword,
				descKeyword, minPrice, maxPrice, page, size);

		return ResponseEntity.ok(result);
	}

	/**
	 * 獲取公開商品並支持分頁、分類和關鍵字過濾
	 * 
	 * 此 API 端點用於商城前端展示商品，支持多種條件篩選和分頁功能。 商品必須是已激活且未刪除的。
	 * 
	 * @param category1Id 一級分類ID (可選)，若提供則只返回該分類下的商品
	 * @param category2Id 二級分類ID (可選)，若提供則只返回該分類下的商品
	 * @param nameKeyword 商品名稱關鍵字 (可選)，用於模糊搜尋商品名稱
	 * @param page        頁碼 (從0開始，默認為0)
	 * @param size        每頁商品數量 (默認為12)
	 * @return 分頁的商品 DTO 列表，包含分頁信息和商品基本信息
	 */
	@GetMapping("/public")
	public ResponseEntity<Page<ProductPublicDTO>> getPublicProducts(@RequestParam(required = false) Integer category1Id,
			@RequestParam(required = false) Integer category2Id, @RequestParam(required = false) String nameKeyword,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "12") int size) {

		Page<ProductPublicDTO> publicDtoPage = productService.findPublicProducts(null, category1Id, category2Id,
				nameKeyword, page, size);

		return ResponseEntity.ok(publicDtoPage);
	}

	/**
	 * 獲取特定店鋪的公開商品並支持分頁、分類和關鍵字過濾
	 * 
	 * 此 API 端點專門用於查詢特定賣家店鋪的商品，支持分類過濾和分頁功能。 商品必須是已激活且未刪除的。
	 * 
	 * @param shopId      店鋪ID (必需)，指定要查詢的店鋪
	 * @param category1Id 一級分類ID (可選)，若提供則只返回該分類下的商品
	 * @param category2Id 二級分類ID (可選)，若提供則只返回該分類下的商品
	 * @param nameKeyword 商品名稱關鍵字 (可選)，用於模糊搜尋商品名稱
	 * @param page        頁碼 (從0開始，默認為0)
	 * @param size        每頁商品數量 (默認為12)
	 * @return 分頁的商品 DTO 列表，包含分頁信息和商品基本信息
	 */
	@GetMapping("/public/shop/{shopId}")
	public ResponseEntity<Page<ProductPublicDTO>> getShopProducts(@PathVariable Integer shopId,
			@RequestParam(required = false) Integer category1Id, @RequestParam(required = false) Integer category2Id,
			@RequestParam(required = false) String nameKeyword, @RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "12") int size) {

		Page<ProductPublicDTO> publicDtoPage = productService.findPublicProducts(shopId, category1Id, category2Id,
				nameKeyword, page, size);

		return ResponseEntity.ok(publicDtoPage);
	}

	/**
	 * 獲取商品所有圖片
	 */
	@GetMapping("/{productId}/images")
	public ResponseEntity<List<ProductImage>> getProductImages(@PathVariable Integer productId) {
		return ResponseEntity.ok(productService.getProductImages(productId));
	}

	/**
	 * 獲取商品主圖
	 */
	@GetMapping("/{productId}/images/primary")
	public ResponseEntity<ProductImage> getProductPrimaryImage(@PathVariable Integer productId) {
		return ResponseEntity.ok(productService.getProductPrimaryImage(productId));
	}

	/**
	 * 新增商品
	 */
	@PostMapping
	public ResponseEntity<ProductResDTO> createProduct(@AuthenticationPrincipal User currentUser, // 注意這裡改用了標準的User類
			@RequestParam @Size(min = 2, max = 100, message = "商品名稱長度必須在2到100個字符之間") String productName,
			@RequestParam(required = false) @Size(max = 2000, message = "商品描述不能超過2000個字符") String description,
			@RequestParam Integer category1Id, @RequestParam Integer category2Id,
			@RequestParam(required = false, defaultValue = "false") Boolean active,
			@RequestPart(required = false) List<MultipartFile> images) {

		// 從JWT claims中獲取userId
		Integer userId = getUserIdFromAuthentication();

		ProductResDTO createdProduct = productService.create(userId, productName, description, category1Id, category2Id,
				active, images);

		return ResponseEntity.status(HttpStatus.CREATED).body(createdProduct);
	}

	// 輔助方法：從當前認證上下文中獲取userId
	private Integer getUserIdFromAuthentication() {
		if (SecurityContextHolder.getContext().getAuthentication() instanceof JsonWebTokenAuthentication) {
			JsonWebTokenAuthentication auth = (JsonWebTokenAuthentication) SecurityContextHolder.getContext()
					.getAuthentication();
			String token = (String) auth.getCredentials();

			// 使用您的jsonWebTokenUtility從token中提取userId
			JWTClaimsSet claims = jsonWebTokenUtility.validateToken(token);
			try {
				if (claims != null && claims.getClaim("userId") != null) {
					return ((Number) claims.getClaim("userId")).intValue();
				}
			} catch (Exception e) {
				throw new IllegalStateException("無法從token中提取用戶ID", e);
			}
		}
		throw new IllegalStateException("用戶未正確認證或token中沒有userId");
	}

	/**
	 * 更新商品
	 */
	@PutMapping("/{id}")
	public ResponseEntity<ProductResDTO> updateProduct(@PathVariable Integer id,
			@RequestParam(required = false) @Size(min = 2, max = 100, message = "商品名稱長度必須在2到100個字符之間") String productName,
			@RequestParam(required = false) @Size(max = 2000, message = "商品描述不能超過2000個字符") String description,
			@RequestParam(required = false) Boolean active,
			@RequestPart(required = false) List<MultipartFile> newImages,
			@RequestParam(required = false) List<Integer> deleteImageIds,
			@RequestParam(required = false) Integer primaryImageId) {

		// 創建ProductUpdateDTO對象
		ProductUpdateDTO productDTO = new ProductUpdateDTO();
		productDTO.setProductName(productName);
		productDTO.setDescription(description);
		productDTO.setActive(active);

		ProductResDTO updatedProduct = productService.update(id, productName, description, active, newImages,
				deleteImageIds, primaryImageId);

		return ResponseEntity.ok(updatedProduct);
	}

	/**
	 * 刪除商品
	 */
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteProduct(@PathVariable Integer id) {
		productService.delete(id);
		return ResponseEntity.noContent().build();
	}

	/**
	 * 更新商品主圖
	 */
	@PutMapping("/{productId}/images/{imageId}/primary")
	public ResponseEntity<Void> updatePrimaryImage(@PathVariable Integer productId, @PathVariable Integer imageId) {

		productService.updatePrimaryImage(productId, imageId);
		return ResponseEntity.ok().build();
	}

	/**
	 * 新增商品圖片
	 */
	@PostMapping("/{productId}/images")
	public ResponseEntity<ProductImage> addProductImage(@PathVariable Integer productId,
			@RequestPart MultipartFile image, @RequestParam(required = false) Boolean isPrimary) {

		ProductImage addedImage = productService.addProductImage(productId, image, isPrimary);
		return ResponseEntity.status(HttpStatus.CREATED).body(addedImage);
	}

	/**
	 * 刪除商品圖片
	 */
	@DeleteMapping("/{productId}/images/{imageId}")
	public ResponseEntity<Void> deleteProductImage(@PathVariable Integer productId, @PathVariable Integer imageId) {

		productService.deleteProductImage(productId, imageId);
		return ResponseEntity.noContent().build();
	}

	// --------------------------↓by ccliu↓--------------------

	// 取得指定商店的所有產品（基本資訊）
	@GetMapping("/shop/{shopId}")
	public ResponseEntity<?> getProductsByShopId(@PathVariable Integer shopId) {
		List<Product> products = productService.getProductsByShopId(shopId);

		if (products.isEmpty()) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body("找不到該商店的產品，shopId=" + shopId);
		}
		return ResponseEntity.ok(products);
	}

	// 取得指定商店的所有產品（包括 SKU 和圖片等詳細資訊）
	@GetMapping("/shop/{shopId}/details")
	public ResponseEntity<?> getProductsWithDetailsByShopId(@PathVariable Integer shopId) {
		List<Product> products = productService.getProductsWithDetailsByShopId(shopId);

		if (products.isEmpty()) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body("找不到該商店的產品詳細資料，shopId=" + shopId);
		}
		return ResponseEntity.ok(products);
	}
	/**
	 * 豐富商品數據，計算最低價和最高價
	 */
	@GetMapping("/{productId}/price-info")
	public ResponseEntity<?> getProductPriceInfo(@PathVariable Integer productId) {
	    try {
	        // 獲取產品詳情
	        ProductResDTO product = productService.getById(productId);
	        
	        // 計算最低價和最高價
	        Map<String, Object> priceInfo = calculateProductPriceRange(productId);
	        
	        return ResponseEntity.ok(priceInfo);
	    } catch (Exception e) {
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	                .body(Map.of("error", "無法獲取商品價格信息: " + e.getMessage()));
	    }
	}

	/**
	 * 計算產品的價格範圍（最低價和最高價）
	 */
	private Map<String, Object> calculateProductPriceRange(Integer productId) {
	    Map<String, Object> priceInfo = new HashMap<>();
	    
	    try {
	        // 獲取產品實體
	        Product product = productRepository.findById(productId)
	                .orElseThrow(() -> new RuntimeException("商品不存在"));
	        
	        // 如果產品有 SKU 列表，計算最低價和最高價
	        if (product.getSkuList() != null && !product.getSkuList().isEmpty()) {
	            List<BigDecimal> prices = product.getSkuList().stream()
	                    .map(sku -> sku.getPrice())
	                    .filter(price -> price != null && price.compareTo(BigDecimal.ZERO) > 0)
	                    .collect(Collectors.toList());
	            
	            if (!prices.isEmpty()) {
	                BigDecimal minPrice = Collections.min(prices);
	                BigDecimal maxPrice = Collections.max(prices);
	                
	                priceInfo.put("minPrice", minPrice);
	                priceInfo.put("maxPrice", maxPrice);
	                priceInfo.put("hasPriceRange", !minPrice.equals(maxPrice));
	                priceInfo.put("priceFormatted", minPrice.equals(maxPrice) 
	                    ? formatPrice(minPrice) 
	                    : formatPrice(minPrice) + " - " + formatPrice(maxPrice));
	            } else {
	                priceInfo.put("minPrice", null);
	                priceInfo.put("maxPrice", null);
	                priceInfo.put("hasPriceRange", false);
	                priceInfo.put("priceFormatted", "未定價");
	            }
	        } else {
	            priceInfo.put("minPrice", null);
	            priceInfo.put("maxPrice", null);
	            priceInfo.put("hasPriceRange", false);
	            priceInfo.put("priceFormatted", "未定價");
	        }
	        
	        // 添加其他有用的價格資訊
	        priceInfo.put("productId", productId);
	        priceInfo.put("hasValidPrice", priceInfo.get("minPrice") != null);
	        
	        return priceInfo;
	    } catch (Exception e) {
	        priceInfo.put("error", "計算價格範圍時出錯: " + e.getMessage());
	        return priceInfo;
	    }
	}

	/**
	 * 格式化價格顯示
	 */
	private String formatPrice(BigDecimal price) {
	    if (price == null) return "未定價";
	    return price.setScale(0, java.math.RoundingMode.HALF_UP).toString();
	}


}