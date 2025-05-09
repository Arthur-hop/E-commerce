package ourpkg.coupon.controller;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ourpkg.coupon.dto.AdminCouponApplicationDTO;
import ourpkg.coupon.dto.AdminCouponDTO;
import ourpkg.coupon.entity.Coupon;
import ourpkg.coupon.service.CouponService;
import ourpkg.user_role_permission.user.User;
import ourpkg.user_role_permission.user.UserRepository;

@RestController
@RequestMapping("/admin/coupons")
@PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
@CrossOrigin
public class AdminCouponController {
	private static final Logger log = LoggerFactory.getLogger(AdminCouponController.class);


	@Autowired
	private CouponService couponService;
	
	@Autowired
	private UserRepository userRepository;
	
	/**
     * 輔助方法：從 Principal 取得並驗證管理員 User 物件。
     * @param principal Spring Security 提供的 Principal 物件。
     * @return 查找到的 User 物件。
     * @throws SecurityException 如果 principal 為空或在資料庫找不到對應使用者。
     */
    private User getAdminUser(Principal principal) {
        if (principal == null || principal.getName() == null) {
            log.error("無法從 Security Context 獲取 Principal 或使用者名稱");
            throw new SecurityException("無法識別管理員身份，請求未經驗證");
        }
        String username = principal.getName();
        // 使用 UserRepository 根據使用者名稱查找 User 實體
        return userRepository.findByUserName(username)
               .orElseThrow(() -> {
                   log.error("資料庫中找不到管理員使用者名稱: {}", username);
                   // 可以拋出更具體的例外，例如 UserNotFoundException，或保持 SecurityException
                   return new SecurityException("管理員帳號不存在或無法訪問");
               });
    }

	
	 // --- 申請管理 ---


