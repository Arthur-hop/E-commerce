package ourpkg.product.version2.complete_create;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.JWTClaimsSet;

import lombok.RequiredArgsConstructor;
import ourpkg.category.Category1;
import ourpkg.category.Category2;
import ourpkg.category.repo.Category1Repository;
import ourpkg.category.repo.Category2Repository;
import ourpkg.jwt.JsonWebTokenAuthentication;
import ourpkg.jwt.JsonWebTokenUtility;
import ourpkg.product.Product;
import ourpkg.product.ProductImage;
import ourpkg.product.ProductImageRepository;
import ourpkg.product.ProductRepository;
import ourpkg.product.ProductShopRepository;
import ourpkg.product.version2.controller_service.ImageFileService;
import ourpkg.shop.Shop;
import ourpkg.sku.Sku;
import ourpkg.sku.SkuRepository;
import ourpkg.user_role_permission.user.User;

/**
 * 商品完整創建控制器 - 提供一次性建立商品基本資訊、圖片和SKU的功能
 */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Validated
public class ProductCompleteCreateController {

	private final JsonWebTokenUtility jsonWebTokenUtility;
	
	private static final Logger logger = LoggerFactory.getLogger(ProductCompleteCreateController.class);

	@Autowired
	private final ProductRepository repo;
	@Autowired
	private final ProductImageRepository productImageRepo;
	@Autowired
	private final ProductShopRepository productShopRepository;
	@Autowired
	private final Category1Repository category1Repo;
	@Autowired
	private final Category2Repository category2Repo;
	@Autowired
	private final SkuRepository skuRepo;
	@Autowired
	private final ImageFileService imageFileService;
	@Autowired
	private final ObjectMapper objectMapper;

	/**
	 * 一次性新增商品、圖片和SKU
	 */
	@PostMapping("/complete")
	public ResponseEntity<ProductCompleteResDTO2> createCompleteProduct(@AuthenticationPrincipal User currentUser,
			@RequestParam("productName") String productName, @RequestParam("description") String description,
			@RequestParam("category1Id") Integer category1Id, @RequestParam("category2Id") Integer category2Id,
			@RequestParam(value = "active", defaultValue = "false") Boolean active,
			@RequestParam(value = "skusJson", required = false) String skusJson,
			@RequestPart(required = false) List<MultipartFile> images) {

		try {
			// 從JWT claims中獲取userId
			Integer userId = getUserIdFromAuthentication();

			// 解析SKU JSON數據
			List<SkuCreateDTO2> skus = new ArrayList<>();
			if (skusJson != null && !skusJson.isEmpty()) {
				skus = objectMapper.readValue(skusJson,
						objectMapper.getTypeFactory().constructCollectionType(List.class, SkuCreateDTO2.class));
			}

			// 創建DTO2對象
			ProductCompleteCreateDTO2 productData = new ProductCompleteCreateDTO2();
			productData.setProductName(productName);
			productData.setDescription(description);
			productData.setCategory1Id(category1Id);
			productData.setCategory2Id(category2Id);
			productData.setActive(active);
			productData.setSkus(skus);

			// 調用服務方法創建完整產品
			ProductCompleteResDTO2 completeProduct = createComplete(userId, productData, images);

			return ResponseEntity.status(HttpStatus.CREATED).body(completeProduct);
		} catch (Exception e) {
			logger.error("處理請求時出錯", e);
			throw new RuntimeException("處理請求時出錯: " + e.getMessage(), e);
		}
	}

