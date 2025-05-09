package ourpkg.shop;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ourpkg.product.ProductRepository;
import ourpkg.product.ProductShopRepository;
import ourpkg.product.version2.controller_service.ProductService;

@Service
public class ShopService {

	@Autowired
	private ProductService productService;

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private ProductShopRepository productShopRepository;

	@Autowired
	private SellerShopRepository sellerShopRepository;

	public ResponseEntity<ShopRequest> getShopById(Integer shopId) {

		Optional<Shop> optional = productShopRepository.findById(shopId);
		if (!optional.isPresent()) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ShopRequest(false, "無此商店", null));
		}

		Shop shop = optional.get();
		ShopDTO shopDTO = new ShopDTO();
		shopDTO.setShopId(shop.getShopId());
		shopDTO.setUserId(shop.getUser().getUserId());
		shopDTO.setShopName(shop.getShopName());
		shopDTO.setDescription(shop.getDescription());
		shopDTO.setCreatedAt(shop.getCreatedAt());
		// 計算商品數量
		Integer countByShop = productRepository.countByShop(shop);
		shopDTO.setCountProducts(countByShop);
		System.out.println(countByShop);

		return ResponseEntity.status(HttpStatus.OK).body(new ShopRequest(true, "獲取成功", shopDTO));

	}

	public boolean isShopOwner(Integer shopId, Integer userId) {

		Optional<Shop> optional = productShopRepository.findById(shopId);
		if (!optional.isPresent()) {
			return false;
		}
		Shop shop = optional.get();
		Integer ownerId = shop.getUser().getUserId();

		if (!(userId == ownerId)) {
			return false;
		}

		return true;
	}

	@Transactional
	public List<ShopDTO> getAllShops() {
		List<Shop> shops = productShopRepository.findAllWithUser(); // 自定義查詢方法

		return shops.stream().map(shop -> {
			ShopDTO dto = new ShopDTO();
			dto.setShopId(shop.getShopId());
			dto.setUserId(shop.getUser().getUserId());
			dto.setShopName(shop.getShopName());
			dto.setDescription(shop.getDescription());
			dto.setCreatedAt(shop.getCreatedAt());

			return dto;
		}).collect(Collectors.toList());
	}

	public Page<ShopInfoDTO> findShopByUserId(Integer userId) {
		Optional<Shop> shopOpt = sellerShopRepository.findByUserUserId(userId);

		if (shopOpt.isPresent()) {
			Shop shop = shopOpt.get();
			System.out.println("找到商店：" + shop.getShopName());
		} else {
			System.out.println("找不到該用戶的商店");
		}
		return null;
	}

	// --- 新增：分頁和搜尋商店列表 ---
	/**
	 * 根據搜尋條件查找商店，並進行分頁。
	 * 
	 * @param searchText 搜尋關鍵字 (商店名稱或賣家名稱)。
	 * @param page       頁碼 (從 0 開始)。
	 * @param size       每頁大小。
	 * @return 包含分頁資訊和 ShopInfoDTO 列表的 Page 物件。
	 */
	@Transactional
	public Page<ShopInfoDTO> findShopsWithPagination(String searchText, int page, int size) {
		// 建立分頁請求物件，可以加入預設排序，例如按商店 ID 排序
		Pageable pageable = PageRequest.of(page, size, Sort.by("shopId").ascending());

		// 呼叫 Repository 的新方法 (推薦直接查詢 DTO)
		return sellerShopRepository.findDTOBySearchText(searchText, pageable);

		// --- 如果 Repository 只回傳 Page<Shop> 的 alternative ---
		// Page<Shop> shopPage = sellerShopRepository.findBySearchText(searchText,
		// pageable);
		// return shopPage.map(this::convertToShopInfoDTO); // 使用 map 轉換 Page 內容
		// --- end alternative ---
	}

	// --- Helper Methods ---

	// 將 Shop 轉換為 ShopDTO (用於 getShopById 和 getAllShops)
	private ShopDTO convertToShopDTO(Shop shop) {
		if (shop == null)
			return null;
		ShopDTO dto = new ShopDTO();
		dto.setShopId(shop.getShopId());
		dto.setUserId(shop.getUser() != null ? shop.getUser().getUserId() : null);
		dto.setShopName(shop.getShopName());
		dto.setDescription(shop.getDescription());
		dto.setCreatedAt(shop.getCreatedAt());
		// 計算商品數量邏輯可以保留或移到更需要的地方
		try {
			Integer countByShop = productRepository.countByShop(shop);
			dto.setCountProducts(countByShop);
		} catch (Exception e) {

			dto.setCountProducts(0); // 或 null
		}
		return dto;

	}
}