    @GetMapping("/applications/pending")
    public ResponseEntity<?> getPendingApplications(Principal principal) {
        User adminUser = getAdminUser(principal);
        log.info("Admin [{}] (ID:{}) fetching pending applications.", adminUser.getUsername(), adminUser.getUserId());
        try {
            // *** Call the new service method returning DTOs ***
            List<AdminCouponApplicationDTO> applicationDTOs = couponService.getPendingApplicationDTOs();
            return ResponseEntity.ok(Map.of("success", true, "list", applicationDTOs, "count", applicationDTOs.size()));
        } catch (Exception e) {
            log.error("Admin [{}] error fetching pending applications: {}", adminUser.getUsername(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("success", false, "message", "獲取待審核列表失敗"));
        }
    }

    @PostMapping("/applications/{applicationId}/approve-create")
    public ResponseEntity<?> approveCreateApplication(@PathVariable Integer applicationId,
                                                     Principal principal) { // *** 改用 Principal ***
         User adminUser = getAdminUser(principal); // *** 透過輔助方法取得 User 物件 ***
         log.info("Admin [{}] (ID:{}) approving CREATE application [{}].", adminUser.getUsername(), adminUser.getUserId(), applicationId);
         if (applicationId == null) {
             return ResponseEntity.badRequest().body(Map.of("success", false, "message", "缺少 applicationId"));
         }
         try {
             // 將取得的管理員 ID 傳遞給 Service 層
             Coupon createdCoupon = couponService.approveCreateApplication(applicationId, adminUser.getUserId());
             return ResponseEntity.ok(Map.of("success", true, "message", "建立申請已核准", "createdCouponId", createdCoupon.getCouponId()));
         } catch (Exception e) {
             log.error("Admin [{}] error approving CREATE application [{}]: {}", adminUser.getUsername(), applicationId, e.getMessage(), e);
             if (e instanceof IllegalStateException || e instanceof SecurityException || e instanceof IllegalArgumentException) {
                 return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("success", false, "message", e.getMessage()));
             }
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("success", false, "message", "核准建立申請失敗: " + e.getMessage()));
         }
    }

    @PostMapping("/applications/{applicationId}/approve-update")
    public ResponseEntity<?> approveUpdateApplication(@PathVariable Integer applicationId,
                                                     Principal principal) { // *** 改用 Principal ***
        User adminUser = getAdminUser(principal); // *** 透過輔助方法取得 User 物件 ***
        log.info("Admin [{}] (ID:{}) approving UPDATE application [{}].", adminUser.getUsername(), adminUser.getUserId(), applicationId);
         if (applicationId == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "缺少 applicationId"));
        }
        try {
            Coupon modifiedCoupon = couponService.approveUpdateApplication(applicationId, adminUser.getUserId());
            return ResponseEntity.ok(Map.of("success", true, "message", "修改申請已核准", "modifiedCouponId", modifiedCoupon.getCouponId()));
        } catch (Exception e) {
            log.error("Admin [{}] error approving UPDATE application [{}]: {}", adminUser.getUsername(), applicationId, e.getMessage(), e);
             if (e instanceof IllegalStateException || e instanceof SecurityException || e instanceof IllegalArgumentException || e instanceof RuntimeException && e.getMessage().contains("找不到")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("success", false, "message", e.getMessage()));
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("success", false, "message", "核准修改申請失敗: " + e.getMessage()));
        }
   }

    @PostMapping("/applications/{applicationId}/approve-delete")
    public ResponseEntity<?> approveDeleteApplication(@PathVariable Integer applicationId,
                                                      Principal principal) { // *** 改用 Principal ***
         User adminUser = getAdminUser(principal); // *** 透過輔助方法取得 User 物件 ***
         log.info("Admin [{}] (ID:{}) approving DELETE application [{}].", adminUser.getUsername(), adminUser.getUserId(), applicationId);
          if (applicationId == null) {
             return ResponseEntity.badRequest().body(Map.of("success", false, "message", "缺少 applicationId"));
         }
         try {
             boolean deleted = couponService.approveDeleteApplication(applicationId, adminUser.getUserId());
             if (deleted) {
                 return ResponseEntity.ok(Map.of("success", true, "message", "刪除申請已核准"));
             } else {
                 // 若 Service 層在失敗時拋出例外，此處可能不會執行到
                 return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("success", false, "message", "核准刪除申請失敗"));
             }
         } catch (Exception e) {
             log.error("Admin [{}] error approving DELETE application [{}]: {}", adminUser.getUsername(), applicationId, e.getMessage(), e);
             if (e instanceof IllegalStateException || e instanceof SecurityException || e instanceof IllegalArgumentException || e instanceof RuntimeException && e.getMessage().contains("找不到")) {
                 return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("success", false, "message", e.getMessage()));
             }
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("success", false, "message", "核准刪除申請失敗: " + e.getMessage()));
         }
    }
    
    @PostMapping("/applications/{applicationId}/reject")
    public ResponseEntity<?> rejectApplication(@PathVariable Integer applicationId,
                                               Principal principal, // *** 改用 Principal ***
                                               @RequestBody(required = false) Map<String, String> body) {
         User adminUser = getAdminUser(principal); // *** 透過輔助方法取得 User 物件 ***
         log.info("Admin [{}] (ID:{}) rejecting application [{}].", adminUser.getUsername(), adminUser.getUserId(), applicationId);
          if (applicationId == null) {
             return ResponseEntity.badRequest().body(Map.of("success", false, "message", "缺少 applicationId"));
         }
        String reason = (body != null) ? body.get("reason") : null;
         try {
             boolean rejected = couponService.rejectApplication(applicationId, adminUser.getUserId(), reason);
              if (rejected) {
                  return ResponseEntity.ok(Map.of("success", true, "message", "申請已拒絕"));
              } else {
                   return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("success", false, "message", "拒絕申請失敗"));
              }
         } catch (Exception e) {
              log.error("Admin [{}] error rejecting application [{}]: {}", adminUser.getUsername(), applicationId, e.getMessage(), e);
              if (e instanceof IllegalStateException || e instanceof RuntimeException && e.getMessage().contains("找不到")) {
                 return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("success", false, "message", e.getMessage()));
             }
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("success", false, "message", "拒絕申請失敗: " + e.getMessage()));
         }
    }
    
    // --- 直接優惠券 CRUD (僅限管理員) ---

    @PostMapping
    public ResponseEntity<?> createCouponDirectly(@RequestBody Coupon coupon,
                                                  Principal principal) { // *** 改用 Principal ***
         User adminUser = getAdminUser(principal); // *** 透過輔助方法取得 User 物件 ***
         log.info("Admin [{}] (ID:{}) directly creating coupon: {}", adminUser.getUsername(), adminUser.getUserId(), coupon.getCouponName());
         // ... (其餘邏輯不變, 使用 adminUser.getUserId()) ...
          if (coupon == null || coupon.getShop() == null || coupon.getShop().getShopId() == null) {
             return ResponseEntity.badRequest().body(Map.of("success", false, "message", "請求內容不完整或缺少 shopId"));
         }
        try {
            Coupon created = couponService.createCouponInternal(coupon, adminUser.getUserId());
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("success", true, "message", "優惠券建立成功", "couponId", created.getCouponId()));
        } catch (Exception e) {
             log.error("Admin [{}] error creating coupon directly: {}", adminUser.getUsername(), e.getMessage(), e);
              if (e instanceof IllegalArgumentException) {
                 return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
             }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("success", false, "message", "建立優惠券失敗: " + e.getMessage()));
        }
    }

    @PutMapping("/{couponId}")
    public ResponseEntity<?> modifyCouponDirectly(@PathVariable Integer couponId,
                                                  @RequestBody Coupon coupon,
                                                  Principal principal) { // *** 改用 Principal ***
         User adminUser = getAdminUser(principal); // *** 透過輔助方法取得 User 物件 ***
         log.info("Admin [{}] (ID:{}) directly modifying coupon [{}].", adminUser.getUsername(), adminUser.getUserId(), couponId);
         // ... (其餘邏輯不變, 使用 adminUser.getUserId()) ...
         if (coupon == null || couponId == null) {
             return ResponseEntity.badRequest().body(Map.of("success", false, "message", "缺少 couponId 或請求內容"));
         }
         coupon.setCouponId(couponId);
        try {
            Coupon updated = couponService.modifyCouponInternal(coupon, adminUser.getUserId());
            return ResponseEntity.ok(Map.of("success", true, "message", "優惠券修改成功", "couponId", updated.getCouponId()));
        } catch (Exception e) {
             log.error("Admin [{}] error modifying coupon [{}] directly: {}", adminUser.getUsername(), couponId, e.getMessage(), e);
             if (e instanceof IllegalArgumentException || e instanceof RuntimeException && e.getMessage().contains("找不到")) {
                 return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
             }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("success", false, "message", "修改優惠券失敗: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{couponId}")
    public ResponseEntity<?> deleteCouponDirectly(@PathVariable Integer couponId,
                                                  Principal principal) { // *** 改用 Principal ***
        User adminUser = getAdminUser(principal); // *** 透過輔助方法取得 User 物件 ***
        log.info("Admin [{}] (ID:{}) directly deleting coupon [{}].", adminUser.getUsername(), adminUser.getUserId(), couponId);
         // ... (其餘邏輯不變, 使用 adminUser.getUserId()) ...
         if (couponId == null) {
             return ResponseEntity.badRequest().body(Map.of("success", false, "message", "缺少 couponId"));
         }
        try {
            boolean deleted = couponService.deleteCouponInternal(couponId, adminUser.getUserId());
             if (deleted) {
                 return ResponseEntity.ok(Map.of("success", true, "message", "優惠券刪除成功"));
             } else {
                 return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("success", false, "message", "刪除優惠券失敗"));
             }
        } catch (Exception e) {
             log.error("Admin [{}] error deleting coupon [{}] directly: {}", adminUser.getUsername(), couponId, e.getMessage(), e);
             if (e instanceof RuntimeException && e.getMessage().contains("找不到")) {
                 return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("success", false, "message", e.getMessage()));
             }
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("success", false, "message", "刪除優惠券失敗: " + e.getMessage()));
        }
    }

    @GetMapping("/{couponId}")
    public ResponseEntity<?> getCouponById(@PathVariable Integer couponId, Principal principal) {
         User adminUser = getAdminUser(principal);
         log.info("Admin [{}] (ID:{}) fetching coupon by ID [{}] directly.", adminUser.getUsername(), adminUser.getUserId(), couponId);
         if (couponId == null) {
             return ResponseEntity.badRequest().body(Map.of("success", false, "message", "缺少 couponId"));
         }
        try {
            // *** Call the new service method returning DTO ***
            AdminCouponDTO couponDTO = couponService.findCouponByIdInternalDTO(couponId);
            return ResponseEntity.ok(Map.of("success", true, "coupon", couponDTO)); // Return DTO
        } catch (Exception e) {
             log.error("Admin [{}] error fetching coupon [{}] directly: {}", adminUser.getUsername(), couponId, e.getMessage(), e);
             // Service throws RuntimeException if not found
             return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> findCoupons(@RequestParam(required = false) Map<String, Object> criteria,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "10") int size,
                                       Principal principal) {
        User adminUser = getAdminUser(principal);
        log.info("Admin [{}] (ID:{}) finding coupons with criteria: {}", adminUser.getUsername(), adminUser.getUserId(), criteria);
        try {
            // *** Call the new service method returning DTO map ***
            Map<String, Object> responseData = couponService.findCouponsInternalDTO(criteria, page, size);
            // responseData already contains success, list (of DTOs), pagination info
            return ResponseEntity.ok(Map.of("success", true, "data", responseData));
        } catch (Exception e) {
             log.error("Admin [{}] error finding coupons: {}", adminUser.getUsername(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("success", false, "message", "查詢優惠券失敗: " + e.getMessage()));
        }
    }

    /**
     * 取得最近幾個月的優惠券統計數據 (新增數 vs 月底有效數)。
     * @param months 回溯的月數 (預設 6)。
     * @param principal 用於驗證管理員身份。
     * @return 包含標籤和數據列表的 Map。
     */
    @GetMapping("/stats/monthly")
    public ResponseEntity<?> getMonthlyStats(
            @RequestParam(defaultValue = "6") int months,
            Principal principal) { // 依賴 Principal 進行權限驗證 (透過 @PreAuthorize)
         User adminUser = getAdminUser(principal); // 確保管理員存在
         log.info("Admin [{}] requested monthly coupon stats for last {} months.", adminUser.getUsername(), months);
         try {
             if (months <= 0 || months > 24) { // 限制查詢範圍
                 months = 6;
                 log.warn("Invalid month count requested, defaulting to 6.");
             }
             Map<String, Object> stats = couponService.getMonthlyCouponStats(months);
             return ResponseEntity.ok(Map.of("success", true, "stats", stats));
         } catch (Exception e) {
             log.error("Admin [{}] error fetching monthly stats: {}", adminUser.getUsername(), e.getMessage(), e);
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                     .body(Map.of("success", false, "message", "獲取統計數據失敗: " + e.getMessage()));
         }
    }
    
    // --- 新增：獲取待審核數量 API ---
    @GetMapping("/applications/pending/count")
    public ResponseEntity<?> getPendingApplicationCount(Principal principal) {
         // getAdminUser(principal); // 驗證管理員身份，如果需要在日誌中記錄是誰查詢的
         log.info("Admin requested pending application count.");
        try {
            long count = couponService.getPendingApplicationCount();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("count", count);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
             log.error("Error fetching pending application count: {}", e.getMessage(), e);
             Map<String, Object> errorResponse = new HashMap<>();
             errorResponse.put("success", false);
             errorResponse.put("message", "獲取待審核數量失敗: " + e.getMessage());
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}