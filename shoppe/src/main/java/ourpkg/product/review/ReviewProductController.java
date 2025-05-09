package ourpkg.product.review;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import ourpkg.auth.mail.EmailService;
import ourpkg.product.Product;
import ourpkg.sku.Sku;
import ourpkg.user_role_permission.user.User;
import ourpkg.user_role_permission.user.service.UserService;

@RestController
@RequestMapping("/api/admin/products")
public class ReviewProductController {

	@Autowired
	private ReviewProductService productService;

	@Autowired
	private UserService userService;

	@Autowired
	private EmailService emailService;

	private static final Logger logger = LoggerFactory.getLogger(ReviewProductController.class);

	@PutMapping("/{productId}/review")
	public ResponseEntity<Map<String, Object>> updateReviewStatus(@PathVariable Integer productId,
			@RequestBody ReviewStatusRequest request, HttpServletRequest httpRequest) {

		Map<String, Object> response = new HashMap<>();

		// 構建伺服器基礎URL
		String serverBaseUrl = getServerBaseUrl(httpRequest);

		try {
			// 更新商品審核狀態
			Integer adminId = request.getAdminId(); // 從請求中獲取管理員ID
			boolean reviewStatus = request.getReviewStatus(); // 假設 ReviewStatusRequest 中的 reviewStatus 為 boolean 類型
			String reviewComment = request.getReviewComment();

			Product updatedProduct = productService.updateReviewStatus(productId, reviewStatus, reviewComment, adminId);

			// 轉換為 DTO
			ReviewProductDTO dto = new ReviewProductDTO(updatedProduct);

			// 無論審核通過或不通過，都發送郵件通知
			// 獲取商品所有者的信息
			Optional<User> op = userService.findById(updatedProduct.getShop().getUser().getUserId());

			User productOwner = op.get();

			if (productOwner != null && productOwner.getEmail() != null) {
				// 構建商品信息列表
				List<Map<String, Object>> productsList = new ArrayList<>();
				Map<String, Object> productInfo = new HashMap<>();
				productInfo.put("id", updatedProduct.getProductId());
				productInfo.put("name", updatedProduct.getName());

				// 從 List<Sku> 中取得價格
				Object price = null;
				List<Sku> skuList = updatedProduct.getSkuList();
				if (skuList != null && !skuList.isEmpty()) {
				    // 方案1: 使用第一個SKU的價格
				    Sku firstSku = skuList.get(0);
				    if (firstSku != null && firstSku.getPrice() != null) {
				        price = firstSku.getPrice();
				    }
				    
				    // 方案2: 尋找最低價格（可選）
				    /*
				    BigDecimal minPrice = null;
				    for (Sku sku : skuList) {
				        if (sku != null && sku.getPrice() != null) {
				            if (minPrice == null || sku.getPrice().compareTo(minPrice) < 0) {
				                minPrice = sku.getPrice();
				            }
				        }
				    }
				    price = minPrice;
				    */
				    
				    // 方案3: 價格範圍（可選）
				    /*
				    BigDecimal minPrice = null;
				    BigDecimal maxPrice = null;
				    for (Sku sku : skuList) {
				        if (sku != null && sku.getPrice() != null) {
				            if (minPrice == null || sku.getPrice().compareTo(minPrice) < 0) {
				                minPrice = sku.getPrice();
				            }
				            if (maxPrice == null || sku.getPrice().compareTo(maxPrice) > 0) {
				                maxPrice = sku.getPrice();
				            }
				        }
				    }
				    // 如果最低價和最高價相同，則顯示單一價格
				    if (minPrice != null && maxPrice != null) {
				        if (minPrice.equals(maxPrice)) {
				            price = minPrice;
				        } else {
				            price = minPrice + " ~ " + maxPrice; // 價格範圍
				        }
				    }
				    */
				}

				// 確保價格有值
				if (price == null) {
				    // 如果無法獲取價格，設置為零或默認值
				    productInfo.put("price", "0");
				} else {
				    productInfo.put("price", price);
				}

				// 獲取商品圖片URL - 正確獲取ProductImage對象的路徑
				String imageUrl = "";
				if (updatedProduct.getProductImages() != null && !updatedProduct.getProductImages().isEmpty()) {
					try {
						// 假設 getImages() 返回 ProductImage 對象列表
						Object images = updatedProduct.getProductImages();

						if (images instanceof List) {
							List<?> imagesList = (List<?>) images;
							if (!imagesList.isEmpty()) {
								Object firstImage = imagesList.get(0);

								// 使用反射獲取 ProductImage 對象的路徑屬性
								// 根據你的 ProductImage 類結構，這可能是 getImagePath(), getPath(), getUrl() 等方法
								Method getPathMethod = firstImage.getClass().getMethod("getImagePath");
								String imagePath = (String) getPathMethod.invoke(firstImage);

								if (imagePath != null && !imagePath.isEmpty()) {
									imageUrl = serverBaseUrl + imagePath;
									System.out.println("圖片URL: " + imageUrl);
								}

							}
						} else {
							logger.warn("商品圖片不是列表格式: " + images.getClass().getName());
						}
					} catch (Exception e) {
						logger.error("處理商品圖片時發生錯誤: " + e.getMessage(), e);
					}
				}

				productInfo.put("imageUrl", imageUrl);
				System.out.println(imageUrl.toString());
				productInfo.put("reviewComment", reviewComment); // 審核評論
				productsList.add(productInfo);

				// 發送郵件通知
				emailService.sendProductReviewNotificationEmail(productOwner.getEmail(), productOwner.getUsername(),
						reviewStatus, // 審核狀態: true為通過，false為不通過
						productsList);
			}

			response.put("success", true);
			response.put("message", reviewStatus ? "商品審核已通過" : "商品審核未通過");
			response.put("product", dto); // 返回更新後的 DTO
			return ResponseEntity.ok(response);

		} catch (Exception e) {
			response.put("success", false);
			response.put("message", e.getMessage());
			return ResponseEntity.badRequest().body(response);
		}
	}

