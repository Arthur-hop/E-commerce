package ourpkg.product.version2.controller_service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import ourpkg.category.Category1;
import ourpkg.category.Category2;
import ourpkg.category.repo.Category1Repository;
import ourpkg.category.repo.Category2Repository;
import ourpkg.product.Product;
import ourpkg.product.ProductImage;
import ourpkg.product.ProductImageRepository;
import ourpkg.product.ProductRepository;
import ourpkg.product.ProductShopRepository;
import ourpkg.product.version2.dto.ProductDetailDTO;
import ourpkg.product.version2.dto.ProductPublicDTO;
import ourpkg.product.version2.dto.ProductResDTO;
import ourpkg.product.version2.dto.ProductSpecDTO;
import ourpkg.product.version2.dto.SpecValueDTO;
import ourpkg.product.version2.mapper.ProductResMapper;
import ourpkg.review.ReviewRepository;
import ourpkg.shop.Shop;
import ourpkg.sku.Sku;
import ourpkg.sku.SkuRepository;
import ourpkg.sku.version2.controller_service.SkuService;
import ourpkg.sku.version2.dto.SkuResDTO;

/**
 * 查詢類方法 商品管理類方法 圖片管理類方法 輔助類私有方法
 */
@Service
@RequiredArgsConstructor
public class ProductService {

	private final ProductRepository repo;
	private final ProductShopRepository productshopRepo; // 需要管理的關聯
	private final Category1Repository category1Repo; // 需要管理的關聯
	private final Category2Repository category2Repo; // 需要管理的關聯
	private final SkuRepository skuRepo; // 不需要管理的關聯
	private final ProductResMapper resMapper;

	private final ImageFileService imageFileService;

	private final ProductImageRepository productImageRepo; // 新增的商品圖片Repository

	private final SkuService skuService;
	
	private final ReviewRepository reviewRepository;

	// 查詢類方法 開始========================================================

	/**
	 * 根據商品ID查詢單一商品（精確查詢）
	 * 
	 * @param id 要查詢的商品ID
	 * @return 商品的資料傳輸對象(DTO)
	 * @throws IllegalArgumentException 當指定ID的商品不存在時拋出
	 */
	public ProductResDTO getById(Integer id) {
		return repo.findByProductIdAndIsDeletedFalse(id).map(resMapper::toDto)
				.orElseThrow(() -> new IllegalArgumentException("該商品 ID 不存在或已被刪除"));
	}

	/**
	 * 根據商品ID查詢單一商品--包含所有規格!
	 */
	public ProductDetailDTO getProductDetail(Integer id) {
		// 獲取商品基本信息
		ProductResDTO product = getById(id);

		// 獲取所有 SKU
		List<SkuResDTO> skus = skuService.findByProductId(id);

		// 從 SKU 中提取規格信息
		List<ProductSpecDTO> specifications = extractSpecifications(skus);

		return new ProductDetailDTO(product, skus, specifications);
	}

	/**
	 * 根據商品ID查詢單一商品--僅包含活跃規格（不包含被软删除的SKU）
	 */
	public ProductDetailDTO getProductActiveDetail(Integer id) {
		// 獲取商品基本信息
		ProductResDTO product = getById(id);

		// 獲取所有 SKU
		List<SkuResDTO> allSkus = skuService.findByProductId(id);

		// 過濾掉已軟刪除的 SKU
		List<SkuResDTO> activeSkus = allSkus.stream().filter(sku -> !Boolean.TRUE.equals(sku.getIsDeleted()))
				.collect(Collectors.toList());

		// 從活跃 SKU 中提取規格信息
		List<ProductSpecDTO> specifications = extractSpecifications(activeSkus);
		
		// 查詢評價總數、星星總數
		List<Object[]> result = reviewRepository.getRatingInfo(id);
		if (result != null && !result.isEmpty()) {
		    Object[] row = result.get(0);
		    Double avg = (Double) row[0];
		    Long count = (Long) row[1];

		    product.setRating(avg != null ? avg : 0.0);
		    product.setReviewCount(count != null ? count.intValue() : 0);
		} else {
		    product.setRating(0.0);
		    product.setReviewCount(0);
		}

		return new ProductDetailDTO(product, activeSkus, specifications);
	}

