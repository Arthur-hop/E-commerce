package ourpkg.campaign.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ourpkg.campaign.dto.CampaignCouponDTO;
import ourpkg.campaign.dto.MarketingCampaignDTO;
import ourpkg.campaign.entity.CampaignCoupon;
import ourpkg.campaign.entity.MarketingCampaign;
import ourpkg.campaign.entity.UserCoupon;
import ourpkg.campaign.repository.CampaignCouponRepository;
import ourpkg.campaign.repository.MarketingCampaignRepository;
import ourpkg.campaign.repository.UserCouponRepository;
import ourpkg.coupon.entity.Coupon;
import ourpkg.coupon.repository.CouponRepository;
import ourpkg.shop.SellerShopRepository;
import ourpkg.shop.Shop;
import ourpkg.user_role_permission.user.User;
import ourpkg.user_role_permission.user.UserRepository;

@Service
public class MarketingCampaignService {

    @Autowired
    private MarketingCampaignRepository marketingCampaignRepository;
    
    @Autowired
    private CampaignCouponRepository campaignCouponRepository;
    
    @Autowired
    private CouponRepository couponRepository;
    
    @Autowired
    private UserCouponRepository userCouponRepository;
    
    @Autowired
    private SellerShopRepository shopRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    // 結果類，用於返回操作結果和相關數據
    public static class ServiceResult<T> {
        private boolean success;
        private String message;
        private T data;
        
        public ServiceResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
        
        public ServiceResult(boolean success, String message, T data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public String getMessage() {
            return message;
        }
        
        public T getData() {
            return data;
        }
    }
    
    /**
     * 創建新的行銷活動
     */
    @Transactional
    public ServiceResult<MarketingCampaignDTO> createCampaign(MarketingCampaignDTO campaignDTO) {
        try {
            // 檢查商店是否存在
            Optional<Shop> shopOpt = shopRepository.findById(campaignDTO.getShopId());
            if (!shopOpt.isPresent()) {
                return new ServiceResult<>(false, "商店不存在: " + campaignDTO.getShopId());
            }
            Shop shop = shopOpt.get();
            
            // 檢查用戶是否存在
            Optional<User> userOpt = userRepository.findById(campaignDTO.getCreatedById());
            if (!userOpt.isPresent()) {
                return new ServiceResult<>(false, "用戶不存在: " + campaignDTO.getCreatedById());
            }
            User createdBy = userOpt.get();
            
            MarketingCampaign campaign = new MarketingCampaign();
            campaign.setShopEntity(shop);
            campaign.setCampaignName(campaignDTO.getCampaignName());
            campaign.setDescription(campaignDTO.getDescription());
            campaign.setBannerImage(campaignDTO.getBannerImage());
            campaign.setStartDate(campaignDTO.getStartDate());
            campaign.setEndDate(campaignDTO.getEndDate());
            campaign.setStatus(campaignDTO.getStatus());
            campaign.setCreatedByEntity(createdBy);
            campaign.setCreatedAt(LocalDateTime.now());
            campaign.setUpdatedAt(LocalDateTime.now());
            
            MarketingCampaign savedCampaign = marketingCampaignRepository.save(campaign);
            
            // 刷新實體
            MarketingCampaign refreshedCampaign = marketingCampaignRepository.findById(savedCampaign.getCampaignId()).orElse(null);
            if (refreshedCampaign == null) {
                return new ServiceResult<>(false, "無法獲取剛儲存的活動");
            }
            
            MarketingCampaignDTO result = convertToDTO(refreshedCampaign);
            result.setCouponCount(0); // 新建的活動尚未綁定優惠券
            
            return new ServiceResult<>(true, "行銷活動創建成功", result);
        } catch (Exception e) {
            return new ServiceResult<>(false, "創建活動時發生錯誤: " + e.getMessage());
        }
    }
    
