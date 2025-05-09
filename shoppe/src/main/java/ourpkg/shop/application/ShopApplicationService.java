package ourpkg.shop.application;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import ourpkg.auth.SignInResponse;
import ourpkg.auth.SignUpResponse;
import ourpkg.auth.mail.EmailService;
import ourpkg.product.ProductShopRepository;
import ourpkg.shop.Shop;
import ourpkg.shop.application.ShopApplication.ApplicationStatus;
import ourpkg.user_role_permission.Role;
import ourpkg.user_role_permission.RoleService;
import ourpkg.user_role_permission.user.User;
import ourpkg.user_role_permission.user.UserRepository;

@Service
public class ShopApplicationService {
    @Autowired
    private ShopApplicationRepository applicationRepo;
    
    @Autowired
    private ProductShopRepository productshopRepo;

    @Autowired
    private UserRepository userRepo;
    
    @Autowired
    private RoleService roleService;
    
    @Autowired
    private EmailService emailService;
    
    /**
     * 提交或更新商店申請
     * @param userId 使用者ID
     * @param dto 申請資料
     * @return 回應結果
     */
    public ResponseEntity<SignUpResponse> submitApplication(Integer userId, ShopApplicationDTO dto) {
        // 檢查用戶是否存在
        Optional<User> op = userRepo.findById(userId);
        
        if(!op.isPresent()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new SignUpResponse(false, "查無使用者，請先註冊"));
        }
        User user = op.get();

        // 檢查是否已經是賣家
        boolean isAlreadySeller = user.getRole().stream()
                .anyMatch(role -> role.getRoleName().equals("SELLER"));
        if (isAlreadySeller) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new SignUpResponse(false, "你已經是賣家，無需申請"));
        }

        // 檢查是否已經有審核中的申請
        Optional<ShopApplication> pendingApplication = applicationRepo.findByUserAndStatus(user, ApplicationStatus.PENDING);
        if (pendingApplication.isPresent()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new SignUpResponse(false, "已有待審核的申請"));
        }

        // 查詢是否有被拒絕的申請
        Optional<ShopApplication> rejectedApplication = applicationRepo.findByUserAndStatus(user, ApplicationStatus.REJECTED);
        
        ShopApplication application;
        boolean isResubmit = false;
        
        if (rejectedApplication.isPresent()) {
            // 更新現有的被拒絕申請
            application = rejectedApplication.get();
            isResubmit = true;
        } else {
            // 創建新申請
            application = new ShopApplication();
            application.setUser(user);
        }

        // 設定/更新申請資料
        application.setShopCategory(dto.getShopCategory());
        application.setReturnCity(dto.getReturnCity());
        application.setReturnDistrict(dto.getReturnDistrict());
        application.setReturnZipCode(dto.getReturnZipCode());
        application.setReturnStreetEtc(dto.getReturnStreetEtc());
        application.setReturnRecipientName(dto.getReturnRecipientName());
        application.setReturnRecipientPhone(dto.getReturnRecipientPhone());
        application.setShopName(dto.getShopName());
        application.setDescription(dto.getDescription());
        application.setStatus(ApplicationStatus.PENDING);
        
        // 如果是重新提交，清除先前的審核資訊
        if (isResubmit) {
            application.setReviewer(null);
            application.setReviewedAt(null);
            application.setAdminComment(null);
        }

        applicationRepo.save(application);
        
        String message = isResubmit ? "申請已重新提交，請靜待審核!!" : "申請成功，請靜待審核!!";
        return ResponseEntity.ok(new SignUpResponse(true, message));
    }

    public ResponseEntity<SignUpResponse> approveApplication(Integer applicationId, Integer adminId) {
        Optional<ShopApplication> optional = applicationRepo.findByApplicationId(applicationId);

        if (!optional.isPresent()) {
        	return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new SignUpResponse(false, "無此筆申請紀錄"));
        }

        ShopApplication application = optional.get();

        if (application.getStatus() == ApplicationStatus.APPROVED) {
        	return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new SignUpResponse(false, "此申請已被處理"));
        }

        // 查詢用戶
        Optional<User> op = userRepo.findByUserId(application.getUser().getUserId());
        
        if(!op.isPresent()) {
        	return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new SignUpResponse(false, "申請用戶不存在"));
        }
        User user = op.get();
        		
        // 創建商店
        Shop shop = new Shop();
        shop.setUser(user);
        shop.setShopCategory(application.getShopCategory());
        shop.setReturnCity(application.getReturnCity());
        shop.setReturnDistrict(application.getReturnDistrict());
        shop.setReturnZipCode(application.getReturnZipCode());
        shop.setReturnStreetEtc(application.getReturnStreetEtc());
        shop.setReturnRecipientName(application.getReturnRecipientName());
        shop.setReturnRecipientPhone(application.getReturnRecipientPhone());
        shop.setShopName(application.getShopName());
        shop.setDescription(application.getDescription());
        productshopRepo.save(shop);

        // 更新用戶角色
        Role userRole = roleService.findByRoleName("USER");
        Role sellerRole = roleService.findByRoleName("SELLER");

        if (sellerRole == null) {
//        	return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new SignUpResponse(false, "你已經是賣家，無需申請"));
            throw new RuntimeException("角色 SELLER 不存在，請先建立角色");
        }

        Set<Role> roles = new HashSet<>(user.getRole()); // 保留原本角色
        roles.add(userRole);  // 確保用戶仍保留 "USER" 角色
        roles.add(sellerRole);

        user.setRole(roles);
        userRepo.save(user);

        // 更新申請狀態
        application.setStatus(ApplicationStatus.APPROVED);
        application.setReviewer(userRepo.findById(adminId).orElse(null));
		application.setReviewedAt(new Date());
        applicationRepo.save(application);
        emailService.sendShopApprovalEmail(user, application);

        return ResponseEntity.ok(new SignUpResponse(true, "審核通過完成!!"));
    }
    
    public ApplicationCountsDto getApplicationCounts() {
        long pendingCount = applicationRepo.countByStatus(ApplicationStatus.PENDING);
        long approvedCount = applicationRepo.countByStatus(ApplicationStatus.APPROVED);
        long rejectedCount = applicationRepo.countByStatus(ApplicationStatus.REJECTED);

        return new ApplicationCountsDto(pendingCount, approvedCount, rejectedCount);
    }
    
    /**
     * 拒絕商店申請
     * @param applicationId 申請ID
     * @param adminId 管理員ID
     * @param comment 拒絕原因
     * @return 回應結果
     */
    public ResponseEntity<SignUpResponse> rejectApplication(Integer applicationId, Integer adminId, String comment) {
        // 查找待審核的申請
        Optional<ShopApplication> op = applicationRepo.findById(applicationId);

        if(!op.isPresent()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new SignUpResponse(false, "無此筆申請紀錄"));
        }
        
        ShopApplication application = op.get();

        if (application.getStatus() != ApplicationStatus.PENDING) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new SignUpResponse(false, "此申請已被處理"));
        }

        // 更新申請狀態為 REJECTED
        application.setStatus(ApplicationStatus.REJECTED);
        application.setReviewedAt(new Date());
        application.setAdminComment(comment);
        application.setReviewer(userRepo.findById(adminId).orElse(null));
        applicationRepo.save(application);
        
        // 寄送拒絕通知郵件
        emailService.sendShopRejectionEmail(application.getUser(), application);

        return ResponseEntity.ok(new SignUpResponse(true, "拒絕申請完成!!"));
    }
}
