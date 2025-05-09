package ourpkg.sku.version2.controller_service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import ourpkg.product.Product;
import ourpkg.product.ProductRepository;
import ourpkg.sku.Sku;
import ourpkg.sku.SkuRepository;
import ourpkg.sku.version2.dto.SkuCreateDTO;
import ourpkg.sku.version2.dto.SkuResDTO;
import ourpkg.sku.version2.dto.SkuUpdateDTO;
import ourpkg.sku.version2.mapper.SkuCreateMapper;
import ourpkg.sku.version2.mapper.SkuResMapper;
import ourpkg.sku.version2.mapper.SkuUpdateMapper;

/**
 * SKU服務類，提供SKU相關操作
 */
@Service
@RequiredArgsConstructor
public class SkuService {

	private final SkuRepository skuRepo;
	private final ProductRepository productRepo;
	private final SkuCreateMapper createMapper;
	private final SkuUpdateMapper updateMapper;
	private final SkuResMapper resMapper;

	// 查詢類方法 開始 ========================================================

	/**
	 * 根據ID查詢單一SKU
	 * 
	 * @param id SKU ID
	 * @return 對應的SKU DTO
	 * @throws IllegalArgumentException 當指定ID的SKU不存在時拋出
	 */
	public SkuResDTO getById(Integer id) {
		return skuRepo.findById(id).filter(sku -> !sku.getIsDeleted()).map(resMapper::toDto)
				.orElseThrow(() -> new IllegalArgumentException("該SKU ID不存在或已被刪除"));
	}

	/**
	 * 根據商品ID查詢所有SKU
	 * 
	 * @param productId 商品ID
	 * @return SKU列表
	 */
	public List<SkuResDTO> findByProductId(Integer productId) {
		return skuRepo.findByProduct_ProductIdAndIsDeletedFalse(productId).stream().map(resMapper::toDto)
				.collect(Collectors.toList());
	}

	/**
	 * 分頁查詢商品的SKU
	 * 
	 * @param productId 商品ID
	 * @param page      頁碼 (從0開始)
	 * @param size      每頁大小
	 * @return SKU分頁結果
	 */
	public Page<SkuResDTO> findByProductIdPaged(Integer productId, int page, int size) {
		Pageable pageable = PageRequest.of(page, size);
		Page<Sku> skuPage = skuRepo.findByProduct_ProductIdAndIsDeletedFalse(productId, pageable);
		return resMapper.toDtoPage(skuPage);
	}

