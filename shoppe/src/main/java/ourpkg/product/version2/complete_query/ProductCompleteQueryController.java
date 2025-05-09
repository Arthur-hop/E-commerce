package ourpkg.product.version2.complete_query;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import ourpkg.order.OrderItemRepository;
import ourpkg.product.Product;
import ourpkg.product.ProductImage;
import ourpkg.product.ProductImageRepository;
import ourpkg.product.ProductRepository;
import ourpkg.shop.Shop;
import ourpkg.sku.Sku;
import ourpkg.sku.SkuRepository;

/**
 * 商品完整查詢控制器 - 提供一次性查詢商品所有相關信息的功能
 */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Validated
public class ProductCompleteQueryController {

	private static final Logger logger = LoggerFactory.getLogger(ProductCompleteQueryController.class);

	@Autowired
	private final ProductRepository productRepo;

	@Autowired
	private final ProductImageRepository productImageRepo;

	@Autowired
	private final SkuRepository skuRepo;

	@Autowired
	private final OrderItemRepository orderItemRepo;

	/**
	 * 一次性查詢商品的所有信息 包含商品基本信息、所屬商店、所屬分類、圖片、庫存和銷售信息
	 * 
	 * @param productId 商品ID
	 * @return 包含商品所有相關信息的完整DTO4
	 */
	@GetMapping("/{productId}/complete-with-sales")
	public ResponseEntity<?> getCompleteProductWithSales(@PathVariable Integer productId) {
		try {
			logger.info("開始一次性查詢商品完整信息，包含銷售數據: ID={}", productId);

			// 調用服務方法查詢完整商品信息
			ProductCompleteResponseDTO4 completeProduct = getCompleteProductInfo(productId, true);

			return ResponseEntity.ok(completeProduct);
		} catch (IllegalArgumentException e) {
			logger.warn("查詢商品時出錯: {}", e.getMessage());
			return ResponseEntity.notFound().build();
		} catch (Exception e) {
			logger.error("處理請求時發生未預期的錯誤", e);
			return ResponseEntity.internalServerError().body("處理請求時出錯: " + e.getMessage());
		}
	}

	/**
	 * 一次性查詢商品的所有基本信息 (不包含銷售數據) 包含商品基本信息、所屬商店、所屬分類、圖片和庫存
	 * 
	 * @param productId 商品ID
	 * @return 包含商品基本相關信息的DTO4
	 */
	@GetMapping("/{productId}/complete")
	public ResponseEntity<?> getCompleteProduct(@PathVariable Integer productId) {
		try {
			logger.info("開始一次性查詢商品基本信息: ID={}", productId);

			// 調用服務方法查詢商品基本信息（不含銷售數據）
			ProductCompleteResponseDTO4 completeProduct = getCompleteProductInfo(productId, false);

			return ResponseEntity.ok(completeProduct);
		} catch (IllegalArgumentException e) {
			logger.warn("查詢商品時出錯: {}", e.getMessage());
			return ResponseEntity.notFound().build();
		} catch (Exception e) {
			logger.error("處理請求時發生未預期的錯誤", e);
			return ResponseEntity.internalServerError().body("處理請求時出錯: " + e.getMessage());
		}
	}

	/**
	 * 查詢商品完整信息的內部實現
	 * 
	 * @param productId    商品ID
	 * @param includeSales 是否包含銷售信息
	 * @return 商品完整信息DTO4
	 */
	private ProductCompleteResponseDTO4 getCompleteProductInfo(Integer productId, boolean includeSales) {
		// 1. 查詢商品基本信息
		Product product = productRepo.findByProductIdAndIsDeletedFalse(productId)
				.orElseThrow(() -> new IllegalArgumentException("該商品 ID 不存在或已被刪除: " + productId));

		logger.info("已找到商品: {}", product.getProductName());

		// 2. 查詢商品圖片
		List<ProductImage> images = productImageRepo.findByProduct_ProductId(productId);

		// 3. 查詢商品SKU
		List<Sku> skus = skuRepo.findByProduct_ProductIdAndIsDeletedFalse(productId);
		logger.info("找到 {} 個SKU", skus.size());

		// 4. 組裝基本信息DTO4
		ProductCompleteResponseDTO4 responseDTO4 = assembleProductBasicInfo(product, images, skus);

		// 5. 如果需要銷售信息，則查詢並添加
		if (includeSales) {
			addSalesInfo(responseDTO4, productId, skus);
			logger.info("已添加銷售信息");
		}

		return responseDTO4;
	}

