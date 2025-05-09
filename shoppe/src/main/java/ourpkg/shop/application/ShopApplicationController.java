package ourpkg.shop.application;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ourpkg.auth.SignUpResponse;
import ourpkg.shop.application.ShopApplication.ApplicationStatus;
import ourpkg.user_role_permission.user.User;
import ourpkg.user_role_permission.user.UserRepository;

@RestController
@RequestMapping("/api/shop/application")
public class ShopApplicationController {

	@Autowired
	private ShopApplicationService applicationService;

	@Autowired
	private ShopApplicationRepository applicationRepo;
	
    @Autowired
    private UserRepository userRepo;

	@PreAuthorize("hasAnyRole('USER')")
	@PostMapping("/submit/{userId}")
	public ResponseEntity<?> submitApplication(@PathVariable Integer userId, @RequestBody ShopApplicationDTO dto) {
		if (dto.getShopCategory() == null || dto.getShopCategory().trim().isEmpty()) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new SignUpResponse(false, "請選擇商品分類"));
		}
		if (dto.getReturnCity() == null || dto.getReturnCity().trim().isEmpty()) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new SignUpResponse(false, "請輸入退貨地址 - 城市"));
		}
		if (dto.getReturnDistrict() == null || dto.getReturnDistrict().trim().isEmpty()) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new SignUpResponse(false, "退貨地址 - 區域"));
		}
		if (dto.getReturnZipCode() == null || !dto.getReturnZipCode().matches("^\\d{3,6}$")) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new SignUpResponse(false, "請輸入退貨地址 - 郵遞區號"));
		}
		if (dto.getReturnStreetEtc() == null || dto.getReturnStreetEtc().trim().isEmpty()) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new SignUpResponse(false, "請輸入退貨地址 - 詳細地址"));
		}
		if (dto.getReturnRecipientName() == null || dto.getReturnRecipientName().trim().isEmpty()) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new SignUpResponse(false, "請輸入收件人姓名"));
		}
		if (dto.getReturnRecipientPhone() == null || !dto.getReturnRecipientPhone().matches("^09\\d{8}$")) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new SignUpResponse(false, "請輸入正確收件人電話"));
		}
		if (dto.getShopName() == null || dto.getShopName().trim().isEmpty()) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new SignUpResponse(false, "請輸入商店名稱"));
		}
		if (dto.getDescription() != null && dto.getDescription().length() > 150) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new SignUpResponse(false, "請輸入150字內且不得為空"));
		}
		ResponseEntity<SignUpResponse> response = applicationService.submitApplication(userId, dto);
		return response;
	}

	@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
	@GetMapping("/pending")
	public ResponseEntity<List<ShopApplicationResponseDTO>> getPendingApplications() {
		List<ShopApplicationResponseDTO> dtos = applicationRepo.findByStatus(ApplicationStatus.PENDING).stream()
				.map(ShopApplicationResponseDTO::fromEntity).toList();

		return ResponseEntity.ok(dtos);
	}

	@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
	@GetMapping("/rejected")
	public ResponseEntity<List<ShopApplicationResponseDTO>> getRejectedApplications() {
		List<ShopApplicationResponseDTO> dtos = applicationRepo
				.findByStatusOrderByCreatedAtDesc(ApplicationStatus.REJECTED).stream()
				.map(ShopApplicationResponseDTO::fromEntityWithReviewer).toList();

		return ResponseEntity.ok(dtos);
	}

	@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
	@GetMapping("/approved")
	public ResponseEntity<List<ShopApplicationResponseDTO>> getApprovedApplications() {
		List<ShopApplicationResponseDTO> dtos = applicationRepo
				.findByStatusOrderByCreatedAtDesc(ApplicationStatus.APPROVED).stream()
				.map(ShopApplicationResponseDTO::fromEntityWithReviewer).toList();

		return ResponseEntity.ok(dtos);
	}

	@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
	@PostMapping("/approve/{applicationId}")
	public ResponseEntity<SignUpResponse> approveApplication(@PathVariable Integer applicationId,
			@RequestParam Integer adminId) {

		ResponseEntity<SignUpResponse> response = applicationService.approveApplication(applicationId, adminId);

		return response;
	}

	@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
	@PostMapping("/reject/{applicationId}")
	public ResponseEntity<SignUpResponse> rejectApplication(@PathVariable Integer applicationId,
			@RequestParam Integer adminId, @RequestParam String comment) {
		return applicationService.rejectApplication(applicationId, adminId, comment);
	}

	@GetMapping("/counts")
	public ResponseEntity<ApplicationCountsDto> getApplicationCounts() {
		ApplicationCountsDto countsDto = applicationService.getApplicationCounts();
		return ResponseEntity.ok(countsDto); // 回傳 200 OK 及包含計數的 DTO
	}
	
	@GetMapping("/user/{userId}")
	@PreAuthorize("hasAnyRole('USER')")
	public ResponseEntity<?> getUserApplication(@PathVariable Integer userId) {
	    // 檢查用戶是否存在
	    Optional<User> userOp = userRepo.findById(userId);
	    if (!userOp.isPresent()) {
	        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new SignUpResponse(false, "查無使用者"));
	    }
	    User user = userOp.get();
	    
	    // 檢查用戶是否已經是賣家
	    boolean isAlreadySeller = user.getRole().stream()
	            .anyMatch(role -> role.getRoleName().equals("SELLER"));
	    
	    // 查詢用戶的申請
	    List<ShopApplication> applications = applicationRepo.findByUserOrderByCreatedAtDesc(user);
	    
	    Map<String, Object> response = new HashMap<>();
	    
	    if (isAlreadySeller) {
	        response.put("isSeller", true);
	        response.put("message", "您已經是賣家，無需申請");
	        return ResponseEntity.ok(response);
	    }
	    
	    if (applications.isEmpty()) {
	        // 沒有申請記錄
	        response.put("hasApplication", false);
	        return ResponseEntity.ok(response);
	    } else {
	        // 有申請記錄，返回最新的一筆
	        ShopApplication latestApp = applications.get(0);
	        response.put("hasApplication", true);
	        response.put("status", latestApp.getStatus().toString());
	        response.put("applicationId", latestApp.getApplicationId());
	        response.put("createdAt", latestApp.getCreatedAt());
	        
	        // 如果申請被審核過，返回審核資訊
	        if (latestApp.getStatus() != ApplicationStatus.PENDING) {
	            response.put("reviewedAt", latestApp.getReviewedAt());
	            response.put("adminComment", latestApp.getAdminComment());
	        }
	        
	        // 轉換申請資料為 DTO
	        ShopApplicationDTO dto = new ShopApplicationDTO();
	        dto.setShopName(latestApp.getShopName());
	        dto.setShopCategory(latestApp.getShopCategory());
	        dto.setReturnCity(latestApp.getReturnCity());
	        dto.setReturnDistrict(latestApp.getReturnDistrict());
	        dto.setReturnZipCode(latestApp.getReturnZipCode());
	        dto.setReturnStreetEtc(latestApp.getReturnStreetEtc());
	        dto.setReturnRecipientName(latestApp.getReturnRecipientName());
	        dto.setReturnRecipientPhone(latestApp.getReturnRecipientPhone());
	        dto.setDescription(latestApp.getDescription());
	        
	        response.put("applicationData", dto);
	        
	        return ResponseEntity.ok(response);
	    }
	}
	
	/**
	 * 搜索商店申請並分頁
	 * @param page 頁碼 (0-based)
	 * @param size 每頁大小
	 * @param query 搜尋關鍵字 (店名、描述等)
	 * @param status 申請狀態 (ALL, PENDING, APPROVED, REJECTED)
	 * @param sortBy 排序欄位 (applicationId, shopName, createdAt)
	 * @param sortDir 排序方向 (asc, desc)
	 * @return 分頁搜索結果
	 */
	@GetMapping("/search")
	public ResponseEntity<?> searchApplications(
	        @RequestParam(defaultValue = "0") int page,
	        @RequestParam(defaultValue = "10") int size,
	        @RequestParam(required = false) String query,
	        @RequestParam(defaultValue = "ALL") String status,
	        @RequestParam(defaultValue = "createdAt") String sortBy, // 更改默認排序欄位為 createdAt
	        @RequestParam(defaultValue = "desc") String sortDir) {
	    
	    try {
	        // 欄位名稱映射，將前端參數映射到實體欄位
	        Map<String, String> fieldMapping = new HashMap<>();
	        fieldMapping.put("applicationId", "applicationId");
	        fieldMapping.put("shopName", "shopName");
	        fieldMapping.put("createdAt", "createdAt");
	        fieldMapping.put("applicationTime", "createdAt"); // 為了向後兼容
	        
	        // 獲取實際的排序欄位名稱
	        String actualSortField = fieldMapping.getOrDefault(sortBy, "createdAt");
	        
	        Pageable pageable = PageRequest.of(page, size, 
	                sortDir.equalsIgnoreCase("asc") ? Sort.by(actualSortField).ascending() : Sort.by(actualSortField).descending());
	        
	        Page<ShopApplication> applications;
	        if (query == null || query.trim().isEmpty()) {
	            // 如果沒有查詢條件，僅按狀態過濾
	            if ("ALL".equalsIgnoreCase(status)) {
	                applications = applicationRepo.findAll(pageable);
	            } else {
	                ApplicationStatus appStatus = ApplicationStatus.valueOf(status.toUpperCase());
	                applications = applicationRepo.findByStatus(appStatus, pageable);
	            }
	        } else {
	            // 有搜尋條件
	            String searchTerm = "%" + query.trim() + "%";
	            if ("ALL".equalsIgnoreCase(status)) {
	                applications = applicationRepo.searchByKeyword(searchTerm, pageable);
	            } else {
	                ApplicationStatus appStatus = ApplicationStatus.valueOf(status.toUpperCase());
	                applications = applicationRepo.searchByKeywordAndStatus(searchTerm, appStatus, pageable);
	            }
	        }
	        
	        // 增強回應，添加用戶信息
	        List<Map<String, Object>> enhancedResults = applications.getContent().stream()
	                .map(app -> {
	                    Map<String, Object> result = new HashMap<>();
	                    // 複製申請數據
	                    result.put("applicationId", app.getApplicationId());
	                    result.put("userId", app.getUser().getUserId()); // 假設 User 對象有 getId() 方法
	                    result.put("userName", app.getUser().getUsername()); // 直接從關聯對象獲取
	                    result.put("shopName", app.getShopName());
	                    result.put("shopCategory", app.getShopCategory());
	                    result.put("description", app.getDescription());
	                    result.put("applicationTime", app.getCreatedAt()); // 使用 createdAt
	                    result.put("status", app.getStatus().name());
	                    
	                    if (app.getStatus() != ApplicationStatus.PENDING) {
	                        result.put("reviewedAt", app.getReviewedAt());
	                        if (app.getReviewer() != null) {
	                            result.put("reviewer", app.getReviewer().getUsername());
	                        } else {
	                            result.put("reviewer", "Unknown");
	                        }
	                        
	                        if (app.getStatus() == ApplicationStatus.REJECTED) {
	                            result.put("adminComment", app.getAdminComment());
	                        }
	                    }
	                    
	                    return result;
	                })
	                .collect(Collectors.toList());
	        
	        // 獲取各狀態的數量統計
	        long pendingCount = applicationRepo.countByStatus(ApplicationStatus.PENDING);
	        long approvedCount = applicationRepo.countByStatus(ApplicationStatus.APPROVED);
	        long rejectedCount = applicationRepo.countByStatus(ApplicationStatus.REJECTED);
	        
	        Map<String, Object> response = new HashMap<>();
	        response.put("content", enhancedResults);
	        response.put("currentPage", applications.getNumber());
	        response.put("totalItems", applications.getTotalElements());
	        response.put("totalPages", applications.getTotalPages());
	        
	        // 添加各狀態數量
	        Map<String, Long> counts = new HashMap<>();
	        counts.put("pending", pendingCount);
	        counts.put("approved", approvedCount);
	        counts.put("rejected", rejectedCount);
	        response.put("counts", counts);
	        
	        return ResponseEntity.ok(response);
	    } catch (IllegalArgumentException e) {
	        return ResponseEntity.badRequest().body(Map.of("message", "無效的狀態或排序參數: " + e.getMessage()));
	    } catch (Exception e) {
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	                .body(Map.of("message", "搜尋申請失敗: " + e.getMessage()));
	    }
	}
}