	/**
	 * 從HTTP請求中獲取伺服器基礎URL
	 */
	private String getServerBaseUrl(HttpServletRequest request) {
		String scheme = request.getScheme();
		String serverName = request.getServerName();
		int serverPort = request.getServerPort();
		String contextPath = request.getContextPath();

		// 根據是否是默認端口決定是否顯示端口號
		boolean isDefaultPort = (scheme.equals("http") && serverPort == 80)
				|| (scheme.equals("https") && serverPort == 443);

		StringBuilder url = new StringBuilder();
		url.append(scheme).append("://").append(serverName);

		if (!isDefaultPort) {
			url.append(":").append(serverPort);
		}

		url.append(contextPath);

		return url.toString();
	}

	/**
	 * 分頁獲取所有商品
	 */
	@GetMapping
	public ResponseEntity<Map<String, Object>> getAllProductsWithPagination(@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size, @RequestParam(defaultValue = "productId") String sortBy,
			@RequestParam(defaultValue = "desc") String direction) {

		return getPageResponse(page, size, sortBy, direction,
				(pageable) -> productService.findAllProductsWithPagination(pageable));
	}

	/**
	 * 根據店鋪ID分頁獲取商品
	 */
	@GetMapping("/shop/{shopId}")
	public ResponseEntity<Map<String, Object>> getProductsByShopIdWithPagination(@PathVariable Integer shopId,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size,
			@RequestParam(defaultValue = "productId") String sortBy,
			@RequestParam(defaultValue = "desc") String direction) {

		return getPageResponse(page, size, sortBy, direction,
				(pageable) -> productService.findProductsByShopIdWithPagination(shopId, pageable));
	}

	/**
	 * 根據一級分類分頁獲取商品
	 */
	@GetMapping("/category1/{category1Id}")
	public ResponseEntity<Map<String, Object>> getProductsByCategory1IdWithPagination(@PathVariable Integer category1Id,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size,
			@RequestParam(defaultValue = "productId") String sortBy,
			@RequestParam(defaultValue = "desc") String direction) {

		return getPageResponse(page, size, sortBy, direction,
				(pageable) -> productService.findProductsByCategory1IdWithPagination(category1Id, pageable));
	}

	/**
	 * 根據二級分類分頁獲取商品
	 */
	@GetMapping("/category2/{category2Id}")
	public ResponseEntity<Map<String, Object>> getProductsByCategory2IdWithPagination(@PathVariable Integer category2Id,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size,
			@RequestParam(defaultValue = "productId") String sortBy,
			@RequestParam(defaultValue = "desc") String direction) {

		return getPageResponse(page, size, sortBy, direction,
				(pageable) -> productService.findProductsByCategory2IdWithPagination(category2Id, pageable));
	}

	/**
	 * 根據店鋪ID和一級分類分頁獲取商品
	 */
	@GetMapping("/shop/{shopId}/category1/{category1Id}")
	public ResponseEntity<Map<String, Object>> getProductsByShopIdAndCategory1IdWithPagination(
			@PathVariable Integer shopId, @PathVariable Integer category1Id, @RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size, @RequestParam(defaultValue = "productId") String sortBy,
			@RequestParam(defaultValue = "desc") String direction) {

		return getPageResponse(page, size, sortBy, direction, (pageable) -> productService
				.findProductsByShopIdAndCategory1IdWithPagination(shopId, category1Id, pageable));
	}

	/**
	 * 根據店鋪ID和二級分類分頁獲取商品
	 */
	@GetMapping("/shop/{shopId}/category2/{category2Id}")
	public ResponseEntity<Map<String, Object>> getProductsByShopIdAndCategory2IdWithPagination(
			@PathVariable Integer shopId, @PathVariable Integer category2Id, @RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size, @RequestParam(defaultValue = "productId") String sortBy,
			@RequestParam(defaultValue = "desc") String direction) {

		return getPageResponse(page, size, sortBy, direction, (pageable) -> productService
				.findProductsByShopIdAndCategory2IdWithPagination(shopId, category2Id, pageable));
	}