	/**
	 * 組裝商品基本信息
	 */
	private ProductCompleteResponseDTO4 assembleProductBasicInfo(Product product, List<ProductImage> images,
			List<Sku> skus) {
		// 創建基本的產品信息DTO4
		ProductBasicInfoDTO4 basicInfo = new ProductBasicInfoDTO4();
		basicInfo.setProductId(product.getProductId());
		basicInfo.setProductName(product.getProductName());
		basicInfo.setDescription(product.getDescription());
		basicInfo.setActive(product.getActive());
		basicInfo.setCreatedAt(product.getCreatedAt());
		basicInfo.setUpdatedAt(product.getUpdatedAt());

		// 設置分類信息
		if (product.getCategory1() != null) {
			basicInfo.setCategory1Id(product.getCategory1().getId());
			basicInfo.setCategory1Name(product.getCategory1().getName());
		}

		if (product.getCategory2() != null) {
			basicInfo.setCategory2Id(product.getCategory2().getId());
			basicInfo.setCategory2Name(product.getCategory2().getName());
		}

		// 設置店鋪信息
		ShopInfoDTO4 shopInfo = null;
		if (product.getShop() != null) {
			Shop shop = product.getShop();
			shopInfo = new ShopInfoDTO4();
			shopInfo.setShopId(shop.getShopId());
			shopInfo.setShopName(shop.getShopName());
			shopInfo.setDescription(shop.getDescription());
			shopInfo.setIsActive(shop.getIsActive());
		}

		// 處理圖片
		List<ProductImageDTO4> imageDTO4s = new ArrayList<>();
		ProductImageDTO4 primaryImage = null;

		for (ProductImage image : images) {
			ProductImageDTO4 imageDTO4 = new ProductImageDTO4();
			imageDTO4.setImageId(image.getImageId());
			imageDTO4.setImagePath(image.getImagePath());
			imageDTO4.setIsPrimary(image.getIsPrimary());
			imageDTO4.setDisplayOrder(image.getDisplayOrder());

			imageDTO4s.add(imageDTO4);

			if (image.getIsPrimary()) {
				primaryImage = imageDTO4;
			}
		}

		// 處理SKU和規格
		List<SkuInfoDTO4> skuDTO4s = new ArrayList<>();
		for (Sku sku : skus) {
			SkuInfoDTO4 skuDTO4 = new SkuInfoDTO4();
			skuDTO4.setSkuId(sku.getSkuId());
			skuDTO4.setPrice(sku.getPrice());
			skuDTO4.setStock(sku.getStock());

			// 處理規格鍵值對
			Map<String, String> specPairs = new HashMap<>();
			if (sku.getSpecPairs() != null && !sku.getSpecPairs().isEmpty()) {
				specPairs = sku.getSpecPairsAsMap();
			}
			skuDTO4.setSpecPairs(specPairs);

			// 生成規格描述
			StringBuilder specDescription = new StringBuilder();
			for (String value : specPairs.values()) {
				if (specDescription.length() > 0) {
					specDescription.append("/");
				}
				specDescription.append(value);
			}
			skuDTO4.setSpecDescription(specDescription.toString());

			skuDTO4s.add(skuDTO4);
		}

		// 提取規格信息
		List<ProductSpecDTO4> specifications = extractSpecifications(skuDTO4s);

		// 計算價格範圍
		BigDecimal minPrice = skuDTO4s.stream().map(SkuInfoDTO4::getPrice).min(BigDecimal::compareTo)
				.orElse(BigDecimal.ZERO);

		BigDecimal maxPrice = skuDTO4s.stream().map(SkuInfoDTO4::getPrice).max(BigDecimal::compareTo)
				.orElse(BigDecimal.ZERO);

		PriceRangeDTO4 priceRange = new PriceRangeDTO4(minPrice, maxPrice);
		basicInfo.setPriceRange(priceRange);

		// 計算總庫存
		int totalStock = skuDTO4s.stream().mapToInt(SkuInfoDTO4::getStock).sum();

		// 組裝最終返回對象
		ProductCompleteResponseDTO4 responseDTO4 = new ProductCompleteResponseDTO4();
		responseDTO4.setBasicInfo(basicInfo);
		responseDTO4.setShop(shopInfo);
		responseDTO4.setImages(imageDTO4s);
		responseDTO4.setPrimaryImage(primaryImage);
		responseDTO4.setSkus(skuDTO4s);
		responseDTO4.setSpecifications(specifications);
		responseDTO4.setTotalStock(totalStock);

		return responseDTO4;
	}

