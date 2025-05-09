package ourpkg.coupon.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ourpkg.coupon.dto.PublicCouponDTO;
import ourpkg.coupon.dto.SellerCouponDTO;
import ourpkg.coupon.entity.Coupon;
import ourpkg.coupon.entity.CouponApplication;
import ourpkg.coupon.service.CouponService;
import ourpkg.coupon.util.DatetimeConverter;

@RestController
@RequestMapping("/coupons")
@CrossOrigin
public class SellerCouponController {
	private static final Logger log = LoggerFactory.getLogger(SellerCouponController.class);

	@Autowired
	private CouponService couponService;

// === Seller Application Endpoints ===

	@PostMapping("/applications/new")
	public ResponseEntity<?> submitNewApplication(@RequestBody String body, @RequestParam("sellerId") Integer sellerId,
			@RequestParam("shopId") Integer shopId) {
		log.info("Seller [{}] submitting NEW application for shop [{}].", sellerId, shopId);
		if (sellerId == null || shopId == null) {
			return ResponseEntity.badRequest().body(Map.of("success", false, "message", "缺少 sellerId 或 shopId"));
		}
		try {
			CouponApplication app = couponService.submitNewCouponApplication(body, sellerId, shopId);
			return ResponseEntity.status(HttpStatus.CREATED)
					.body(Map.of("success", true, "message", "新增申請已提交", "applicationId", app.getApplicationId()));
		} catch (Exception e) {
			log.error("Seller [{}] error submitting NEW application: {}", sellerId, e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("success", false, "message", "提交新增申請失敗: " + e.getMessage()));
		}
	}

	@PostMapping("/applications/update/{targetCouponId}")
	public ResponseEntity<?> submitUpdateApplication(@PathVariable Integer targetCouponId, @RequestBody String body,
			@RequestParam("sellerId") Integer sellerId) {
		log.info("Seller [{}] submitting UPDATE application for coupon [{}].", sellerId, targetCouponId);
		if (sellerId == null || targetCouponId == null) {
			return ResponseEntity.badRequest()
					.body(Map.of("success", false, "message", "缺少 sellerId 或 targetCouponId"));
		}
		try {
			CouponApplication app = couponService.submitUpdateCouponApplication(targetCouponId, body, sellerId);
			return ResponseEntity.status(HttpStatus.CREATED)
					.body(Map.of("success", true, "message", "修改申請已提交", "applicationId", app.getApplicationId()));
		} catch (Exception e) {
			log.error("Seller [{}] error submitting UPDATE application for coupon [{}]: {}", sellerId, targetCouponId,
					e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("success", false, "message", "提交修改申請失敗: " + e.getMessage()));
		}
	}

	@PostMapping("/applications/delete/{targetCouponId}")
	public ResponseEntity<?> submitDeleteApplication(@PathVariable Integer targetCouponId,
			@RequestParam("sellerId") Integer sellerId) {
		log.info("Seller [{}] submitting DELETE application for coupon [{}].", sellerId, targetCouponId);
		if (sellerId == null || targetCouponId == null) {
			return ResponseEntity.badRequest()
					.body(Map.of("success", false, "message", "缺少 sellerId 或 targetCouponId"));
		}
		try {
			CouponApplication app = couponService.submitDeleteCouponApplication(targetCouponId, sellerId);
			return ResponseEntity.status(HttpStatus.CREATED)
					.body(Map.of("success", true, "message", "刪除申請已提交", "applicationId", app.getApplicationId()));
		} catch (Exception e) {
			log.error("Seller [{}] error submitting DELETE application for coupon [{}]: {}", sellerId, targetCouponId,
					e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("success", false, "message", "提交刪除申請失敗: " + e.getMessage()));
		}
	}

// === Seller Check Duplicate ===
	@GetMapping("/check-duplicate")
	public ResponseEntity<?> checkDuplicate(@RequestParam("shopId") Integer shopId,
			@RequestParam(required = false) String code, @RequestParam(required = false) String name,
			@RequestParam("sellerId") Integer sellerId) {
		log.debug("Seller [{}] checking duplicate for shop [{}], code [{}], name [{}]", sellerId, shopId, code, name);
		if (sellerId == null || shopId == null) {
			return ResponseEntity.badRequest().body(Map.of("success", false, "message", "缺少 sellerId 或 shopId"));
		}
		if ((code == null || code.isEmpty()) && (name == null || name.isEmpty())) {
			return ResponseEntity.badRequest().body(Map.of("success", false, "message", "請提供優惠券代碼或名稱以供檢查"));
		}
		try {
			Map<String, Boolean> result = couponService.checkDuplicateCoupon(shopId, code, name, sellerId);
			return ResponseEntity.ok(Map.of("success", true, "exists", result)); // result is Map("codeExists": T/F,
																					// "nameExists": T/F)
		} catch (Exception e) {
			log.error("Seller [{}] error checking duplicate for shop [{}]: {}", sellerId, shopId, e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("success", false, "message", "檢查重複時發生錯誤: " + e.getMessage()));
		}
	}