	/**
	 * (內部方法)從SKU列表中提取規格信息並分組
	 */
	private List<ProductSpecDTO> extractSpecifications(List<SkuResDTO> skus) {
		// 創建一個 Map 來存儲每種規格類型的值集合
		Map<String, Set<String>> specValueMap = new HashMap<>();

		// 遍歷所有SKU，提取規格信息
		for (SkuResDTO sku : skus) {
			Map<String, String> specPairs = sku.getSpecPairs();
			if (specPairs != null && !specPairs.isEmpty()) {
				for (Map.Entry<String, String> entry : specPairs.entrySet()) {
					String specName = entry.getKey(); // 例如："顏色"
					String specValue = entry.getValue(); // 例如："紅色"

					// 將規格值添加到對應的集合中
					specValueMap.computeIfAbsent(specName, k -> new HashSet<>()).add(specValue);
				}
			}
		}

		// 將 Map 轉換為 DTO 列表
		List<ProductSpecDTO> result = new ArrayList<>();
		for (Map.Entry<String, Set<String>> entry : specValueMap.entrySet()) {
			String specName = entry.getKey();
			Set<String> values = entry.getValue();

			List<SpecValueDTO> specValues = new ArrayList<>();
			for (String value : values) {
				// 使用簡化的SpecValueDTO，只設置value屬性
				specValues.add(new SpecValueDTO(value));
			}

			result.add(new ProductSpecDTO(specName, specValues));
		}

		return result;
	}

