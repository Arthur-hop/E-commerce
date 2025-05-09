package ourpkg.shop;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ourpkg.user_role_permission.user.User;
import ourpkg.user_role_permission.user.service.UserService;

// import ourpkg.user.user_role_permission.User;/先註解
// import ourpkg.user.user_role_permission.UserService; /先註解

@RestController
@RequestMapping("/api/shop")
public class ShopController {

	@Autowired
	private ShopService shopService;

	@Autowired
	private UserService userService;

	@Autowired
	private SellerShopRepository shopRepository;

	@GetMapping("/{shopId}/is-owner")
	public ResponseEntity<Map<String, Boolean>> checkShopOwner(@PathVariable Integer shopId,
			@AuthenticationPrincipal UserDetails userDetails) {
		Integer userId = userService.getUserIdByUsername(userDetails.getUsername()); // 取得登入者 ID
		boolean isOwner = shopService.isShopOwner(shopId, userId);

		Map<String, Boolean> response = new HashMap<>();
		response.put("isOwner", isOwner);

		return ResponseEntity.ok(response);
	}

	@GetMapping("/{shopId}")
	public ResponseEntity<ShopRequest> getShopById(@PathVariable Integer shopId) {
		ResponseEntity<ShopRequest> response = shopService.getShopById(shopId);

		return response;

	}

	// 取得全部商店資料 API
	@GetMapping("/allShop")
	public ResponseEntity<ShopListResponse> getAllShops() {
		try {
			List<ShopDTO> shops = shopService.getAllShops();
			return ResponseEntity.ok(new ShopListResponse(true, "成功取得所有商店資料", shops));
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(new ShopListResponse(false, "取得商店資料失敗: " + e.getMessage(), null));
		}
	} // 🔹 更新目前登入賣家的店鋪資訊

	@PutMapping("/my")
	public Shop updateMyShop(@RequestBody Shop updatedShopData) {
		User currentUser = userService.getCurrentUser();

		Shop shop = shopRepository.findByUserUserId(currentUser.getUserId())
				.orElseThrow(() -> new RuntimeException("尚未建立商店"));

		// 更新欄位（你可以再依據需要擴充）
		shop.setShopName(updatedShopData.getShopName());
		shop.setDescription(updatedShopData.getDescription());
		shop.setShopCategory(updatedShopData.getShopCategory());

		shop.setReturnCity(updatedShopData.getReturnCity());
		shop.setReturnDistrict(updatedShopData.getReturnDistrict());
		shop.setReturnZipCode(updatedShopData.getReturnZipCode());
		shop.setReturnStreetEtc(updatedShopData.getReturnStreetEtc());
		shop.setReturnRecipientName(updatedShopData.getReturnRecipientName());
		shop.setReturnRecipientPhone(updatedShopData.getReturnRecipientPhone());

		return shopRepository.save(shop);
	}

	// 讓前端可以拿到資料
	@GetMapping("/my")
	public ResponseEntity<Shop> getMyShop() {
		User currentUser = userService.getCurrentUser();
		Shop shop = shopRepository.findByUserUserId(currentUser.getUserId())
				.orElseThrow(() -> new RuntimeException("尚未建立商店"));
		return ResponseEntity.ok(shop);
	}

	// 取得商店資料 API
	@GetMapping("/user/{userId}")
	public ResponseEntity<?> getShopByUserId(@PathVariable Integer userId) {
		if (userId == null || userId <= 0) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body("無效的用戶 ID");
		}

		Optional<Shop> shopOpt = shopRepository.findByUserUserId(userId);

		if (shopOpt.isPresent()) {
			Shop shop = shopOpt.get();
			return ResponseEntity.ok(shop);
		} else {
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body(userId + "尚未建立商店");
		}
	}

	// *** 新增：獲取商店列表 (分頁+搜尋) ***
	@GetMapping("/list") // 或者使用 /shops
	public ResponseEntity<?> getShopList(
			@RequestParam(required = false) String searchText,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size // 與前端預設值或常用值保持一致
	) {

		try {
			Page<ShopInfoDTO> shopPage = shopService.findShopsWithPagination(searchText, page, size);

			// 將 Page 物件轉換為前端期望的 Map 結構
			Map<String, Object> pageData = new HashMap<>();
			pageData.put("list", shopPage.getContent());
			pageData.put("totalItems", shopPage.getTotalElements());
			pageData.put("totalPages", shopPage.getTotalPages());
			pageData.put("currentPage", shopPage.getNumber());
			pageData.put("itemsPerPage", shopPage.getSize());

			// 回傳標準格式
			Map<String, Object> response = new HashMap<>();
			response.put("success", true);
			response.put("message", "成功獲取商店列表");
			response.put("data", pageData);

			return ResponseEntity.ok(response);

		} catch (Exception e) {
			Map<String, Object> errorResponse = new HashMap<>();
			errorResponse.put("success", false);
			errorResponse.put("message", "獲取商店列表失敗: " + e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
		}
	}

	@GetMapping("/user/seller/{userId}")
	public ResponseEntity<?> getShopByUserId1(@PathVariable Integer userId) {
		if (userId == null || userId <= 0) {
			return ResponseEntity.badRequest().body("無效的用戶 ID");
		}

		Optional<Shop> shopOpt = shopRepository.findByUserUserId(userId);

		if (shopOpt.isPresent()) {
			ShopInfoDTO dto = convertToShopInfoDTO(shopOpt.get());
			return ResponseEntity.ok(dto);
		} else {
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body(userId + " 尚未建立商店");
		}
	}

	// ✨ DTO 轉換邏輯直接放這邊
	private ShopInfoDTO convertToShopInfoDTO(Shop shop) {
		return new ShopInfoDTO(
				shop.getShopId(),
				shop.getShopName(),
				shop.getUser().getUserId(),
				shop.getUser().getUserName(),
				shop.getShopCategory());
	}
}