	// === Seller View Own Active Coupons ===
	 @GetMapping("/my-active")
	 public ResponseEntity<?> getMyActiveCoupons(@RequestParam("sellerId") Integer sellerId,
	                                             @RequestParam("shopId") Integer shopId
	                                             /* TODO: Add Principal/Authentication to verify sellerId belongs to logged-in user */) {
	      log.debug("Seller [{}] fetching active coupons for shop [{}]", sellerId, shopId);
	       // *** Add proper security check here to ensure sellerId matches logged-in user ***
	       // Example:
	       // User loggedInUser = getLoggedInUser(principal); // Assuming a similar helper
	       // if (!loggedInUser.getUserId().equals(sellerId)) {
	       //     return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("success", false, "message", "權限不足"));
	       // }

	       if (sellerId == null || shopId == null) {
	         return ResponseEntity.badRequest().body(Map.of("success", false, "message", "缺少 sellerId 或 shopId"));
	       }
	       try {
	           // *** Call the new service method returning DTOs ***
	           List<SellerCouponDTO> coupons = couponService.findSellerActiveCouponDTOs(sellerId, shopId);
	            return ResponseEntity.ok(Map.of("success", true, "list", coupons)); // Return list of DTOs
	       } catch (Exception e) {
	           log.error("Seller [{}] error fetching active coupons for shop [{}]: {}", sellerId, shopId, e.getMessage(), e);
	            if (e instanceof SecurityException) {
	                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("success", false, "message", e.getMessage()));
	            }
	           return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("success", false, "message", "查詢有效優惠券失敗: " + e.getMessage()));
	       }
	 }

	@GetMapping("/unclaimed")
	public ResponseEntity<Map<String, Object>> getUnclaimedCoupons(@RequestParam(required = false) String search,
			@RequestParam(required = false, defaultValue = "1") int page,
			@RequestParam(required = false, defaultValue = "10") int rows) { // Adjust default rows
		log.debug("Public fetching unclaimed coupons page: {}, rows: {}, search: '{}'", page, rows, search);
		Map<String, Object> responseBody = new HashMap<>();
		try {
			List<Coupon> coupons = couponService.getUnclaimedCoupons(search, page, rows);
			int totalCount = couponService.countUnclaimed(search); // Get total count

			// ... (Formatting logic similar to previous version) ...
			List<Map<String, Object>> couponList = new ArrayList<>();
			for (Coupon coupon : coupons) {
				Map<String, Object> couponJson = new HashMap<>();
				couponJson.put("couponId", coupon.getCouponId());
				couponJson.put("couponName", coupon.getCouponName());
				// couponJson.put("couponCode", coupon.getCouponCode()); // Don't show code
				// publicly
				couponJson.put("description", coupon.getDescription());
				couponJson.put("discountValue", coupon.getDiscountValue());
				couponJson.put("discountType", coupon.getDiscountType());
				couponJson.put("startDate", DatetimeConverter.toString(coupon.getStartDate(), "yyyy-MM-dd"));
				couponJson.put("endDate", DatetimeConverter.toString(coupon.getEndDate(), "yyyy-MM-dd"));
				couponList.add(couponJson);
			}

			responseBody.put("success", true);
			responseBody.put("list", couponList);
			responseBody.put("currentPage", page);
			responseBody.put("rowsPerPage", rows);
			responseBody.put("totalCount", totalCount);
			responseBody.put("totalPages", (int) Math.ceil((double) totalCount / rows));

			return ResponseEntity.ok(responseBody);
		} catch (Exception e) {
			log.error("Error fetching unclaimed coupons: {}", e.getMessage(), e);
			responseBody.put("success", false);
			responseBody.put("message", "獲取未領取優惠券列表失敗");
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseBody);
		}
	}

	@PostMapping("/redeem/{couponId}") // Example for single redeem
	public ResponseEntity<?> redeemSingleCoupon(
			@PathVariable Integer couponId /* , TODO: Add userId if redemption is user-specific */) {
		log.info("Attempting to redeem single coupon [{}]", couponId);
		if (couponId == null) {
			return ResponseEntity.badRequest().body(Map.of("success", false, "message", "缺少 couponId"));
		}
		// TODO: Add logic to associate redemption with a specific user if required.
		try {
			boolean redeemed = couponService.redeemCoupon(couponId);
			if (redeemed) {
				return ResponseEntity.ok(Map.of("success", true, "message", "優惠券兌換成功"));
			} else {
				// Could be not found, already redeemed, or expired/inactive
				return ResponseEntity.badRequest().body(Map.of("success", false, "message", "優惠券無法兌換 (可能不存在、已兌換或已過期)"));
			}
		} catch (Exception e) {
			log.error("Error redeeming coupon [{}]: {}", couponId, e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("success", false, "message", "兌換優惠券時發生錯誤"));
		}
	}

	 /**
     * 公開查詢有效的優惠券列表。
     * @param shopId 可選，篩選特定商店。
     * @param page 頁碼 (從 0 開始)。
     * @param size 每頁數量。
     * @return 分頁的有效優惠券 DTO 列表。
     */
    @GetMapping("/public/active")
    public ResponseEntity<?> getActivePublicCoupons(
            @RequestParam(required = false) Integer shopId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.debug("Request received for active public coupons. ShopId: {}, Page: {}, Size: {}", shopId, page, size);
        try {
            Map<String, Object> responseData = couponService.findActivePublicCoupons(shopId, page, size);
            return ResponseEntity.ok(Map.of("success", true, "data", responseData));
        } catch (Exception e) {
            log.error("Error fetching active public coupons: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "查詢有效優惠券列表失敗"));
        }
    }

    /**
     * 公開根據代碼驗證並取得單一有效優惠券資訊。
     * @param couponCode 優惠券代碼。
     * @param shopId 可選，若提供則驗證此券是否適用於該商店。
     * @return 有效的優惠券 DTO 或錯誤訊息。
     */
    @GetMapping("/public/code/{couponCode}")
    public ResponseEntity<?> getValidCouponByCode(
            @PathVariable String couponCode,
            @RequestParam(required = false) Integer shopId) {
        log.debug("Request received to validate coupon code [{}] for shopId [{}]", couponCode, shopId);
        try {
            PublicCouponDTO couponDTO = couponService.validateAndGetCouponByCode(couponCode, shopId);
            return ResponseEntity.ok(Map.of("success", true, "coupon", couponDTO));
        } catch (IllegalArgumentException e) { // Catch specific exception for bad input
            log.warn("Validation failed for coupon code [{}]: {}", couponCode, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        } catch (RuntimeException e) { // Catch not found or other validation errors from service
            log.warn("Validation failed for coupon code [{}]: {}", couponCode, e.getMessage());
            // 回傳 404 Not Found 或 400 Bad Request 可能比 500 更合適
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) { // Catch unexpected errors
             log.error("Unexpected error validating coupon code [{}]: {}", couponCode, e.getMessage(), e);
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "驗證優惠券代碼時發生內部錯誤"));
        }
    }
    
    //-----------------user-------------by  ccliu----------------------
    
	
	
}