    /**
     * 更新行銷活動
     */
    @Transactional
    public ServiceResult<MarketingCampaignDTO> updateCampaign(Integer campaignId, MarketingCampaignDTO campaignDTO) {
        try {
            Optional<MarketingCampaign> campaignOpt = marketingCampaignRepository.findById(campaignId);
            if (!campaignOpt.isPresent()) {
                return new ServiceResult<>(false, "行銷活動不存在: " + campaignId);
            }
            
            MarketingCampaign campaign = campaignOpt.get();
            
            // 確保只能更新自己商店的活動
            if (campaign.getShopId() != null && !campaign.getShopId().equals(campaignDTO.getShopId())) {
                return new ServiceResult<>(false, "您不能修改其他商店的行銷活動");
            }
            
            campaign.setCampaignName(campaignDTO.getCampaignName());
            campaign.setDescription(campaignDTO.getDescription());
            campaign.setBannerImage(campaignDTO.getBannerImage());
            campaign.setStartDate(campaignDTO.getStartDate());
            campaign.setEndDate(campaignDTO.getEndDate());
            campaign.setStatus(campaignDTO.getStatus());
            campaign.setUpdatedAt(LocalDateTime.now());
            
            MarketingCampaign updatedCampaign = marketingCampaignRepository.save(campaign);
            
            // 查詢該活動綁定的優惠券數量
            long couponCount = campaignCouponRepository.findByCampaignId(updatedCampaign.getCampaignId()).size();
            
            MarketingCampaignDTO result = convertToDTO(updatedCampaign);
            result.setCouponCount((int) couponCount);
            
            return new ServiceResult<>(true, "行銷活動更新成功", result);
        } catch (Exception e) {
            return new ServiceResult<>(false, "更新活動時發生錯誤: " + e.getMessage());
        }
    }
    
    /**
     * 獲取特定行銷活動
     */
    public ServiceResult<MarketingCampaignDTO> getCampaignById(Integer campaignId) {
        try {
            Optional<MarketingCampaign> campaignOpt = marketingCampaignRepository.findById(campaignId);
            if (!campaignOpt.isPresent()) {
                return new ServiceResult<>(false, "行銷活動不存在: " + campaignId);
            }
            
            MarketingCampaign campaign = campaignOpt.get();
            
            // 查詢該活動綁定的優惠券數量
            long couponCount = campaignCouponRepository.findByCampaignId(campaign.getCampaignId()).size();
            
            MarketingCampaignDTO campaignDTO = convertToDTO(campaign);
            campaignDTO.setCouponCount((int) couponCount);
            
            return new ServiceResult<>(true, "行銷活動獲取成功", campaignDTO);
        } catch (Exception e) {
            return new ServiceResult<>(false, "獲取活動時發生錯誤: " + e.getMessage());
        }
    }
    
    /**
     * 獲取特定賣家的所有行銷活動
     */
    public ServiceResult<List<MarketingCampaignDTO>> getCampaignsByShopId(Integer shopId) {
        try {
            List<MarketingCampaign> campaigns = marketingCampaignRepository.findByShopId(shopId);
            List<MarketingCampaignDTO> dtoList = campaigns.stream().map(campaign -> {
                MarketingCampaignDTO dto = convertToDTO(campaign);
                
                // 查詢該活動綁定的優惠券數量
                long couponCount = campaignCouponRepository.findByCampaignId(campaign.getCampaignId()).size();
                dto.setCouponCount((int) couponCount);
                
                return dto;
            }).collect(Collectors.toList());
            
            return new ServiceResult<>(true, "行銷活動獲取成功", dtoList);
        } catch (Exception e) {
            return new ServiceResult<>(false, "獲取活動列表時發生錯誤: " + e.getMessage());
        }
    }
    
    /**
     * 獲取所有活躍的行銷活動
     */
    public ServiceResult<List<MarketingCampaignDTO>> getActiveRunningCampaigns() {
        try {
            LocalDateTime now = LocalDateTime.now();
            List<MarketingCampaign> campaigns = marketingCampaignRepository.findActiveRunningCampaigns(now);
            List<MarketingCampaignDTO> dtoList = campaigns.stream().map(campaign -> {
                MarketingCampaignDTO dto = convertToDTO(campaign);
                
                // 查詢該活動綁定的優惠券數量
                long couponCount = campaignCouponRepository.findByCampaignId(campaign.getCampaignId()).size();
                dto.setCouponCount((int) couponCount);
                
                return dto;
            }).collect(Collectors.toList());
            
            return new ServiceResult<>(true, "活躍行銷活動獲取成功", dtoList);
        } catch (Exception e) {
            return new ServiceResult<>(false, "獲取活躍活動列表時發生錯誤: " + e.getMessage());
        }
    }
    