	/**
	 * 添加銷售信息
	 */
	private void addSalesInfo(ProductCompleteResponseDTO4 responseDTO4, Integer productId, List<Sku> skus) {
		// 創建銷售信息DTO4
		SalesInfoDTO4 salesInfo = new SalesInfoDTO4();

		// 查詢總銷售量
		Integer totalSoldCount = orderItemRepo.countSoldQuantityByProductId(productId);
		salesInfo.setTotalSoldCount(totalSoldCount != null ? totalSoldCount : 0);

		// 查詢總銷售金額
		BigDecimal totalSalesAmount = orderItemRepo.sumSalesAmountByProductId(productId);
		salesInfo.setTotalSalesAmount(totalSalesAmount != null ? totalSalesAmount : BigDecimal.ZERO);

		// 由於系統中沒有ProductStats，我們將瀏覽次數設為0或從其他來源獲取
		// 這裡可以根據實際情況修改，例如從日誌分析、Redis計數器等獲取
		Integer viewCount = 0; // 暫時設為0
		salesInfo.setTotalViewCount(viewCount);

		// 計算轉化率
		double conversionRate = 0.0;
		if (viewCount > 0 && totalSoldCount != null) {
			conversionRate = (double) totalSoldCount / viewCount;
		}
		salesInfo.setConversionRate(conversionRate);

		// 獲取各SKU銷售數據
		List<SkuSalesDTO4> skuSalesList = new ArrayList<>();

		for (Sku sku : skus) {
			SkuSalesDTO4 skuSales = new SkuSalesDTO4();
			skuSales.setSkuId(sku.getSkuId());

			// 查詢SKU銷售量
			Integer skuSoldCount = orderItemRepo.countSoldQuantityBySkuId(sku.getSkuId());
			skuSales.setSoldCount(skuSoldCount != null ? skuSoldCount : 0);

			// 查詢SKU銷售金額
			BigDecimal skuSalesAmount = orderItemRepo.sumSalesAmountBySkuId(sku.getSkuId());
			skuSales.setTotalAmount(skuSalesAmount != null ? skuSalesAmount : BigDecimal.ZERO);

			skuSales.setCurrentPrice(sku.getPrice());
			skuSales.setStockRemaining(sku.getStock());

			skuSalesList.add(skuSales);
		}

		salesInfo.setSkuSalesDetails(skuSalesList);

		// 獲取最近30天的銷售趨勢
		try {
			Map<String, Integer> dailySales = orderItemRepo.getDailySalesCountLast30Days(productId);
			salesInfo.setDailySalesLast30Days(dailySales);
		} catch (Exception e) {
			logger.warn("獲取商品每日銷售趨勢失敗", e);
			// 如果獲取失敗，使用空Map
			salesInfo.setDailySalesLast30Days(new HashMap<>());
		}

		// 設置最後更新時間
		salesInfo.setLastUpdated(java.time.LocalDateTime.now());

		// 將銷售信息添加到響應DTO4
		responseDTO4.setSalesInfo(salesInfo);
	}

	/**
	 * 從SKU列表中提取規格信息
	 */
	private List<ProductSpecDTO4> extractSpecifications(List<SkuInfoDTO4> skus) {
		if (skus == null || skus.isEmpty()) {
			return new ArrayList<>();
		}

		// 創建一個Map來存儲每種規格類型的值集合
		Map<String, Set<String>> specValueMap = new HashMap<>();

		// 遍歷所有SKU，提取規格信息
		for (SkuInfoDTO4 sku : skus) {
			Map<String, String> specPairs = sku.getSpecPairs();
			if (specPairs != null && !specPairs.isEmpty()) {
				for (Map.Entry<String, String> entry : specPairs.entrySet()) {
					String specName = entry.getKey();
					String specValue = entry.getValue();

					// 將規格值添加到對應的集合中
					specValueMap.computeIfAbsent(specName, k -> new java.util.HashSet<>()).add(specValue);
				}
			}
		}

		// 將Map轉換為DTO4列表
		return specValueMap.entrySet().stream().map(entry -> {
			ProductSpecDTO4 spec = new ProductSpecDTO4();
			spec.setSpecName(entry.getKey());

			List<SpecValueDTO4> specValues = entry.getValue().stream().map(value -> {
				SpecValueDTO4 valueDTO4 = new SpecValueDTO4();
				valueDTO4.setValue(value);
				return valueDTO4;
			}).collect(Collectors.toList());

			spec.setValues(specValues);
			return spec;
		}).collect(Collectors.toList());
	}
}