	/**
	 * 一次性新增商品基本資訊、圖片和庫存
	 */
	@Transactional
	public ProductCompleteResDTO2 createComplete(Integer userId, ProductCompleteCreateDTO2 productData,
			List<MultipartFile> images) {

		try {
			logger.info("開始一次性創建商品: {}", productData.getProductName());

			// 確認使用者有店鋪
			Shop shop = productShopRepository.findByUser_UserId(userId)
					.orElseThrow(() -> new IllegalArgumentException("用戶沒有對應的店鋪"));

			// 確認分類存在
			Category1 category1 = category1Repo.findById(productData.getCategory1Id())
					.orElseThrow(() -> new IllegalArgumentException("一級分類 ID 不存在"));

			Category2 category2 = category2Repo.findById(productData.getCategory2Id())
					.orElseThrow(() -> new IllegalArgumentException("二級分類 ID 不存在"));

			// 確認分類組合有效
			if (!category2Repo.existsByIdAndCategory1List_Id(productData.getCategory2Id(),
					productData.getCategory1Id())) {
				throw new IllegalArgumentException("該一級分類 ID 和 二級分類 ID 的組合不存在：" + productData.getCategory1Id() + " - "
						+ productData.getCategory2Id());
			}

			// 建立商品實體
			Product product = new Product();
			product.setProductName(productData.getProductName());
			product.setDescription(productData.getDescription());
			product.setActive(productData.getActive() != null ? productData.getActive() : false);
			product.setShop(shop);
			product.setCategory1(category1);
			product.setCategory2(category2);
			product.setIsDeleted(false);
			product.setProductImages(new ArrayList<>()); // 初始化空集合以避免空指針

			// 保存商品實體以獲取 ID
			Product savedProduct = repo.saveAndFlush(product);

			// 再次確認保存的實體有有效的 ID
			if (savedProduct.getProductId() == null) {
				throw new RuntimeException("商品保存後無法獲取有效的 ID");
			}

			logger.info("商品實體已建立, ID: {}", savedProduct.getProductId());

			// 處理商品圖片
			processProductImages(savedProduct, images);

			// 處理 SKU 資料
			List<SkuResDTO2> createdSkus = createProductSkus(savedProduct, productData.getSkus());

			logger.info("成功創建 {} 個 SKU", createdSkus.size());

			// 重新查詢商品以獲取完整資訊
			Product finalProduct = repo.findById(savedProduct.getProductId())
					.orElseThrow(() -> new RuntimeException("無法找到剛保存的商品"));

			// 組合結果並返回
			return buildCompleteResponse(finalProduct, createdSkus);

		} catch (Exception e) {
			// 記錄異常詳細信息
			logger.error("創建完整商品失敗", e);
			// 重新拋出異常，但添加更多信息
			throw new RuntimeException("創建完整商品失敗: " + e.getMessage(), e);
		}
	}

	/**
	 * 處理商品圖片上傳
	 */
	private void processProductImages(Product product, List<MultipartFile> images) {
		// 檢查是否有圖片上傳
		if (images == null || images.isEmpty()) {
			logger.info("沒有提供圖片，將使用默認圖片");

			// 創建默認圖片
			ProductImage defaultImage = new ProductImage();
			defaultImage.setProduct(product);
			defaultImage.setImagePath("/uploads/default-product-image.jpg");
			defaultImage.setIsPrimary(true);
			defaultImage.setDisplayOrder(0);

			productImageRepo.saveAndFlush(defaultImage);
			product.getProductImages().add(defaultImage);
			return;
		}

		logger.info("處理 {} 張圖片", images.size());
		boolean hasPrimary = false;
		int displayOrder = 0;

		for (MultipartFile image : images) {
			if (image != null && !image.isEmpty()) {
				try {
					// 使用ImageFileService儲存圖片
					String imagePath = imageFileService.saveImage(image);
					logger.info("已保存圖片: {}", imagePath);

					// 創建商品圖片實體並設置關聯
					ProductImage productImage = new ProductImage();
					productImage.setProduct(product);
					productImage.setImagePath(imagePath);
					productImage.setDisplayOrder(displayOrder++);

					// 將第一張圖片設為主圖
					if (!hasPrimary) {
						productImage.setIsPrimary(true);
						hasPrimary = true;
					} else {
						productImage.setIsPrimary(false);
					}

					ProductImage savedImage = productImageRepo.save(productImage);
					product.getProductImages().add(savedImage);
				} catch (Exception e) {
					logger.error("圖片處理失敗", e);
					throw new RuntimeException("圖片處理失敗: " + e.getMessage(), e);
				}
			}
		}

		// 如果沒有成功保存任何圖片，添加默認圖片
		if (product.getProductImages().isEmpty()) {
			logger.info("未能成功處理任何圖片，將使用默認圖片");

			ProductImage defaultImage = new ProductImage();
			defaultImage.setProduct(product);
			defaultImage.setImagePath("/uploads/default-product-image.jpg");
			defaultImage.setIsPrimary(true);
			defaultImage.setDisplayOrder(0);

			productImageRepo.saveAndFlush(defaultImage);
			product.getProductImages().add(defaultImage);
		}
	}