	/**
	 * 通用商品查詢方法，支援多條件組合及分頁
	 * 
	 * @param shopId      店鋪ID (可選)
	 * @param category1Id 一級分類ID (可選)
	 * @param category2Id 二級分類ID (可選)
	 * @param nameKeyword 商品名稱關鍵字 (可選，用於模糊查詢)
	 * @param descKeyword 描述關鍵字 (可選，用於模糊查詢)
	 * @param minPrice    最低價格 (可選，用於價格範圍查詢)
	 * @param maxPrice    最高價格 (可選，用於價格範圍查詢)
	 * @param page        頁碼 (從0開始)
	 * @param size        每頁大小
	 * @return 分頁商品結果
	 */
	public Page<ProductResDTO> findProducts(Integer shopId, Integer category1Id, Integer category2Id,
			String nameKeyword, String descKeyword, BigDecimal minPrice, BigDecimal maxPrice, int page, int size) {

		// 創建分頁請求
		Pageable pageable = PageRequest.of(page, size);

		// 驗證參數有效性
		validateParameters(shopId, category1Id, category2Id);

		// 使用規範模式構建查詢條件
		Specification<Product> spec = Specification.where(null);

		// 添加未刪除條件
		spec = spec.and((root, query, cb) -> cb.equal(root.get("isDeleted"), false));

		// 添加店鋪條件
		if (shopId != null) {
			spec = spec.and((root, query, cb) -> cb.equal(root.get("shop").get("shopId"), shopId));
		}

		// 添加一級分類條件
		if (category1Id != null) {
			spec = spec.and((root, query, cb) -> cb.equal(root.get("category1").get("id"), category1Id));
		}

		// 添加二級分類條件
		if (category2Id != null) {
			spec = spec.and((root, query, cb) -> cb.equal(root.get("category2").get("id"), category2Id));
		}

		// 添加商品名稱模糊查詢
		if (nameKeyword != null && !nameKeyword.trim().isEmpty()) {
			spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("productName")),
					"%" + nameKeyword.toLowerCase() + "%"));
		}

		// 添加商品描述模糊查詢
		if (descKeyword != null && !descKeyword.trim().isEmpty()) {
			spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("description")),
					"%" + descKeyword.toLowerCase() + "%"));
		}

		// 處理價格範圍查詢 (這部分較複雜，需要關聯SKU表)
		if (minPrice != null || maxPrice != null) {
			// 查詢符合價格條件的商品ID
			List<Integer> productIds = getProductIdsByPriceRange(minPrice, maxPrice);

			if (productIds.isEmpty()) {
				// 如果沒有符合價格條件的商品，直接返回空結果
				return Page.empty(pageable);
			}

			// 添加商品ID條件
			spec = spec.and((root, query, cb) -> root.get("productId").in(productIds));
		}

		// 執行查詢
		Page<Product> productPage = repo.findAll(spec, pageable);

		// 將實體轉換為DTO並返回
		return resMapper.toDtoList(productPage);
	}

	/**
	 * 獲取公開商品列表，支持分頁和分類過濾
	 * 
	 * @param shopId      店鋪ID (可選)，若提供則只返回該店鋪的商品
	 * @param category1Id 一級分類ID (可選)，若提供則只返回該分類下的商品
	 * @param category2Id 二級分類ID (可選)，若提供則只返回該分類下的商品
	 * @param nameKeyword 商品名稱關鍵字 (可選)，用於模糊搜尋商品名稱
	 * @param page        頁碼 (從0開始)
	 * @param size        每頁大小
	 * @return 分頁的商品公開DTO
	 */
	public Page<ProductPublicDTO> findPublicProducts(Integer shopId, Integer category1Id, Integer category2Id,
			String nameKeyword, int page, int size) {

		// 創建分頁請求
		Pageable pageable = PageRequest.of(page, size);

		// 使用規範模式構建查詢條件
		Specification<Product> spec = Specification.where(null);

		// 添加未刪除條件，且商品必須是激活狀態
		spec = spec.and((root, query, cb) -> cb.equal(root.get("isDeleted"), false));
		spec = spec.and((root, query, cb) -> cb.equal(root.get("active"), true));
		spec = spec.and((root, query, cb) -> cb.equal(root.get("reviewStatus"), true)); // 確保已通過審核

		// 添加店鋪條件
		if (shopId != null) {
			spec = spec.and((root, query, cb) -> cb.equal(root.get("shop").get("shopId"), shopId));
		}

		// 添加一級分類條件
		if (category1Id != null) {
			spec = spec.and((root, query, cb) -> cb.equal(root.get("category1").get("id"), category1Id));
		}

		// 添加二級分類條件
		if (category2Id != null) {
			spec = spec.and((root, query, cb) -> cb.equal(root.get("category2").get("id"), category2Id));
		}

		// 添加商品名稱模糊查詢
		if (nameKeyword != null && !nameKeyword.trim().isEmpty()) {
			spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("productName")),
					"%" + nameKeyword.toLowerCase() + "%"));
		}

		// 執行查詢
		Page<Product> productPage = repo.findAll(spec, pageable);

		// 將實體轉換為 ProductPublicDTO
		return productPage.map(product -> new ProductPublicDTO(product));
	}

	/** 獲取商品所有圖片 */
	public List<ProductImage> getProductImages(Integer productId) {
		if (!repo.existsByProductIdAndIsDeletedFalse(productId)) {
			throw new IllegalArgumentException("該商品 ID 不存在或已被刪除");
		}
		return productImageRepo.findByProduct_ProductId(productId);
	}

	/** 獲取商品主圖 */
	public ProductImage getProductPrimaryImage(Integer productId) {
		if (!repo.existsByProductIdAndIsDeletedFalse(productId)) {
			throw new IllegalArgumentException("該商品 ID 不存在或已被刪除");
		}
		return productImageRepo.findByProduct_ProductIdAndIsPrimaryTrue(productId)
				.orElseThrow(() -> new IllegalArgumentException("該商品沒有主圖"));
	}

	// 查詢類方法 結束========================================================
	// 商品管理類方法 開始========================================================

	/** 新增商品 */
	@Transactional
	public ProductResDTO create(Integer userId, String productName, String description, Integer category1Id,
			Integer category2Id, Boolean active, List<MultipartFile> images) {
		try {
			// 確認使用者有店鋪
			Shop shop = productshopRepo.findByUser_UserId(userId)
					.orElseThrow(() -> new IllegalArgumentException("用戶沒有對應的店鋪"));

			// 確認分類存在
			Category1 category1 = category1Repo.findById(category1Id)
					.orElseThrow(() -> new IllegalArgumentException("一級分類 ID 不存在"));

			Category2 category2 = category2Repo.findById(category2Id)
					.orElseThrow(() -> new IllegalArgumentException("二級分類 ID 不存在"));

			// 確認分類組合有效
			if (!category2Repo.existsByIdAndCategory1List_Id(category2Id, category1Id)) {
				throw new IllegalArgumentException("該一級分類 ID 和 二級分類 ID 的組合不存在：" + category1Id + " - " + category2Id);
			}

			// 建立商品實體
			Product entity = new Product();
			entity.setProductName(productName);
			entity.setDescription(description);
			entity.setActive(active != null ? active : false);
			entity.setShop(shop);
			entity.setCategory1(category1);
			entity.setCategory2(category2);
			entity.setIsDeleted(false); // 設置為未刪除狀態

			// 保存商品實體以獲取 ID
			Product savedProduct = repo.saveAndFlush(entity); // 使用 saveAndFlush 而不是 save

			// 再次確認保存的實體有有效的 ID
			if (savedProduct.getProductId() == null) {
				throw new RuntimeException("商品保存後無法獲取有效的 ID");
			}

			// 打印 Product ID 以確認有效性
			System.out.println("已保存商品 ID: " + savedProduct.getProductId());

			// 處理商品圖片 (如果沒有提供圖片，至少添加一個默認圖片)
			if (images == null || images.isEmpty()) {
				// 創建默認圖片
				ProductImage defaultImage = new ProductImage();
				defaultImage.setProduct(savedProduct);
				defaultImage.setImagePath("/uploads/default-product-image.jpg");
				defaultImage.setIsPrimary(true); // 設為主圖

				// 再次確認 product_id 已設置
				if (defaultImage.getProduct() == null || defaultImage.getProduct().getProductId() == null) {
					throw new RuntimeException("默認圖片的商品引用無效");
				}

				productImageRepo.saveAndFlush(defaultImage); // 使用 saveAndFlush
			} else {
				// 處理上傳的多張圖片
				boolean hasPrimary = false;

				for (int i = 0; i < images.size(); i++) {
					MultipartFile image = images.get(i);

					if (image != null && !image.isEmpty()) {
						try {
							// 儲存圖片
							String imagePath = imageFileService.saveImage(image);

							// 創建商品圖片實體並設置關聯
							ProductImage productImage = new ProductImage();
							productImage.setProduct(savedProduct);
							productImage.setImagePath(imagePath);

							// 將第一張圖片設為主圖
							if (!hasPrimary) {
								productImage.setIsPrimary(true);
								hasPrimary = true;
							} else {
								productImage.setIsPrimary(false);
							}

							// 添加到 Product 的集合中
							savedProduct.getProductImages().add(productImage);

						} catch (Exception e) {
							throw new RuntimeException("圖片處理失敗: " + e.getMessage(), e);
						}
					}
				}

				// 不需要單獨保存 ProductImage，只需保存更新後的 Product
				repo.saveAndFlush(savedProduct);
			}

			// 重新查詢商品以獲取完整資訊（包含圖片）
			Product finalProduct = repo.findById(savedProduct.getProductId())
					.orElseThrow(() -> new RuntimeException("無法找到剛保存的商品"));

			return resMapper.toDto(finalProduct);
		} catch (Exception e) {
			// 記錄異常詳細信息
			e.printStackTrace();
			// 重新拋出異常，但添加更多信息
			throw new RuntimeException("創建商品失敗: " + e.getMessage(), e);
		}
	}

	/**
	 * 更新商品（不允許變更關聯的 店鋪、一級分類、二級分類） 1. 處理要刪除的圖片 2. 添加新圖片 3. 設置主圖 4. 確保商品至少有一張主圖
	 */
	@Transactional
	public ProductResDTO update(Integer id, String productName, String description, Boolean active,
			List<MultipartFile> newImages, List<Integer> deleteImageIds, Integer primaryImageId) {
		// 獲取現有商品，確保未刪除
		Product entity = repo.findByProductIdAndIsDeletedFalse(id)
				.orElseThrow(() -> new IllegalArgumentException("該商品 ID 不存在或已被刪除"));

		// 更新商品基本信息
		if (productName != null) {
			entity.setProductName(productName);
		}
		if (description != null) {
			entity.setDescription(description);
		}
		if (active != null) {
			entity.setActive(active);
		}

		// 1. 處理要刪除的圖片
		if (deleteImageIds != null && !deleteImageIds.isEmpty()) {
			// 檢查是否要刪除的圖片中包含主圖
			boolean deletingPrimary = false;

			List<ProductImage> imagesToDelete = productImageRepo.findAllById(deleteImageIds);
			for (ProductImage image : imagesToDelete) {
				// 確認圖片屬於該產品
				if (!image.getProduct().getProductId().equals(id)) {
					throw new IllegalArgumentException("圖片ID " + image.getImageId() + " 不屬於該商品");
				}

				if (image.getIsPrimary()) {
					deletingPrimary = true;
				}

				// 刪除實際圖片文件
				imageFileService.deleteImageFile(image.getImagePath());
			}

			// 刪除資料庫中的圖片記錄
			productImageRepo.deleteAllById(deleteImageIds);

			// 如果刪除了主圖，需要指定一個新的主圖
			if (deletingPrimary && primaryImageId == null) {
				// 查找第一個非刪除的圖片作為新主圖
				List<ProductImage> remainingImages = productImageRepo.findByProduct_ProductIdAndImageIdNotIn(id,
						deleteImageIds);
				if (!remainingImages.isEmpty()) {
					ProductImage newPrimary = remainingImages.get(0);
					newPrimary.setIsPrimary(true);
					productImageRepo.save(newPrimary);
				} else if (newImages == null || newImages.isEmpty()) {
					// 如果沒有剩餘圖片且沒有新上傳圖片，添加默認圖片作為主圖
					ProductImage defaultImage = new ProductImage();
					defaultImage.setProduct(entity);
					defaultImage.setImagePath("/uploads/default-product-image.jpg");
					defaultImage.setIsPrimary(true);
					productImageRepo.save(defaultImage);
				}
			}
		}

		// 2. 添加新圖片
		if (newImages != null && !newImages.isEmpty()) {
			for (MultipartFile image : newImages) {
				if (image != null && !image.isEmpty()) {
					try {
						String imagePath = imageFileService.saveImage(image);

						ProductImage productImage = new ProductImage();
						productImage.setProduct(entity);
						productImage.setImagePath(imagePath);
						productImage.setIsPrimary(false); // 默認為非主圖

						productImageRepo.save(productImage);
					} catch (Exception e) {
						throw new RuntimeException("圖片處理失敗", e);
					}
				}
			}
		}

		// 3. 設置主圖
		if (primaryImageId != null) {
			// 重置所有圖片為非主圖
			productImageRepo.findByProduct_ProductId(id).forEach(img -> {
				img.setIsPrimary(false);
				productImageRepo.save(img);
			});

			// 設置新的主圖
			ProductImage primaryImage = productImageRepo.findById(primaryImageId)
					.orElseThrow(() -> new IllegalArgumentException("指定的主圖ID不存在"));

			// 確認圖片屬於該產品
			if (!primaryImage.getProduct().getProductId().equals(id)) {
				throw new IllegalArgumentException("圖片ID " + primaryImageId + " 不屬於該商品");
			}

			primaryImage.setIsPrimary(true);
			productImageRepo.save(primaryImage);
		}

		// 4. 確保商品至少有一張主圖
		List<ProductImage> allImages = productImageRepo.findByProduct_ProductId(id);

		if (allImages.isEmpty()) {
			// 如果沒有任何圖片，添加默認圖片
			ProductImage defaultImage = new ProductImage();
			defaultImage.setProduct(entity);
			defaultImage.setImagePath("/uploads/default-product-image.jpg");
			defaultImage.setIsPrimary(true);
			productImageRepo.save(defaultImage);
		} else {
			// 檢查是否有主圖
			boolean hasPrimary = allImages.stream().anyMatch(ProductImage::getIsPrimary);

			if (!hasPrimary) {
				// 將第一張圖片設為主圖
				ProductImage firstImage = allImages.get(0);
				firstImage.setIsPrimary(true);
				productImageRepo.save(firstImage);
			}
		}

		// 保存更新後的商品
		Product updatedProduct = repo.save(entity);
		return resMapper.toDto(updatedProduct);
	}

	/**
	 * 軟刪除商品 需先刪除該商品的所有 SKU
	 */
	@Transactional
	public void delete(Integer id) {
		Product entity = repo.findByProductIdAndIsDeletedFalse(id)
				.orElseThrow(() -> new IllegalArgumentException("該商品 ID 不存在或已被刪除"));

		// 自動刪除所有相關的 SKU，不再拋出異常
		if (skuRepo.existsByProduct_ProductIdAndIsDeletedFalse(id)) {
			// 獲取並軟刪除所有關聯的 SKU
			List<Sku> skus = skuRepo.findByProduct_ProductIdAndIsDeletedFalse(id);
			for (Sku sku : skus) {
				sku.setIsDeleted(true);
				skuRepo.save(sku);
			}
		}

		// 軟刪除商品
		entity.setActive(false);
		entity.setIsDeleted(true);
		repo.save(entity);
	}

	// 商品管理類方法 結束========================================================
	// 圖片管理類方法 開始========================================================

	/** 更新商品主圖 */
	@Transactional
	public void updatePrimaryImage(Integer productId, Integer imageId) {
		// 確認商品存在且未被刪除
		if (!repo.existsByProductIdAndIsDeletedFalse(productId)) {
			throw new IllegalArgumentException("該商品 ID 不存在或已被刪除");
		}

		// 確認圖片存在且屬於該商品
		ProductImage newPrimary = productImageRepo.findById(imageId)
				.orElseThrow(() -> new IllegalArgumentException("該圖片 ID 不存在"));

		if (!newPrimary.getProduct().getProductId().equals(productId)) {
			throw new IllegalArgumentException("該圖片不屬於指定的商品");
		}

		// 使用單個SQL查詢重置所有主圖標記
		productImageRepo.resetAllPrimaryImages(productId);

		// 設置新的主圖
		newPrimary.setIsPrimary(true);
		productImageRepo.save(newPrimary);
	}

	/** 新增商品圖片 */
	@Transactional
	public ProductImage addProductImage(Integer productId, MultipartFile image, Boolean isPrimary) {
		// 確認商品存在且未被刪除
		Product product = repo.findByProductIdAndIsDeletedFalse(productId)
				.orElseThrow(() -> new IllegalArgumentException("該商品 ID 不存在或已被刪除"));

		if (image == null || image.isEmpty()) {
			throw new IllegalArgumentException("圖片不能為空");
		}

		String imagePath = null;
		try {
			// 儲存圖片
			imagePath = imageFileService.saveImage(image);

			// 使用單個SQL查詢重置所有主圖標記（如果要設為主圖）
			if (isPrimary != null && isPrimary) {
				// 建議新增一個Repository方法來執行此操作
				productImageRepo.resetAllPrimaryImages(productId);
			}

			// 創建新的圖片記錄
			ProductImage productImage = new ProductImage();
			productImage.setProduct(product);
			productImage.setImagePath(imagePath);
			productImage.setIsPrimary(isPrimary != null && isPrimary);

			return productImageRepo.save(productImage);
		} catch (Exception e) {
			// 如果發生錯誤，刪除已上傳的圖片
			if (imagePath != null) {
				try {
					imageFileService.deleteImageFile(imagePath);
				} catch (Exception ignored) {
					// 忽略清理過程中的錯誤
				}
			}

			throw new RuntimeException("圖片處理失敗", e);
		}
	}

	/** 刪除商品圖片 */
	@Transactional
	public void deleteProductImage(Integer productId, Integer imageId) {
		// 確認商品存在且未被刪除
		if (!repo.existsByProductIdAndIsDeletedFalse(productId)) {
			throw new IllegalArgumentException("該商品 ID 不存在或已被刪除");
		}

		// 獲取要刪除的圖片
		ProductImage image = productImageRepo.findById(imageId)
				.orElseThrow(() -> new IllegalArgumentException("該圖片 ID 不存在"));

		// 確認圖片屬於該商品
		if (!image.getProduct().getProductId().equals(productId)) {
			throw new IllegalArgumentException("該圖片不屬於指定的商品");
		}

		// 檢查是否為主圖
		boolean isPrimary = image.getIsPrimary();

		// 刪除實際圖片文件
		imageFileService.deleteImageFile(image.getImagePath());

		// 刪除圖片記錄
		productImageRepo.delete(image);

		// 如果刪除的是主圖，需要設置新的主圖
		if (isPrimary) {
			List<ProductImage> remainingImages = productImageRepo.findByProduct_ProductId(productId);
			if (!remainingImages.isEmpty()) {
				ProductImage newPrimary = remainingImages.get(0);
				newPrimary.setIsPrimary(true);
				productImageRepo.save(newPrimary);
			}
		}
	}

	// 圖片管理類方法 結束========================================================
	// 輔助類私有方法 開始========================================================

	/**
	 * 根據價格範圍查詢符合條件的商品ID
	 * 
	 * @param minPrice 最低價格 (可為空)
	 * @param maxPrice 最高價格 (可為空)
	 * @return 符合價格範圍的商品ID列表
	 */
	private List<Integer> getProductIdsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
		if (minPrice == null && maxPrice == null) {
			return Collections.emptyList();
		}

		// 設置默認值，如果參數為null
		BigDecimal min = minPrice != null ? minPrice : BigDecimal.ZERO;
		BigDecimal max = maxPrice != null ? maxPrice : new BigDecimal("999999999");

		return skuRepo.findProductIdsByPriceRange(min, max);
	}

	/**
	 * 驗證查詢參數的有效性
	 * 
	 * @param shopId      店鋪ID
	 * @param category1Id 一級分類ID
	 * @param category2Id 二級分類ID
	 * @throws IllegalArgumentException 當任何一個提供的ID不存在時拋出
	 */
	private void validateParameters(Integer shopId, Integer category1Id, Integer category2Id) {
		if (shopId != null && !productshopRepo.existsByShopId(shopId)) {
			throw new IllegalArgumentException("該店鋪 ID (" + shopId + ") 不存在");
		}

		if (category1Id != null && !category1Repo.existsById(category1Id)) {
			throw new IllegalArgumentException("該一級分類 ID (" + category1Id + ") 不存在");
		}

		if (category2Id != null && !category2Repo.existsById(category2Id)) {
			throw new IllegalArgumentException("該二級分類 ID (" + category2Id + ") 不存在");
		}
	}

	// 輔助類私有方法 結束========================================================

	// 取得指定商店的所有產品
	public List<Product> getProductsByShopId(Integer shopId) {
		return repo.findByShopShopId(shopId);
	}

	// 取得指定商店的所有產品（包括 SKU 和圖片）
	public List<Product> getProductsWithDetailsByShopId(Integer shopId) {
		return repo.findProductsWithDetailsByShopId(shopId);
	}
}