	/**
	 * 根據關鍵字搜尋商品
	 */
	@GetMapping("/search")
	public ResponseEntity<Map<String, Object>> searchProductsByName(@RequestParam String keyword,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size,
			@RequestParam(defaultValue = "productId") String sortBy,
			@RequestParam(defaultValue = "desc") String direction) {

		return getPageResponse(page, size, sortBy, direction,
				(pageable) -> productService.findProductsByNameContaining(keyword, pageable));
	}

	/**
	 * 根據上架狀態搜尋商品
	 */
	@GetMapping("/status/active")
	public ResponseEntity<Map<String, Object>> getProductsByActiveStatus(@RequestParam Boolean active,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size,
			@RequestParam(defaultValue = "productId") String sortBy,
			@RequestParam(defaultValue = "desc") String direction) {

		return getPageResponse(page, size, sortBy, direction,
				(pageable) -> productService.findProductsByActiveStatus(active, pageable));
	}

	/**
	 * 根據審核狀態搜尋商品
	 */
	@GetMapping("/status/review")
	public ResponseEntity<Map<String, Object>> getProductsByReviewStatus(@RequestParam Boolean reviewStatus,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size,
			@RequestParam(defaultValue = "productId") String sortBy,
			@RequestParam(defaultValue = "desc") String direction) {

		return getPageResponse(page, size, sortBy, direction,
				(pageable) -> productService.findProductsByReviewStatus(reviewStatus, pageable));
	}

	/**
	 * 根據上架狀態和審核狀態搜尋商品
	 */
	@GetMapping("/status/combined")
	public ResponseEntity<Map<String, Object>> getProductsByActiveAndReviewStatus(@RequestParam Boolean active,
			@RequestParam Boolean reviewStatus, @RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size, @RequestParam(defaultValue = "productId") String sortBy,
			@RequestParam(defaultValue = "desc") String direction) {

		return getPageResponse(page, size, sortBy, direction,
				(pageable) -> productService.findProductsByActiveAndReviewStatus(active, reviewStatus, pageable));
	}

	/**
	 * 通用方法：創建分頁請求並處理分頁響應
	 */
	private ResponseEntity<Map<String, Object>> getPageResponse(int page, int size, String sortBy, String direction,
			java.util.function.Function<Pageable, Page<Product>> pageSupplier) {

		Sort.Direction sortDirection = direction.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
		Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));

		try {
			Page<Product> productPage = pageSupplier.apply(pageable);

			// 將 Product 轉換為 ReviewProductDTO
			List<ReviewProductDTO> dtoList = productPage.getContent().stream().map(ReviewProductDTO::new)
					.collect(Collectors.toList());

			Map<String, Object> response = new HashMap<>();
			response.put("products", dtoList); // 返回 DTO 列表而不是實體
			response.put("currentPage", productPage.getNumber());
			response.put("totalItems", productPage.getTotalElements());
			response.put("totalPages", productPage.getTotalPages());

			return ResponseEntity.ok(response);
		} catch (UnsupportedOperationException e) {
			Map<String, Object> errorResponse = new HashMap<>();
			errorResponse.put("error", "此查詢方法尚未實現分頁功能");
			errorResponse.put("message", e.getMessage());
			return ResponseEntity.badRequest().body(errorResponse);
		}
	}
	
	/**
	 * 根據創建日期範圍搜尋商品
	 */
	@GetMapping("/date-range")
	public ResponseEntity<Map<String, Object>> getProductsByDateRange(
	        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
	        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate,
	        @RequestParam(defaultValue = "0") int page,
	        @RequestParam(defaultValue = "10") int size,
	        @RequestParam(defaultValue = "productId") String sortBy,
	        @RequestParam(defaultValue = "desc") String direction) {

	    return getPageResponse(page, size, sortBy, direction,
	            (pageable) -> productService.findProductsByCreatedDateRange(startDate, endDate, pageable));
	}

	/**
	 * 進階組合條件搜尋
	 */
	@GetMapping("/advanced-search")
	public ResponseEntity<Map<String, Object>> getProductsByAdvancedSearch(
	        @RequestParam(required = false) String keyword,
	        @RequestParam(required = false) BigDecimal minPrice,
	        @RequestParam(required = false) BigDecimal maxPrice,
	        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
	        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate,
	        @RequestParam(required = false) Integer shopId,
	        @RequestParam(required = false) Integer category1Id,
	        @RequestParam(required = false) Integer category2Id,
	        @RequestParam(required = false) Boolean active,
	        @RequestParam(required = false) Boolean reviewStatus,
	        @RequestParam(defaultValue = "0") int page,
	        @RequestParam(defaultValue = "10") int size,
	        @RequestParam(defaultValue = "productId") String sortBy,
	        @RequestParam(defaultValue = "desc") String direction) {

	    return getPageResponse(page, size, sortBy, direction,
	            (pageable) -> productService.findProductsByAdvancedCriteria(
	                    keyword, minPrice, maxPrice, startDate, endDate,
	                    shopId, category1Id, category2Id, active, reviewStatus,
	                    pageable));
	}
}