	/**
	 * 創建商品的所有SKU
	 */
	private List<SkuResDTO2> createProductSkus(Product product, List<SkuCreateDTO2> skuDTO2s) {
		List<SkuResDTO2> createdSkus = new ArrayList<>();

		if (skuDTO2s == null || skuDTO2s.isEmpty()) {
			logger.info("沒有提供SKU資料，不創建SKU");
			return createdSkus;
		}

		logger.info("開始創建 {} 個 SKU", skuDTO2s.size());

		// 獲取已存在的規格組合 (以確保唯一性)
		List<String> existingSpecPairs = new ArrayList<>();

		for (SkuCreateDTO2 skuDTO2 : skuDTO2s) {
			// 確認價格和庫存符合要求
			if (skuDTO2.getStock() == null) {
				throw new IllegalArgumentException("SKU 庫存不能為空");
			}
			if (skuDTO2.getStock() < 0) {
				throw new IllegalArgumentException("SKU 庫存不能小於 0");
			}
			if (skuDTO2.getPrice() == null) {
				throw new IllegalArgumentException("SKU 價格不能為空");
			}
			if (skuDTO2.getPrice().compareTo(BigDecimal.ZERO) < 0) {
				throw new IllegalArgumentException("SKU 價格不能小於 0");
			}

			// 檢查規格組合唯一性
			if (skuDTO2.getSpecPairs() != null && !skuDTO2.getSpecPairs().isEmpty()) {
				Sku tempSku = new Sku();
				tempSku.setSpecPairsFromMap(skuDTO2.getSpecPairs());
				String specPairsJson = tempSku.getSpecPairs();

				if (existingSpecPairs.contains(specPairsJson)) {
					throw new IllegalArgumentException("規格組合重複: " + skuDTO2.getSpecPairs());
				}

				existingSpecPairs.add(specPairsJson);
			}

			// 創建 SKU
			Sku sku = new Sku();
			sku.setProduct(product);
			sku.setStock(skuDTO2.getStock());
			sku.setPrice(skuDTO2.getPrice());
			sku.setSpecPairsFromMap(skuDTO2.getSpecPairs());
			sku.setIsDeleted(false);

			Sku savedSku = skuRepo.save(sku);

			// 生成規格描述文字
			String specDescription = generateSpecDescription(skuDTO2.getSpecPairs());
			SkuResDTO2 skuResDTO2 = new SkuResDTO2();
			skuResDTO2.setSkuId(savedSku.getSkuId());
			skuResDTO2.setProductId(product.getProductId());
			skuResDTO2.setProductName(product.getProductName());
			skuResDTO2.setStock(savedSku.getStock());
			skuResDTO2.setPrice(savedSku.getPrice());
			skuResDTO2.setSpecPairs(skuDTO2.getSpecPairs());
			skuResDTO2.setSpecDescription(specDescription);
			skuResDTO2.setIsDeleted(false);

			createdSkus.add(skuResDTO2);
		}

		return createdSkus;
	}

