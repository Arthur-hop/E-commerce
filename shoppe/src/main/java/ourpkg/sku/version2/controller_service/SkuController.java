package ourpkg.sku.version2.controller_service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ourpkg.sku.version2.dto.SkuCreateDTO;
import ourpkg.sku.version2.dto.SkuResDTO;
import ourpkg.sku.version2.dto.SkuUpdateDTO;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SkuController {

	private final SkuService skuService;

	/**
	 * 獲取單一SKU
	 */
	@GetMapping("/skus/{id}")
	public ResponseEntity<SkuResDTO> getSkuById(@PathVariable Integer id) {
		return ResponseEntity.ok(skuService.getById(id));
	}

	/**
	 * 根據商品ID獲取所有SKU
	 */
	@GetMapping("/products/{productId}/skus")
	public ResponseEntity<List<SkuResDTO>> getSkusByProductId(@PathVariable Integer productId) {
		return ResponseEntity.ok(skuService.findByProductId(productId));
	}

	/**
	 * 分頁獲取商品的SKU
	 */
	@GetMapping("/products/{productId}/skus/paged")
	public ResponseEntity<Page<SkuResDTO>> getSkusByProductIdPaged(@PathVariable Integer productId,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
		return ResponseEntity.ok(skuService.findByProductIdPaged(productId, page, size));
	}

	/**
	 * 根據價格範圍查詢SKU
	 */
	@GetMapping("/skus/price-range")
	public ResponseEntity<List<SkuResDTO>> getSkusByPriceRange(@RequestParam BigDecimal minPrice,
			@RequestParam BigDecimal maxPrice) {
		return ResponseEntity.ok(skuService.findByPriceRange(minPrice, maxPrice));
	}

	/**
	 * 查詢庫存低於閾值的SKU
	 */
	@GetMapping("/skus/low-stock")
	public ResponseEntity<List<SkuResDTO>> getSkusByLowStock(@RequestParam Integer threshold) {
		return ResponseEntity.ok(skuService.findByStockLessThanOrEqual(threshold));
	}

	/**
	 * 新增單個SKU
	 */
	@PostMapping("/products/{productId}/skus")
	public ResponseEntity<SkuResDTO> createSku(@PathVariable Integer productId, @RequestBody @Valid SkuCreateDTO dto) {
		SkuResDTO createdSku = skuService.create(productId, dto);
		return ResponseEntity.status(HttpStatus.CREATED).body(createdSku);
	}

	/**
	 * 批量新增SKU
	 */
	@PostMapping("/products/{productId}/skus/batch")
	public ResponseEntity<List<SkuResDTO>> batchCreateSkus(@PathVariable Integer productId,
			@RequestBody @Valid List<SkuCreateDTO> dtos) {
		List<SkuResDTO> createdSkus = skuService.batchCreate(productId, dtos);
		return ResponseEntity.status(HttpStatus.CREATED).body(createdSkus);
	}

	/**
	 * 更新SKU
	 */
	@PutMapping("/skus/{id}")
	public ResponseEntity<SkuResDTO> updateSku(@PathVariable Integer id, @RequestBody @Valid SkuUpdateDTO dto) {
		return ResponseEntity.ok(skuService.update(id, dto));
	}

	/**
	 * 更新SKU庫存
	 */
	@PatchMapping("/skus/{id}/stock")
	public ResponseEntity<SkuResDTO> updateSkuStock(@PathVariable Integer id, @RequestParam Integer stock) {
		return ResponseEntity.ok(skuService.updateStock(id, stock));
	}

	/**
	 * 更新SKU價格
	 */
	@PatchMapping("/skus/{id}/price")
	public ResponseEntity<SkuResDTO> updateSkuPrice(@PathVariable Integer id, @RequestParam BigDecimal price) {
		return ResponseEntity.ok(skuService.updatePrice(id, price));
	}

	/**
	 * 刪除SKU
	 */
	@DeleteMapping("/skus/{id}")
	public ResponseEntity<Void> deleteSku(@PathVariable Integer id) {
		skuService.delete(id);
		return ResponseEntity.noContent().build();
	}

	/**
	 * 批量刪除SKU
	 */
	@DeleteMapping("/skus/batch")
	public ResponseEntity<Void> batchDeleteSkus(@RequestBody List<Integer> ids) {
		skuService.batchDelete(ids);
		return ResponseEntity.noContent().build();
	}

	/**
	 * 刪除商品的所有SKU
	 */
	@DeleteMapping("/products/{productId}/skus")
	public ResponseEntity<Void> deleteAllSkusByProductId(@PathVariable Integer productId) {
		skuService.deleteByProductId(productId);
		return ResponseEntity.noContent().build();
	}
	
	//get by shopId
		@GetMapping("/byshop/{shopId}")
		public ResponseEntity<List<SkuResDTO>> getSKUByShopId(@PathVariable Integer shopId) {
			return ResponseEntity.ok(skuService.getByShopId(shopId));
		} 
}