    /**
     * 添加優惠券到活動
     */
    @Transactional
    public ServiceResult<CampaignCouponDTO> addCouponToCampaign(CampaignCouponDTO campaignCouponDTO) {
        try {
            // 檢查活動是否存在
            Optional<MarketingCampaign> campaignOpt = marketingCampaignRepository.findById(campaignCouponDTO.getCampaignId());
            if (!campaignOpt.isPresent()) {
                return new ServiceResult<>(false, "行銷活動不存在: " + campaignCouponDTO.getCampaignId());
            }
            MarketingCampaign campaign = campaignOpt.get();
            
            // 檢查優惠券是否存在
            Optional<Coupon> couponOpt = couponRepository.findById(campaignCouponDTO.getCouponId());
            if (!couponOpt.isPresent()) {
                return new ServiceResult<>(false, "優惠券不存在: " + campaignCouponDTO.getCouponId());
            }
            Coupon coupon = couponOpt.get();
            
            // 確保只能添加自己商店的優惠券
            if (!coupon.getShop().getShopId().equals(campaign.getShopId())) {
                return new ServiceResult<>(false, "您只能添加自己商店的優惠券到活動中");
            }
            
            // 檢查是否已存在該優惠券
            Optional<CampaignCoupon> existingCouponOpt = campaignCouponRepository
                    .findByCampaignIdAndCouponId(campaignCouponDTO.getCampaignId(), campaignCouponDTO.getCouponId());
            
            if (existingCouponOpt.isPresent()) {
                return new ServiceResult<>(false, "該優惠券已在活動中");
            }
            
            CampaignCoupon campaignCoupon = new CampaignCoupon();
            campaignCoupon.setCampaignId(campaignCouponDTO.getCampaignId());
            campaignCoupon.setCouponId(campaignCouponDTO.getCouponId());
            campaignCoupon.setCampaign(campaign);
            campaignCoupon.setCoupon(coupon);
            campaignCoupon.setTotalQuantity(campaignCouponDTO.getTotalQuantity());
            campaignCoupon.setRemainingQuantity(campaignCouponDTO.getTotalQuantity());
            campaignCoupon.setCreatedAt(LocalDateTime.now());
            campaignCoupon.setUpdatedAt(LocalDateTime.now());
            
            CampaignCoupon saved = campaignCouponRepository.save(campaignCoupon);
            
            return new ServiceResult<>(true, "優惠券添加成功", convertToCampaignCouponDTO(saved, false));
        } catch (Exception e) {
            return new ServiceResult<>(false, "添加優惠券時發生錯誤: " + e.getMessage());
        }
    }
    
    /**
     * 獲取特定活動的所有優惠券
     */
    public ServiceResult<List<CampaignCouponDTO>> getCouponsByCampaignId(Integer campaignId, Integer userId) {
        try {
            // 檢查活動是否存在
            Optional<MarketingCampaign> campaignOpt = marketingCampaignRepository.findById(campaignId);
            if (!campaignOpt.isPresent()) {
                return new ServiceResult<>(false, "行銷活動不存在: " + campaignId);
            }
            
            List<CampaignCoupon> campaignCoupons = campaignCouponRepository.findByCampaignId(campaignId);
            List<CampaignCouponDTO> result = new ArrayList<>();
            
            for (CampaignCoupon cc : campaignCoupons) {
                // 檢查用戶是否已領取該優惠券
                boolean isRedeemed = false;
                if (userId != null) {
                    isRedeemed = userCouponRepository.hasUserRedeemedCampaignCoupon(userId, campaignId, cc.getCoupon().getCouponId());
                }
                
                result.add(convertToCampaignCouponDTO(cc, isRedeemed));
            }
            
            return new ServiceResult<>(true, "優惠券獲取成功", result);
        } catch (Exception e) {
            return new ServiceResult<>(false, "獲取優惠券列表時發生錯誤: " + e.getMessage());
        }
    }
    