	/**
	 * 構建完整的響應結果 - 不使用Mapper
	 */
	private ProductCompleteResDTO2 buildCompleteResponse(Product product, List<SkuResDTO2> skus) {
		// 手動構建ProductResDTO2，不使用Mapper
		ProductResDTO2 productResDTO2 = new ProductResDTO2();
		productResDTO2.setProductId(product.getProductId());
		productResDTO2.setProductName(product.getProductName());
		productResDTO2.setDescription(product.getDescription());
		productResDTO2.setActive(product.getActive());
		productResDTO2.setCategory1Id(product.getCategory1().getId());
		productResDTO2.setCategory1Name(product.getCategory1().getName());
		productResDTO2.setCategory2Id(product.getCategory2().getId());
		productResDTO2.setCategory2Name(product.getCategory2().getName());
		productResDTO2.setIsDeleted(product.getIsDeleted());
		productResDTO2.setCreatedAt(product.getCreatedAt());
		productResDTO2.setUpdatedAt(product.getUpdatedAt());

		// 設置商店信息
		if (product.getShop() != null) {
			productResDTO2.setShopId(product.getShop().getShopId());
			productResDTO2.setShopName(product.getShop().getShopName());
		}

		// 設置主圖 URL (如果有的話)
		String primaryImageUrl = null;
		List<String> imageUrls = new ArrayList<>();
		for (ProductImage img : product.getProductImages()) {
			imageUrls.add(img.getImagePath());
			if (img.getIsPrimary()) {
				primaryImageUrl = img.getImagePath();
			}
		}
		productResDTO2.setPrimaryImageUrl(primaryImageUrl);
		productResDTO2.setImageUrls(imageUrls);

		// 設置價格範圍
		if (!skus.isEmpty()) {
			double minPrice = skus.stream().map(sku -> sku.getPrice().doubleValue()).min(Double::compare).orElse(0.0);

			double maxPrice = skus.stream().map(sku -> sku.getPrice().doubleValue()).max(Double::compare).orElse(0.0);

			ProductResDTO2.PriceRangeDTO2 priceRange = new ProductResDTO2.PriceRangeDTO2(minPrice, maxPrice);
			productResDTO2.setPriceRange(priceRange);
		}

		// 設置SKU數量
		productResDTO2.setSkuCount(skus.size());

		// 組合結果並返回
		ProductCompleteResDTO2 result = new ProductCompleteResDTO2();
		result.setProduct(productResDTO2);
		result.setSkus(skus);

		// 添加規格信息
		List<ProductSpecDTO2> specs = extractProductSpecifications(skus);
		result.setSpecifications(specs);

		return result;
	}

	/**
	 * 根據規格鍵值對生成規格描述文字
	 */
	private String generateSpecDescription(Map<String, String> specPairs) {
		if (specPairs == null || specPairs.isEmpty()) {
			return "";
		}

		return specPairs.values().stream().filter(value -> value != null && !value.isEmpty())
				.collect(Collectors.joining("/"));
	}

	/**
	 * 從SKU列表中提取商品規格信息
	 */
	private List<ProductSpecDTO2> extractProductSpecifications(List<SkuResDTO2> skus) {
		if (skus == null || skus.isEmpty()) {
			return Collections.emptyList();
		}

		// 提取所有規格名稱和對應的值
		Map<String, Set<String>> specValueMap = new HashMap<>();

		for (SkuResDTO2 sku : skus) {
			if (sku.getSpecPairs() != null) {
				for (Map.Entry<String, String> entry : sku.getSpecPairs().entrySet()) {
					String specName = entry.getKey();
					String specValue = entry.getValue();

					if (specName != null && !specName.isEmpty() && specValue != null && !specValue.isEmpty()) {
						specValueMap.computeIfAbsent(specName, k -> new HashSet<>()).add(specValue);
					}
				}
			}
		}

		// 轉換為 ProductSpecDTO2 列表
		return specValueMap.entrySet().stream().map(entry -> {
			ProductSpecDTO2 spec = new ProductSpecDTO2();
			spec.setSpecName(entry.getKey());

			List<SpecValueDTO2> values = entry.getValue().stream().map(value -> {
				SpecValueDTO2 valueDTO2 = new SpecValueDTO2();
				valueDTO2.setValue(value);
				return valueDTO2;
			}).collect(Collectors.toList());

			spec.setValues(values);
			return spec;
		}).collect(Collectors.toList());
	}

	/**
	 * 輔助方法：從當前認證上下文中獲取userId
	 * 
	 * 實現從JWT取得用戶ID的邏輯
	 */
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
}