	/**
	 * 根據價格範圍查詢SKU
	 * 
	 * @param minPrice 最低價格
	 * @param maxPrice 最高價格
	 * @return 符合條件的SKU列表
	 */
	public List<SkuResDTO> findByPriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
		return skuRepo.findByPriceBetweenAndIsDeletedFalse(minPrice, maxPrice).stream().map(resMapper::toDto)
				.collect(Collectors.toList());
	}

	/**
	 * 根據庫存閾值查詢SKU
	 * 
	 * @param threshold 庫存閾值
	 * @return 庫存低於或等於閾值的SKU列表
	 */
	public List<SkuResDTO> findByStockLessThanOrEqual(Integer threshold) {
		return skuRepo.findByStockLessThanEqualAndIsDeletedFalse(threshold).stream().map(resMapper::toDto)
				.collect(Collectors.toList());
	}

	// 查詢類方法 結束 ========================================================
	// 管理類方法 開始 ========================================================

	/**
	 * 新增單個SKU
	 * 
	 * @param productId 商品ID
	 * @param dto       SKU創建DTO
	 * @return 創建的SKU DTO
	 * @throws IllegalArgumentException 當商品不存在時拋出
	 */
	@Transactional
	public SkuResDTO create(Integer productId, SkuCreateDTO dto) {
		// 確認商品存在
		Product product = productRepo.findById(productId)
				.orElseThrow(() -> new IllegalArgumentException("商品ID不存在: " + productId));

		// 確認規格組合在該商品中是唯一的
		if (dto.getSpecPairs() != null && !dto.getSpecPairs().isEmpty()) {
			// 將規格對轉換為JSON格式
			Sku tempSku = new Sku();
			tempSku.setSpecPairsFromMap(dto.getSpecPairs());
			String specPairsJson = tempSku.getSpecPairs();

			if (skuRepo.existsByProduct_ProductIdAndSpecPairsAndIsDeletedFalse(productId, specPairsJson)) {
				throw new IllegalArgumentException("該商品下已存在相同規格組合的SKU");
			}
		}

		// 建立SKU實體
		Sku entity = createMapper.toEntity(dto);
		entity.setProduct(product);
		entity.setIsDeleted(false);

		// 保存SKU
		Sku savedSku = skuRepo.save(entity);

		return resMapper.toDto(savedSku);
	}

	/**
	 * 批量新增SKU
	 * 
	 * @param productId 商品ID
	 * @param dtos      SKU創建DTO列表
	 * @return 創建的SKU DTO列表
	 * @throws IllegalArgumentException 當商品不存在時拋出
	 */
	@Transactional
	public List<SkuResDTO> batchCreate(Integer productId, List<SkuCreateDTO> dtos) {
		// 確認商品存在
		Product product = productRepo.findById(productId)
				.orElseThrow(() -> new IllegalArgumentException("商品ID不存在: " + productId));

		// 獲取已存在的規格組合
		List<String> existingSpecPairs = skuRepo.findByProduct_ProductIdAndIsDeletedFalse(productId).stream()
				.map(Sku::getSpecPairs).collect(Collectors.toList());

		// 檢查並創建SKU
		return dtos.stream().map(dto -> {
			// 檢查規格組合唯一性
			if (dto.getSpecPairs() != null && !dto.getSpecPairs().isEmpty()) {
				Sku tempSku = new Sku();
				tempSku.setSpecPairsFromMap(dto.getSpecPairs());
				String specPairsJson = tempSku.getSpecPairs();

				if (existingSpecPairs.contains(specPairsJson)) {
					throw new IllegalArgumentException("規格組合重複: " + dto.getSpecPairs());
				}
				existingSpecPairs.add(specPairsJson);
			}

			// 創建SKU
			Sku entity = createMapper.toEntity(dto);
			entity.setProduct(product);
			entity.setIsDeleted(false);

			Sku savedSku = skuRepo.save(entity);
			return resMapper.toDto(savedSku);
		}).collect(Collectors.toList());
	}

	/**
	 * 更新SKU
	 * 
	 * @param id  SKU ID
	 * @param dto SKU更新DTO
	 * @return 更新後的SKU DTO
	 * @throws IllegalArgumentException 當SKU不存在時拋出
	 */
	@Transactional
	public SkuResDTO update(Integer id, SkuUpdateDTO dto) {
		// 確認SKU存在且未被刪除
		Sku entity = skuRepo.findById(id).filter(sku -> !sku.getIsDeleted())
				.orElseThrow(() -> new IllegalArgumentException("該SKU ID不存在或已被刪除"));

		// 檢查規格組合唯一性
		if (dto.getSpecPairs() != null && !dto.getSpecPairs().isEmpty()
				&& !dto.getSpecPairs().equals(entity.getSpecPairsAsMap())) {
			// 將規格對轉換為JSON格式
			Sku tempSku = new Sku();
			tempSku.setSpecPairsFromMap(dto.getSpecPairs());
			String specPairsJson = tempSku.getSpecPairs();

			if (skuRepo.existsByProduct_ProductIdAndSpecPairsAndSkuIdNotAndIsDeletedFalse(
					entity.getProduct().getProductId(), specPairsJson, id)) {
				throw new IllegalArgumentException("該商品下已存在相同規格組合的SKU");
			}
		}

		// 更新SKU
		Sku updatedEntity = updateMapper.toEntity(entity, dto);
		Sku savedSku = skuRepo.save(updatedEntity);

		return resMapper.toDto(savedSku);
	}

	/**
	 * 更新SKU庫存
	 * 
	 * @param id    SKU ID
	 * @param stock 新庫存值
	 * @return 更新後的SKU DTO
	 * @throws IllegalArgumentException 當SKU不存在時拋出
	 */
	@Transactional
	public SkuResDTO updateStock(Integer id, Integer stock) {
		if (stock < 0) {
			throw new IllegalArgumentException("庫存不能為負數");
		}

		// 確認SKU存在且未被刪除
		Sku entity = skuRepo.findById(id).filter(sku -> !sku.getIsDeleted())
				.orElseThrow(() -> new IllegalArgumentException("該SKU ID不存在或已被刪除"));

		entity.setStock(stock);
		Sku savedSku = skuRepo.save(entity);

		return resMapper.toDto(savedSku);
	}

	/**
	 * 更新SKU價格
	 * 
	 * @param id    SKU ID
	 * @param price 新價格值
	 * @return 更新後的SKU DTO
	 * @throws IllegalArgumentException 當SKU不存在或價格無效時拋出
	 */
	@Transactional
	public SkuResDTO updatePrice(Integer id, BigDecimal price) {
		if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
			throw new IllegalArgumentException("價格不能為負數");
		}

		// 確認SKU存在且未被刪除
		Sku entity = skuRepo.findById(id).filter(sku -> !sku.getIsDeleted())
				.orElseThrow(() -> new IllegalArgumentException("該SKU ID不存在或已被刪除"));

		entity.setPrice(price);
		Sku savedSku = skuRepo.save(entity);

		return resMapper.toDto(savedSku);
	}

	/**
	 * 軟刪除SKU
	 * 
	 * @param id SKU ID
	 * @throws IllegalArgumentException 當SKU不存在時拋出
	 */
	@Transactional
	public void delete(Integer id) {
		// 確認SKU存在且未被刪除
		Sku entity = skuRepo.findById(id).filter(sku -> !sku.getIsDeleted())
				.orElseThrow(() -> new IllegalArgumentException("該SKU ID不存在或已被刪除"));

		// 軟刪除
		entity.setIsDeleted(true);
		skuRepo.save(entity);
	}

	/**
	 * 批量軟刪除SKU
	 * 
	 * @param ids SKU ID列表
	 */
	@Transactional
	public void batchDelete(List<Integer> ids) {
		List<Sku> skus = skuRepo.findAllById(ids);

		for (Sku sku : skus) {
			if (!sku.getIsDeleted()) {
				sku.setIsDeleted(true);
				skuRepo.save(sku);
			}
		}
	}

	/**
	 * 刪除商品的所有SKU
	 * 
	 * @param productId 商品ID
	 */
	@Transactional
	public void deleteByProductId(Integer productId) {
		List<Sku> skus = skuRepo.findByProduct_ProductIdAndIsDeletedFalse(productId);

		for (Sku sku : skus) {
			sku.setIsDeleted(true);
			skuRepo.save(sku);
		}
	}

	// 管理類方法 結束 ========================================================
	
	public List<SkuResDTO> getByShopId(Integer shopId) {
		if (!productRepo.existsByShop_ShopId(shopId)) {
			throw new IllegalArgumentException("該商店不存在");
		}
		return resMapper.toDtoList(skuRepo.findByProduct_Shop_ShopId(shopId));
	}
}