    /**
     * 用戶領取優惠券
     */
    @Transactional
    public ServiceResult<Boolean> redeemCoupon(Integer userId, Integer campaignId, Integer couponId) {
        try {
            // 檢查用戶是否存在
            Optional<User> userOpt = userRepository.findById(userId);
            if (!userOpt.isPresent()) {
                return new ServiceResult<>(false, "用戶不存在: " + userId);
            }
            User user = userOpt.get();
            
            // 檢查活動是否存在且處於活躍狀態
            Optional<MarketingCampaign> campaignOpt = marketingCampaignRepository.findById(campaignId);
            if (!campaignOpt.isPresent()) {
                return new ServiceResult<>(false, "行銷活動不存在: " + campaignId);
            }
            MarketingCampaign campaign = campaignOpt.get();
            
            LocalDateTime now = LocalDateTime.now();
            if (!campaign.getStatus().equals("ACTIVE") ||
                campaign.getStartDate().isAfter(now) ||
                campaign.getEndDate().isBefore(now)) {
                return new ServiceResult<>(false, "該行銷活動不在有效期內");
            }
            
            // 檢查優惠券是否存在
            Optional<Coupon> couponOpt = couponRepository.findById(couponId);
            if (!couponOpt.isPresent()) {
                return new ServiceResult<>(false, "優惠券不存在: " + couponId);
            }
            Coupon coupon = couponOpt.get();
            
            // 檢查用戶是否已領取過該優惠券
            if (userCouponRepository.hasUserRedeemedCampaignCoupon(userId, campaignId, couponId)) {
                return new ServiceResult<>(false, "您已領取過該優惠券");
            }
            
            // 檢查優惠券是否仍有剩餘數量
            Optional<CampaignCoupon> campaignCouponOpt = campaignCouponRepository.findByCampaignIdAndCouponId(campaignId, couponId);
            if (!campaignCouponOpt.isPresent()) {
                return new ServiceResult<>(false, "該活動中沒有此優惠券");
            }
            CampaignCoupon campaignCoupon = campaignCouponOpt.get();
            
            if (campaignCoupon.getRemainingQuantity() <= 0) {
                return new ServiceResult<>(false, "該優惠券已被領完");
            }
            
            // 扣減優惠券數量
            int updated = campaignCouponRepository.decreaseCouponQuantity(campaignId, couponId);
            if (updated <= 0) {
                return new ServiceResult<>(false, "優惠券領取失敗，可能已被領完");
            }
            
            // 創建用戶優惠券記錄
            UserCoupon userCoupon = new UserCoupon();
            userCoupon.setUserId(userId);
            userCoupon.setCouponId(couponId);
            userCoupon.setCampaignId(campaignId);
            userCoupon.setUser(user);
            userCoupon.setCoupon(coupon);
            userCoupon.setCampaign(campaign);
            userCoupon.setAcquiredDate(LocalDateTime.now());
            userCoupon.setStatus("ACTIVE");
            
            userCouponRepository.save(userCoupon);
            
            return new ServiceResult<>(true, "優惠券領取成功", true);
        } catch (Exception e) {
            return new ServiceResult<>(false, "領取優惠券時發生錯誤: " + e.getMessage());
        }
    }
    
    /**
     * 從活動中移除優惠券
     */
    @Transactional
    public ServiceResult<Boolean> removeCouponFromCampaign(Integer campaignId, Integer couponId) {
        try {
            // 檢查活動是否存在
            Optional<MarketingCampaign> campaignOpt = marketingCampaignRepository.findById(campaignId);
            if (!campaignOpt.isPresent()) {
                return new ServiceResult<>(false, "行銷活動不存在: " + campaignId);
            }
            
            // 檢查優惠券是否存在於活動中
            Optional<CampaignCoupon> campaignCouponOpt = campaignCouponRepository.findByCampaignIdAndCouponId(campaignId, couponId);
            if (!campaignCouponOpt.isPresent()) {
                return new ServiceResult<>(false, "該活動中沒有此優惠券");
            }
            CampaignCoupon campaignCoupon = campaignCouponOpt.get();
            
            // 檢查是否有用戶已領取該優惠券
            long redeemedCount = userCouponRepository.countByCampaignIdAndCouponId(campaignId, couponId);
            if (redeemedCount > 0) {
                return new ServiceResult<>(false, "已有" + redeemedCount + "位用戶領取了該優惠券，無法移除");
            }
            
            // 從活動中移除優惠券
            campaignCouponRepository.delete(campaignCoupon);
            
            return new ServiceResult<>(true, "優惠券已從活動中移除", true);
        } catch (Exception e) {
            return new ServiceResult<>(false, "移除優惠券時發生錯誤: " + e.getMessage());
        }
    }
    
