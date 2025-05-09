package ourpkg.campaign.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ourpkg.campaign.ApiResponse;
import ourpkg.campaign.dto.CampaignCouponDTO;
import ourpkg.campaign.dto.MarketingCampaignDTO;
import ourpkg.campaign.service.MarketingCampaignService;
import ourpkg.campaign.service.MarketingCampaignService.ServiceResult;
import ourpkg.user_role_permission.user.User;
import ourpkg.user_role_permission.user.UserRepository;

@RestController
@RequestMapping("/api/campaigns")
public class MarketingCampaignController {

    @Autowired
    private MarketingCampaignService marketingCampaignService;
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * 創建新的行銷活動
     */
    @PostMapping
    public ResponseEntity<ApiResponse> createCampaign(@RequestBody MarketingCampaignDTO campaignDTO) {
        // 設置創建者為當前登錄用戶
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Integer userId = getUserIdFromAuthentication(authentication);
        campaignDTO.setCreatedById(userId);
        
        ServiceResult<MarketingCampaignDTO> result = marketingCampaignService.createCampaign(campaignDTO);
        
        if (result.isSuccess()) {
            return ResponseEntity.ok(new ApiResponse(true, result.getMessage(), result.getData()));
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(false, result.getMessage()));
        }
    }
    
    /**
     * 更新行銷活動
     */
    @PutMapping("/{campaignId}")
    public ResponseEntity<ApiResponse> updateCampaign(
            @PathVariable Integer campaignId,
            @RequestBody MarketingCampaignDTO campaignDTO) {
        
        ServiceResult<MarketingCampaignDTO> result = marketingCampaignService.updateCampaign(campaignId, campaignDTO);
        
        if (result.isSuccess()) {
            return ResponseEntity.ok(new ApiResponse(true, result.getMessage(), result.getData()));
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(false, result.getMessage()));
        }
    }
    
    /**
     * 刪除行銷活動
     */
    @DeleteMapping("/{campaignId}")
    public ResponseEntity<ApiResponse> deleteCampaign(@PathVariable Integer campaignId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Integer userId = getUserIdFromAuthentication(authentication);

        ServiceResult<Boolean> result = marketingCampaignService.deleteCampaign(campaignId, userId);
        
        if (result.isSuccess()) {
            return ResponseEntity.ok(new ApiResponse(true, result.getMessage()));
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(false, result.getMessage()));
        }
    }
    
    /**
     * 獲取特定行銷活動
     */
    @GetMapping("/{campaignId}")
    public ResponseEntity<ApiResponse> getCampaignById(@PathVariable Integer campaignId) {
        ServiceResult<MarketingCampaignDTO> result = marketingCampaignService.getCampaignById(campaignId);
        
        if (result.isSuccess()) {
            return ResponseEntity.ok(new ApiResponse(true, result.getMessage(), result.getData()));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse(false, result.getMessage()));
        }
    }
    
    /**
     * 獲取特定賣家的所有行銷活動
     */
    @GetMapping("/shop/{shopId}")
    public ResponseEntity<ApiResponse> getCampaignsByShopId(@PathVariable Integer shopId) {
        ServiceResult<List<MarketingCampaignDTO>> result = marketingCampaignService.getCampaignsByShopId(shopId);
        
        if (result.isSuccess()) {
            return ResponseEntity.ok(new ApiResponse(true, result.getMessage(), result.getData()));
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(false, result.getMessage()));
        }
    }
    
    /**
     * 獲取所有活躍的行銷活動
     */
    @GetMapping("/active")
    public ResponseEntity<ApiResponse> getActiveRunningCampaigns() {
        ServiceResult<List<MarketingCampaignDTO>> result = marketingCampaignService.getActiveRunningCampaigns();
        
        if (result.isSuccess()) {
            return ResponseEntity.ok(new ApiResponse(true, result.getMessage(), result.getData()));
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(false, result.getMessage()));
        }
    }
    
    /**
     * 添加優惠券到活動
     */
    @PostMapping("/coupons")
    public ResponseEntity<ApiResponse> addCouponToCampaign(@RequestBody CampaignCouponDTO campaignCouponDTO) {
        ServiceResult<CampaignCouponDTO> result = marketingCampaignService.addCouponToCampaign(campaignCouponDTO);
        
        if (result.isSuccess()) {
            return ResponseEntity.ok(new ApiResponse(true, result.getMessage(), result.getData()));
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(false, result.getMessage()));
        }
    }
    
    /**
     * 獲取特定活動的所有優惠券
     */
    @GetMapping("/{campaignId}/coupons")
    public ResponseEntity<ApiResponse> getCouponsByCampaignId(@PathVariable Integer campaignId) {
        // 獲取當前登錄用戶ID，未登錄則為null
        Integer userId = null;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            userId = getUserIdFromAuthentication(authentication);
        }
        
        ServiceResult<List<CampaignCouponDTO>> result = marketingCampaignService.getCouponsByCampaignId(campaignId, userId);
        
        if (result.isSuccess()) {
            return ResponseEntity.ok(new ApiResponse(true, result.getMessage(), result.getData()));
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(false, result.getMessage()));
        }
    }
    
    /**
     * 用戶領取優惠券
     */
    @PostMapping("/redeem")
    public ResponseEntity<ApiResponse> redeemCoupon(@RequestBody Map<String, Integer> request) {
        // 獲取當前登錄用戶
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Integer userId = getUserIdFromAuthentication(authentication);
        
        Integer campaignId = request.get("campaignId");
        Integer couponId = request.get("couponId");
        
        if (campaignId == null || couponId == null) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "請提供活動ID和優惠券ID"));
        }
        
        ServiceResult<Boolean> result = marketingCampaignService.redeemCoupon(userId, campaignId, couponId);
        
        if (result.isSuccess()) {
            return ResponseEntity.ok(new ApiResponse(true, result.getMessage(), result.getData()));
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(false, result.getMessage()));
        }
    }
    
    /**
     * 從活動中移除優惠券
     */
    @DeleteMapping("/{campaignId}/coupons/{couponId}")
    public ResponseEntity<ApiResponse> removeCouponFromCampaign(
            @PathVariable Integer campaignId,
            @PathVariable Integer couponId) {
        
        ServiceResult<Boolean> result = marketingCampaignService.removeCouponFromCampaign(campaignId, couponId);
        
        if (result.isSuccess()) {
            return ResponseEntity.ok(new ApiResponse(true, result.getMessage(), result.getData()));
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(false, result.getMessage()));
        }
    }
    
    /**
     * 從認證對象中取得用戶ID
     * 此方法需要根據您的安全配置來實現
     */
    private Integer getUserIdFromAuthentication(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            System.out.println("認證為空或匿名用戶");
            return null;
        }
        
        // 輸出詳細的認證信息以便調試
        System.out.println("認證類型: " + authentication.getClass().getName());
        System.out.println("Principal 類型: " + authentication.getPrincipal().getClass().getName());
        System.out.println("Principal 內容: " + authentication.getPrincipal());
        
        // 方式1: User 類型
        if (authentication.getPrincipal() instanceof User) {
            User user = (User) authentication.getPrincipal();
            System.out.println("從 User 類型獲取到用戶ID: " + user.getUserId());
            return user.getUserId();
        }
        
        // 方式2: 處理 JsonWebTokenAuthentication 中的 Spring UserDetails
        if (authentication.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails) {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String username = userDetails.getUsername();
            System.out.println("從 UserDetails 獲取到用戶名: " + username);
            
            // 通過用戶名查詢用戶
            try {
                System.out.println("嘗試通過用戶名查詢用戶ID");
                // 這裡需要修改為您的實際查詢方法
                return 1; // 臨時硬編碼返回值，僅用於測試
            } catch (Exception e) {
                System.err.println("查詢用戶ID時出錯: " + e.getMessage());
            }
        }
        
        System.err.println("無法獲取用戶ID，返回默認值");
        return 1; // 臨時硬編碼一個默認值，僅用於測試
    }
}