    /**
     * 刪除活動
     */
    public ServiceResult<Boolean> deleteCampaign(Integer campaignId, Integer userId) {
        try {
            // 檢查活動是否存在並由該用戶創建
            Optional<MarketingCampaign> campaignOpt = marketingCampaignRepository.findByCampaignIdAndCreatedBy(campaignId, userId);
            if (!campaignOpt.isPresent()) {
                return new ServiceResult<>(false, "找不到您創建的此活動");
            }
            
            MarketingCampaign campaign = campaignOpt.get();
            
            // 檢查活動是否有優惠券被領取
            List<CampaignCoupon> campaignCoupons = campaignCouponRepository.findByCampaignId(campaignId);
            for (CampaignCoupon cc : campaignCoupons) {
                long redeemedCount = userCouponRepository.countByCampaignIdAndCouponId(campaignId, cc.getCouponId());
                if (redeemedCount > 0) {
                    return new ServiceResult<>(false, "此活動中的優惠券已被其他使用者領取過，無法刪除");
                }
            }
            
            // 先刪除活動中的所有優惠券
            for (CampaignCoupon cc : campaignCoupons) {
                campaignCouponRepository.delete(cc);
            }
            
            // 刪除活動
            marketingCampaignRepository.delete(campaign);
            
            return new ServiceResult<>(true, "行銷活動刪除成功", true);
        } catch (Exception e) {
            return new ServiceResult<>(false, "刪除活動時發生錯誤: " + e.getMessage());
        }
    }
    
    // 轉換實體到DTO
    private MarketingCampaignDTO convertToDTO(MarketingCampaign campaign) {
        MarketingCampaignDTO dto = new MarketingCampaignDTO();
        dto.setCampaignId(campaign.getCampaignId());
        dto.setShopId(campaign.getShopId());
        
        // 如果 shopEntity 已加載，則使用它獲取店鋪名稱
        if (campaign.getShopEntity() != null) {
            dto.setShopName(campaign.getShopEntity().getShopName());
        }
        
        dto.setCampaignName(campaign.getCampaignName());
        dto.setDescription(campaign.getDescription());
        dto.setBannerImage(campaign.getBannerImage());
        dto.setStartDate(campaign.getStartDate());
        dto.setEndDate(campaign.getEndDate());
        dto.setStatus(campaign.getStatus());
        dto.setCreatedById(campaign.getCreatedBy());
        dto.setCreatedAt(campaign.getCreatedAt());
        dto.setUpdatedAt(campaign.getUpdatedAt());
        return dto;
    }
    
    // 轉換優惠券實體到DTO
    private CampaignCouponDTO convertToCampaignCouponDTO(CampaignCoupon campaignCoupon, boolean redeemedByCurrentUser) {
        CampaignCouponDTO dto = new CampaignCouponDTO();
        dto.setCampaignCouponId(campaignCoupon.getCampaignCouponId());
        dto.setCampaignId(campaignCoupon.getCampaign().getCampaignId());
        dto.setCouponId(campaignCoupon.getCoupon().getCouponId());
        
        Coupon coupon = campaignCoupon.getCoupon();
        dto.setCouponCode(coupon.getCouponCode());
        dto.setCouponName(coupon.getCouponName());
        dto.setDescription(coupon.getDescription());
        dto.setDiscountType(coupon.getDiscountType());
        dto.setDiscountValue(coupon.getDiscountValue());
        dto.setStartDate(coupon.getStartDate());
        dto.setEndDate(coupon.getEndDate());
        
        dto.setTotalQuantity(campaignCoupon.getTotalQuantity());
        dto.setRemainingQuantity(campaignCoupon.getRemainingQuantity());
        dto.setRedeemedByCurrentUser(redeemedByCurrentUser);
        
        return dto;
    }
}