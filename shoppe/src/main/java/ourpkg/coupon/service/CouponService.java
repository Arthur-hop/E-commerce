package ourpkg.coupon.service;

import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import ourpkg.coupon.dto.AdminCouponApplicationDTO;
import ourpkg.coupon.dto.AdminCouponDTO;
import ourpkg.coupon.dto.PublicCouponDTO;
import ourpkg.coupon.dto.SellerCouponDTO;
import ourpkg.coupon.entity.Coupon;
import ourpkg.coupon.entity.CouponApplication;
import ourpkg.coupon.repository.CouponApplicationRepository;
import ourpkg.coupon.repository.CouponRepository;
import ourpkg.coupon.util.DatetimeConverter;
import ourpkg.shop.SellerShopRepository;
import ourpkg.shop.Shop;
import ourpkg.user_role_permission.user.UserRepository;
import ourpkg.user_role_permission.user.service.UserService;

@Service
public class CouponService {

	private static final Logger log = LoggerFactory.getLogger(CouponService.class);

	@Autowired
	private CouponRepository couponRepository;

	@Autowired
	private UserService userService;

	@Autowired
	private SellerShopRepository shopRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private EntityManager entityManager;

	@Autowired
	private CouponApplicationRepository applicationRepository;

	// === Seller Actions ===

	@Transactional
	public CouponApplication submitNewCouponApplication(String json, Integer sellerId, Integer shopId) {
		log.info("Seller [{}] submitting NEW coupon application for shop [{}].", sellerId, shopId);
		Shop shop = validateSellerShop(sellerId, shopId); // Validate ownership
		try {
			JSONObject obj = new JSONObject(json);
			CouponApplication app = new CouponApplication();
			app.setApplicationType("CREATE");
			app.setRequestedBySellerId(sellerId);
			app.setRequestedShopId(shop.getShopId());
			app.setStatus("PENDING_CREATE");
			app.setApplicationDate(new Date());

			// Populate proposed values
			app.setCouponName(obj.getString("couponName"));
			app.setCouponCode(obj.getString("couponCode")); // Consider adding uniqueness check here or on approval
			app.setDescription(obj.getString("description"));
			app.setDiscountType(obj.getString("discountType"));
			app.setDiscountValue(obj.getBigDecimal("discountValue"));
			app.setStartDate(DatetimeConverter.parse(obj.getString("startDate"), "yyyy-MM-dd"));
			app.setEndDate(DatetimeConverter.parse(obj.getString("endDate"), "yyyy-MM-dd"));
			app.setUsageLimit(obj.getInt("usageLimit"));
			app.setUsagePerUser(obj.getInt("usagePerUser"));

			// Basic validation (can add more)
			if (app.getEndDate().before(app.getStartDate())) {
				throw new IllegalArgumentException("結束日期不能早於開始日期");
			}
			// Check for duplicate active coupon code/name within the shop before submitting
			// application
			if (couponRepository.existsActiveByShopIdAndCouponCode(shopId, app.getCouponCode(), new Date())) {
				throw new IllegalArgumentException("此商店已存在使用中的相同優惠券代碼");
			}
			if (couponRepository.existsActiveByShopIdAndCouponName(shopId, app.getCouponName(), new Date())) {
				throw new IllegalArgumentException("此商店已存在使用中的相同優惠券名稱");
			}

			return applicationRepository.save(app);
		} catch (Exception e) {
			log.error("Error submitting new coupon application for seller [{}]: {}", sellerId, e.getMessage(), e);
			throw new RuntimeException("提交新優惠券申請失敗: " + e.getMessage(), e);
		}
	}

	@Transactional
	public CouponApplication submitUpdateCouponApplication(Integer targetCouponId, String json, Integer sellerId) {
		log.info("Seller [{}] submitting UPDATE application for coupon [{}].", sellerId, targetCouponId);
		Coupon existingCoupon = couponRepository.findById(targetCouponId)
				.orElseThrow(() -> new RuntimeException("找不到要修改的優惠券 ID: " + targetCouponId));
		Shop shop = validateSellerShop(sellerId, existingCoupon.getShop().getShopId()); // Validate seller owns the
																						// coupon's shop

		try {
			JSONObject obj = new JSONObject(json);
			CouponApplication app = new CouponApplication();
			app.setApplicationType("UPDATE");
			app.setRequestedBySellerId(sellerId);
			app.setRequestedShopId(shop.getShopId());
			app.setTargetCouponId(targetCouponId); // Link to the coupon being modified
			app.setStatus("PENDING_UPDATE");
			app.setApplicationDate(new Date());

			// Populate proposed new values (only include fields allowed to be modified)
			// Use optString etc. to allow partial updates
			app.setCouponName(obj.optString("couponName", existingCoupon.getCouponName())); // Default to existing if
																							// not provided
			// Usually coupon code is not changed, if allowed, add logic here
			app.setCouponCode(obj.optString("couponCode", existingCoupon.getCouponCode()));
			app.setDescription(obj.optString("description", existingCoupon.getDescription()));
			app.setDiscountType(obj.optString("discountType", existingCoupon.getDiscountType()));
			app.setDiscountValue(
					obj.has("discountValue") ? obj.getBigDecimal("discountValue") : existingCoupon.getDiscountValue());
			app.setStartDate(obj.has("startDate") ? DatetimeConverter.parse(obj.getString("startDate"), "yyyy-MM-dd")
					: existingCoupon.getStartDate());
			app.setEndDate(obj.has("endDate") ? DatetimeConverter.parse(obj.getString("endDate"), "yyyy-MM-dd")
					: existingCoupon.getEndDate());
			app.setUsageLimit(obj.has("usageLimit") ? obj.getInt("usageLimit") : existingCoupon.getUsageLimit());
			app.setUsagePerUser(
					obj.has("usagePerUser") ? obj.getInt("usagePerUser") : existingCoupon.getUsagePerUser());

			// Validation for proposed changes
			if (app.getEndDate().before(app.getStartDate())) {
				throw new IllegalArgumentException("建議的結束日期不能早於開始日期");
			}
			// Check if proposed name conflicts with *other* active coupons in the shop
			String proposedName = app.getCouponName();
			if (!proposedName.equals(existingCoupon.getCouponName())
					&& couponRepository.existsActiveByShopIdAndCouponName(shop.getShopId(), proposedName, new Date())) {
				throw new IllegalArgumentException("此商店已存在另一張使用中的相同優惠券名稱");
			}
			// Add similar check for coupon code if modification is allowed

			return applicationRepository.save(app);
		} catch (Exception e) {
			log.error("Error submitting update coupon application for seller [{}], coupon [{}]: {}", sellerId,
					targetCouponId, e.getMessage(), e);
			throw new RuntimeException("提交修改優惠券申請失敗: " + e.getMessage(), e);
		}
	}

	@Transactional
	public CouponApplication submitDeleteCouponApplication(Integer targetCouponId, Integer sellerId) {
		log.info("Seller [{}] submitting DELETE application for coupon [{}].", sellerId, targetCouponId);
		Coupon existingCoupon = couponRepository.findById(targetCouponId)
				.orElseThrow(() -> new RuntimeException("找不到要刪除的優惠券 ID: " + targetCouponId));
		Shop shop = validateSellerShop(sellerId, existingCoupon.getShop().getShopId()); // Validate ownership

		try {
			CouponApplication app = new CouponApplication();
			app.setApplicationType("DELETE");
			app.setRequestedBySellerId(sellerId);
			app.setRequestedShopId(shop.getShopId());
			app.setTargetCouponId(targetCouponId); // Link to the coupon to be deleted
			app.setStatus("PENDING_DELETE");
			app.setApplicationDate(new Date());
			// No proposed values needed for delete

			return applicationRepository.save(app);
		} catch (Exception e) {
			log.error("Error submitting delete coupon application for seller [{}], coupon [{}]: {}", sellerId,
					targetCouponId, e.getMessage(), e);
			throw new RuntimeException("提交刪除優惠券申請失敗: " + e.getMessage(), e);
		}
	}

	/**
	 * Seller checks if a proposed coupon code or name already exists for an active
	 * coupon in their shop.
	 * 
	 * @param shopId   The seller's shop ID.
	 * @param code     Proposed coupon code (optional).
	 * @param name     Proposed coupon name (optional).
	 * @param sellerId The seller's user ID for permission check.
	 * @return Map containing boolean flags for codeExists and nameExists.
	 */
	public Map<String, Boolean> checkDuplicateCoupon(Integer shopId, String code, String name, Integer sellerId) {
		log.debug("Seller [{}] checking duplicate coupon for shop [{}], code [{}], name [{}]", sellerId, shopId, code,
				name);
		validateSellerShop(sellerId, shopId); // Basic permission check

		Map<String, Boolean> result = Map.of("codeExists", false, "nameExists", false);
		Date now = new Date();

		boolean codeExists = (code != null && !code.isEmpty())
				&& couponRepository.existsActiveByShopIdAndCouponCode(shopId, code, now);
		boolean nameExists = (name != null && !name.isEmpty())
				&& couponRepository.existsActiveByShopIdAndCouponName(shopId, name, now);

		result = Map.of("codeExists", codeExists, "nameExists", nameExists);
		return result;
	}

	/**
	 * Seller gets a list of their own active/valid coupons (e.g., for
	 * modification/deletion request UI).
	 * 
	 * @param sellerId The seller's user ID.
	 * @param shopId   The seller's shop ID.
	 * @return List of active coupons for the shop.
	 */
	public List<Coupon> findSellerActiveCoupons(Integer sellerId, Integer shopId) {
		log.debug("Seller [{}] fetching active coupons for shop [{}]", sellerId, shopId);
		validateSellerShop(sellerId, shopId); // Permission check
		// Fetch coupons for the shop that are not expired
		return couponRepository.findByShop_ShopIdAndEndDateGreaterThanEqualOrderByEndDateAsc(shopId, new Date());
	}

	// === Admin Actions ===

	public List<CouponApplication> getPendingApplications() {
		log.debug("Admin fetching pending applications.");
		List<String> pendingStatuses = List.of("PENDING_CREATE", "PENDING_UPDATE", "PENDING_DELETE");
		return applicationRepository.findByStatusInOrderByApplicationDateDesc(pendingStatuses);
	}

	@Transactional
	public Coupon approveCreateApplication(Integer applicationId, Integer adminUserId) {
		log.info("Admin [{}] approving CREATE application [{}].", adminUserId, applicationId);
		CouponApplication app = findAndValidateApplication(applicationId, "PENDING_CREATE");
		Shop shop = findShop(app.getRequestedShopId()); // Ensure shop exists

		// Prepare data for internal create method
		Coupon couponToCreate = mapApplicationToCoupon(app);
		couponToCreate.setShop(shop); // Associate shop

		Coupon createdCoupon = createCouponInternal(couponToCreate, adminUserId); // Use internal method

		// Update application status and link result
		app.setStatus("APPROVED");
		app.setResultingCouponId(createdCoupon.getCouponId());
		app.setAdminNotes("Approved CREATE by Admin ID: " + adminUserId);
		applicationRepository.save(app);
		log.info("CREATE application [{}] approved. Resulting coupon ID [{}].", applicationId,
				createdCoupon.getCouponId());
		return createdCoupon;
	}

	@Transactional
	public Coupon approveUpdateApplication(Integer applicationId, Integer adminUserId) {
		log.info("Admin [{}] approving UPDATE application [{}].", adminUserId, applicationId);
		CouponApplication app = findAndValidateApplication(applicationId, "PENDING_UPDATE");
		if (app.getTargetCouponId() == null) {
			throw new IllegalStateException("UPDATE 申請缺少目標優惠券 ID");
		}

		Coupon existingCoupon = couponRepository.findById(app.getTargetCouponId())
				.orElseThrow(() -> new RuntimeException("找不到要修改的目標優惠券 ID: " + app.getTargetCouponId()));

		// Check if shop matches (should match from submission validation, but
		// double-check)
		if (!existingCoupon.getShop().getShopId().equals(app.getRequestedShopId())) {
			throw new IllegalStateException("目標優惠券不屬於申請中的商店");
		}

		// Prepare updated Coupon object from application data
		Coupon updatedCouponData = mapApplicationToCoupon(app); // Map proposed changes
		updatedCouponData.setCouponId(existingCoupon.getCouponId()); // Set the ID to update
		updatedCouponData.setShop(existingCoupon.getShop()); // Keep original shop association
		// Keep original createdAt, update updatedAt
		updatedCouponData.setCreatedAt(existingCoupon.getCreatedAt());

		Coupon modifiedCoupon = modifyCouponInternal(updatedCouponData, adminUserId); // Use internal method

		// Update application status
		app.setStatus("APPROVED");
		// resulting_coupon_id might not be relevant for update, or could store the
		// target_coupon_id again? Let's leave it null.
		app.setAdminNotes("Approved UPDATE by Admin ID: " + adminUserId);
		applicationRepository.save(app);
		log.info("UPDATE application [{}] approved for target coupon [{}].", applicationId, app.getTargetCouponId());
		return modifiedCoupon;
	}

	@Transactional
	public boolean approveDeleteApplication(Integer applicationId, Integer adminUserId) {
		log.info("Admin [{}] approving DELETE application [{}].", adminUserId, applicationId);
		CouponApplication app = findAndValidateApplication(applicationId, "PENDING_DELETE");
		if (app.getTargetCouponId() == null) {
			throw new IllegalStateException("DELETE 申請缺少目標優惠券 ID");
		}

		boolean deleted = deleteCouponInternal(app.getTargetCouponId(), adminUserId); // Use internal method

		if (deleted) {
			// Update application status
			app.setStatus("APPROVED");
			app.setAdminNotes("Approved DELETE by Admin ID: " + adminUserId);
			applicationRepository.save(app);
			log.info("DELETE application [{}] approved for target coupon [{}].", applicationId,
					app.getTargetCouponId());
			return true;
		} else {
			// This shouldn't happen if deleteCouponInternal throws on failure
			log.error("Internal delete failed for coupon [{}] during DELETE application [{}] approval.",
					app.getTargetCouponId(), applicationId);
			return false;
		}
	}

	@Transactional
	public boolean rejectApplication(Integer applicationId, Integer adminUserId, String reason) {
		log.info("Admin [{}] rejecting application [{}] with reason: {}", adminUserId, applicationId, reason);
		CouponApplication app = applicationRepository.findById(applicationId)
				.orElseThrow(() -> new RuntimeException("找不到申請紀錄 ID: " + applicationId));

		if (!app.getStatus().startsWith("PENDING")) {
			throw new IllegalStateException("此申請狀態不是待審核，無法拒絕");
		}

		app.setStatus("REJECTED");
		app.setAdminNotes(reason);
		applicationRepository.save(app);
		log.info("Application [{}] rejected by Admin [{}].", applicationId, adminUserId);
		return true;
	}

	// === Admin Internal Coupon CRUD Methods ===
	// These bypass seller permissions and operate directly

	@Transactional
	public Coupon createCouponInternal(Coupon coupon, Integer adminUserId) {
		log.info("Admin [{}] creating coupon internally: {}", adminUserId, coupon.getCouponName());
		// Ensure shop is set
		if (coupon.getShop() == null || coupon.getShop().getShopId() == null) {
			throw new IllegalArgumentException("內部建立優惠券需要有效的 Shop 關聯");
		}
		// Ensure shop exists (redundant if shop object is passed correctly, but safe)
		findShop(coupon.getShop().getShopId());

		// Set timestamps
		Date now = new Date();
		coupon.setCreatedAt(now);
		coupon.setUpdatedAt(now);
		coupon.setCouponId(null); // Ensure ID is null for creation
		coupon.setRedeemed(false); // Default

		// Add validation if needed (e.g., date checks, uniqueness checks)
		if (coupon.getEndDate().before(coupon.getStartDate())) {
			throw new IllegalArgumentException("結束日期不能早於開始日期");
		}
		if (couponRepository.existsActiveByShopIdAndCouponCode(coupon.getShop().getShopId(), coupon.getCouponCode(),
				new Date())) {
			throw new IllegalArgumentException("此商店已存在使用中的相同優惠券代碼");
		}
		if (couponRepository.existsActiveByShopIdAndCouponName(coupon.getShop().getShopId(), coupon.getCouponName(),
				new Date())) {
			throw new IllegalArgumentException("此商店已存在使用中的相同優惠券名稱");
		}

		return couponRepository.save(coupon);
	}

	@Transactional
	public Coupon modifyCouponInternal(Coupon updatedCouponData, Integer adminUserId) {
		log.info("Admin [{}] modifying coupon internally: ID {}", adminUserId, updatedCouponData.getCouponId());
		if (updatedCouponData.getCouponId() == null) {
			throw new IllegalArgumentException("內部修改優惠券需要 couponId");
		}
		Coupon existing = couponRepository.findById(updatedCouponData.getCouponId())
				.orElseThrow(() -> new RuntimeException("找不到要修改的優惠券 ID: " + updatedCouponData.getCouponId()));

		// Apply updates selectively (Admin can change more fields potentially)
		if (updatedCouponData.getCouponName() != null)
			existing.setCouponName(updatedCouponData.getCouponName());
		if (updatedCouponData.getCouponCode() != null)
			existing.setCouponCode(updatedCouponData.getCouponCode()); // Admin might change code
		if (updatedCouponData.getDescription() != null)
			existing.setDescription(updatedCouponData.getDescription());
		if (updatedCouponData.getDiscountType() != null)
			existing.setDiscountType(updatedCouponData.getDiscountType());
		if (updatedCouponData.getDiscountValue() != null)
			existing.setDiscountValue(updatedCouponData.getDiscountValue());
		if (updatedCouponData.getStartDate() != null)
			existing.setStartDate(updatedCouponData.getStartDate());
		if (updatedCouponData.getEndDate() != null)
			existing.setEndDate(updatedCouponData.getEndDate());
		if (updatedCouponData.getUsageLimit() != null)
			existing.setUsageLimit(updatedCouponData.getUsageLimit());
		if (updatedCouponData.getUsagePerUser() != null)
			existing.setUsagePerUser(updatedCouponData.getUsagePerUser());
		// Admin might change redeemed status? Be cautious.
		// if (updatedCouponData.isRedeemed() != existing.isRedeemed())
		// existing.setRedeemed(updatedCouponData.isRedeemed());
		// Admin might change the shop? Be very cautious.
		// if (updatedCouponData.getShop() != null &&
		// !updatedCouponData.getShop().getShopId().equals(existing.getShop().getShopId()))
		// {
		// Shop newShop = findShop(updatedCouponData.getShop().getShopId());
		// existing.setShop(newShop);
		// }

		// Validate dates
		if (existing.getEndDate().before(existing.getStartDate())) {
			throw new IllegalArgumentException("結束日期不能早於開始日期");
		}
		// Add uniqueness checks if name/code changed

		existing.setUpdatedAt(new Date());
		return couponRepository.save(existing);
	}

	@Transactional
	public boolean deleteCouponInternal(Integer couponId, Integer adminUserId) {
		log.info("Admin [{}] deleting coupon internally: ID {}", adminUserId, couponId);
		if (!couponRepository.existsById(couponId)) {
			log.warn("Coupon ID [{}] not found for internal deletion.", couponId);
			// Decide whether to throw an exception or return false
			throw new RuntimeException("找不到要刪除的優惠券 ID: " + couponId);
			// return false;
		}
		try {
			// Consider related data before deleting (e.g., applications referencing this
			// coupon)
			// If FKs are set to SET NULL, deletion should be okay.
			couponRepository.deleteById(couponId);
			log.info("Coupon ID [{}] deleted internally by Admin [{}].", couponId, adminUserId);
			return true;
		} catch (Exception e) {
			log.error("Error during internal deletion of coupon ID [{}]: {}", couponId, e.getMessage(), e);
			throw new RuntimeException("內部刪除優惠券失敗: " + e.getMessage(), e);
		}
	}

	// Admin: Find one coupon by ID (no shop/seller restriction)
	public Coupon findCouponByIdInternal(Integer couponId) {
		log.debug("Admin fetching coupon by ID [{}] internally.", couponId);
		return couponRepository.findById(couponId).orElseThrow(() -> new RuntimeException("找不到優惠券 ID: " + couponId));
	}

	  @Transactional(readOnly = true)
	    public List<Coupon> findCouponsInternal(Map<String, Object> criteria, int start, int rows) {
	         log.debug("Admin finding coupons internally with criteria: {}", criteria);
	         CriteriaBuilder cb = entityManager.getCriteriaBuilder();
	         CriteriaQuery<Coupon> cq = cb.createQuery(Coupon.class);
	         Root<Coupon> couponRoot = cq.from(Coupon.class);
	         // 提前 Join shop，LEFT JOIN 較安全
	         Join<Coupon, Shop> shopJoin = couponRoot.join("shop", JoinType.LEFT);
	         List<Predicate> mainPredicates = new ArrayList<>(); // 用於 AND 組合

	         // --- *** 修改：處理通用 searchText *** ---
	         Object searchTextValue = criteria.get("searchText");
	         if (searchTextValue != null && !searchTextValue.toString().trim().isEmpty()) {
	             String searchTextTrimmed = searchTextValue.toString().trim();
	             Predicate searchCondition = null; // 用於 OR 組合
	             Integer searchId = null;

	             try {
	                 searchId = Integer.parseInt(searchTextTrimmed);
	                 // --- 精確數字搜尋 (ID 或 Shop ID) ---
	                 log.debug("Search text is numeric. Searching for Coupon ID or Shop ID: {}", searchId);
	                 Predicate couponIdMatch = cb.equal(couponRoot.get("couponId"), searchId);
	                 // 確保 shopJoin 在這裡可用
	                 Predicate shopIdMatch = cb.equal(shopJoin.get("shopId"), searchId);
	                 searchCondition = cb.or(couponIdMatch, shopIdMatch);
	             } catch (NumberFormatException e) {
	                 // --- 文字搜尋 (名稱或代碼，不分大小寫) ---
	                 String lowerSearchText = "%" + searchTextTrimmed.toLowerCase() + "%";
	                 log.debug("Search text is not numeric. Searching for Name or Code (case-insensitive): {}", lowerSearchText);
	                 Predicate nameMatch = cb.like(cb.lower(couponRoot.get("couponName")), lowerSearchText);
	                 Predicate codeMatch = cb.like(cb.lower(couponRoot.get("couponCode")), lowerSearchText);
	                 searchCondition = cb.or(nameMatch, codeMatch);
	             }
	             // 將 searchText 的 OR 條件加入主 Predicate 列表
	             if (searchCondition != null) {
	                 mainPredicates.add(searchCondition);
	             }
	         }
	         // --- *** 結束 searchText 處理 *** ---

	         // 處理其他獨立的篩選條件 (使用 AND 連接)
	         criteria.forEach((key, value) -> {
	            // 排除 searchText 和分頁參數
	            if ("searchText".equals(key) || "page".equals(key) || "size".equals(key) || value == null || (value instanceof String && ((String) value).isEmpty())) {
	                return;
	            }
	            try {
	                switch (key) {
	                    // 注意：如果 searchText 是數字，這裡的 couponId/shopId 條件會與上面的 OR 條件 AND 在一起
	                    // 這可能不是預期行為，如果希望獨立篩選優先，則需要調整邏輯
	                    // 例如：如果傳了 searchText，就忽略獨立的 couponId/shopId 參數？
	                    case "couponId":
	                        // 只有在 searchText 不是數字時才考慮獨立的 couponId 篩選？
	                        // if (searchId == null) // 或者總是允許，讓它們 AND 在一起
	                        mainPredicates.add(cb.equal(couponRoot.get("couponId"), value));
	                        break;
	                    case "shopId":
	                        // 只有在 searchText 不是數字時才考慮獨立的 shopId 篩選？
	                        // if (searchId == null)
	                        mainPredicates.add(cb.equal(shopJoin.get("shopId"), value)); // 使用 Join
	                        break;
	                    case "discountType":
	                        mainPredicates.add(cb.equal(couponRoot.get("discountType"), value));
	                        break;
	                    case "isActiveNow":
	                         if (Boolean.TRUE.equals(value)) {
	                             Date now = new Date();
	                             mainPredicates.add(cb.lessThanOrEqualTo(couponRoot.get("startDate"), now));
	                             mainPredicates.add(cb.greaterThanOrEqualTo(couponRoot.get("endDate"), now));
	                         }
	                         break;
	                    default:
	                         log.warn("Unsupported criteria key in findCouponsInternal: {}", key);
	                 }
	            } catch (Exception e) {
	                 log.error("Error processing criteria key '{}' with value '{}': {}", key, value, e.getMessage());
	            }
	         });

	         cq.where(mainPredicates.toArray(new Predicate[0])); // 所有條件用 AND 連接
	         cq.orderBy(cb.desc(couponRoot.get("createdAt")));

	         TypedQuery<Coupon> query = entityManager.createQuery(cq);
	         if (start >= 0 && rows > 0) {
	            query.setFirstResult(start);
	            query.setMaxResults(rows);
	         }
	         return query.getResultList();
	    }

	     // Admin: Count coupons using dynamic criteria (再次修正版)
	     public long countCouponsInternal(Map<String, Object> criteria) {
	         log.debug("Admin counting coupons internally with criteria: {}", criteria);
	         CriteriaBuilder cb = entityManager.getCriteriaBuilder();
	         CriteriaQuery<Long> cq = cb.createQuery(Long.class);
	         Root<Coupon> couponRoot = cq.from(Coupon.class);
	         Join<Coupon, Shop> shopJoin = couponRoot.join("shop", JoinType.LEFT); // 提前 Join
	         cq.select(cb.count(couponRoot));
	         List<Predicate> mainPredicates = new ArrayList<>(); // 主條件列表

	         // --- *** 修改：處理通用 searchText (邏輯同上) *** ---
	         Object searchTextValue = criteria.get("searchText");
	         if (searchTextValue != null && !searchTextValue.toString().trim().isEmpty()) {
	              String searchTextTrimmed = searchTextValue.toString().trim();
	             Predicate searchCondition = null;
	             Integer searchId = null;
	             try { searchId = Integer.parseInt(searchTextTrimmed); } catch (NumberFormatException e) { /* ignore */ }

	             if (searchId != null) {
	                 Predicate couponIdMatch = cb.equal(couponRoot.get("couponId"), searchId);
	                 Predicate shopIdMatch = cb.equal(shopJoin.get("shopId"), searchId);
	                 searchCondition = cb.or(couponIdMatch, shopIdMatch);
	             } else {
	                 String lowerSearchText = "%" + searchTextTrimmed.toLowerCase() + "%";
	                 Predicate nameMatch = cb.like(cb.lower(couponRoot.get("couponName")), lowerSearchText);
	                 Predicate codeMatch = cb.like(cb.lower(couponRoot.get("couponCode")), lowerSearchText);
	                 searchCondition = cb.or(nameMatch, codeMatch);
	             }
	              if (searchCondition != null) {
	                 mainPredicates.add(searchCondition);
	             }
	             criteria.remove("searchText"); // 從 criteria 移除以免下面重複處理
	         }
	         // --- *** 結束 searchText 處理 *** ---

	         // 處理其他獨立的篩選條件 (邏輯同上)
	          criteria.forEach((key, value) -> {
	             if ("page".equals(key) || "size".equals(key) || value == null || (value instanceof String && ((String) value).isEmpty())) return;
	             try {
	                 switch (key) {
	                    case "couponId": mainPredicates.add(cb.equal(couponRoot.get("couponId"), value)); break;
	                    case "shopId": mainPredicates.add(cb.equal(shopJoin.get("shopId"), value)); break; // 使用 Join
	                    case "discountType": mainPredicates.add(cb.equal(couponRoot.get("discountType"), value)); break;
	                    case "isActiveNow": if (Boolean.TRUE.equals(value)) { Date now = new Date(); mainPredicates.add(cb.lessThanOrEqualTo(couponRoot.get("startDate"), now)); mainPredicates.add(cb.greaterThanOrEqualTo(couponRoot.get("endDate"), now)); } break;
	                    default: log.warn("Unsupported criteria key in countCouponsInternal: {}", key);
	                 }
	             } catch (Exception e) { log.error("Error processing count criteria key '{}': {}", key, e.getMessage()); }
	         });

	         cq.where(mainPredicates.toArray(new Predicate[0])); // 所有條件用 AND 連接
	         return entityManager.createQuery(cq).getSingleResult();
	    }

     
	// === Helper Methods ===

	private Shop validateSellerShop(Integer sellerId, Integer shopId) {
		if (sellerId == null || shopId == null) {
			throw new IllegalArgumentException("需要 SellerId 和 ShopId");
		}
		Shop shop = shopRepository.findById(shopId).orElseThrow(() -> new RuntimeException("找不到商店 ID: " + shopId));
		if (shop.getUser() == null || !shop.getUser().getUserId().equals(sellerId)) {
			throw new SecurityException("權限不足：您無權操作此商店的優惠券");
		}
		return shop;
	}

	private Shop findShop(Integer shopId) {
		return shopRepository.findById(shopId).orElseThrow(() -> new RuntimeException("找不到商店 ID: " + shopId));
	}

	private CouponApplication findAndValidateApplication(Integer applicationId, String expectedStatus) {
		CouponApplication app = applicationRepository.findById(applicationId)
				.orElseThrow(() -> new RuntimeException("找不到申請紀錄 ID: " + applicationId));
		if (!expectedStatus.equals(app.getStatus())) {
			throw new IllegalStateException("申請狀態不符 (預期: " + expectedStatus + ", 實際: " + app.getStatus() + ")");
		}
		return app;
	}

	// Helper to map application data to a Coupon object (for create/update internal
	// calls)
	private Coupon mapApplicationToCoupon(CouponApplication app) {
		Coupon coupon = new Coupon();
		// ID is not set here, set by createInternal or modifyInternal
		coupon.setCouponName(app.getCouponName());
		coupon.setCouponCode(app.getCouponCode());
		coupon.setDescription(app.getDescription());
		coupon.setDiscountType(app.getDiscountType());
		coupon.setDiscountValue(app.getDiscountValue());
		coupon.setStartDate(app.getStartDate());
		coupon.setEndDate(app.getEndDate());
		coupon.setUsageLimit(app.getUsageLimit());
		coupon.setUsagePerUser(app.getUsagePerUser());
		// Shop association happens in the calling method (approveCreate/approveUpdate)
		// Timestamps are set by internal methods
		// Redeemed status defaults to false on creation
		return coupon;
	}

	// 領取優惠券
	public boolean redeemCoupon(Integer couponId) {
		Optional<Coupon> couponOpt = couponRepository.findById(couponId);

		if (couponOpt.isPresent()) {
			Coupon coupon = couponOpt.get();
			if (!coupon.isRedeemed()) { // 確保優惠券未被使用
				coupon.setRedeemed(true);
				couponRepository.save(coupon);
				return true;
			}
		}
		return false;
	}

	// 取得所有未領取的優惠券
	public List<Coupon> getAllAvailableCoupons() {
		return couponRepository.findByRedeemedFalse();
	}

	// 取得已領取的優惠券
	public List<Coupon> getRedeemedCoupons() {
		return couponRepository.findByRedeemedTrue();
	}

	// 計算「未領取」的優惠券總數（可支援 search）
	public int countUnclaimed(String search) {
		if (search == null || search.trim().isEmpty()) {
			return couponRepository.countByRedeemedFalse();
		} else {
			return couponRepository.countByRedeemedFalseAndCouponNameLike("%" + search + "%");
		}
	}

	// 取得「未領取」優惠券（分頁 + 搜尋）
	public List<Coupon> getUnclaimedCoupons(String search, int page, int rows) {
		int offset = (page - 1) * rows;
		if (search == null || search.trim().isEmpty()) {
			return couponRepository.findUnclaimedPage(offset, rows);
		} else {
			return couponRepository.findUnclaimedPageByName("%" + search + "%", offset, rows);
		}
	}

	// 計算「已領取」的優惠券總數
	public int countRedeemed(String search) {
		if (search == null || search.trim().isEmpty()) {
			return couponRepository.countByRedeemedTrue();
		} else {
			return couponRepository.countByRedeemedTrueAndCouponNameLike("%" + search + "%");
		}
	}

	// 取得「已領取」優惠券（分頁 + 搜尋）
	public List<Coupon> getRedeemedCoupons(String search, int page, int rows) {
		int offset = (page - 1) * rows;
		if (search == null || search.trim().isEmpty()) {
			return couponRepository.findRedeemedPage(offset, rows);
		} else {
			return couponRepository.findRedeemedPageByName("%" + search + "%", offset, rows);
		}
	}

	 /**
     * 公開查找有效的優惠券列表 (用於結帳時顯示給使用者)。
     * @param shopId 可選，若提供則只查找該商店的優惠券。
     * @param page 頁碼 (從 0 開始)。
     * @param size 每頁數量。
     * @return 包含 PublicCouponDTO 列表和分頁資訊的 Map。
     */
	 @Transactional(readOnly = true) // 查詢操作建議設為 readOnly
	    public Map<String, Object> findActivePublicCoupons(Integer shopId, int page, int size) {
	        log.debug("Finding active public coupons for shopId [{}], page [{}], size [{}]", shopId, page, size);
	        Date now = new Date();

	        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

	        // --- 查詢列表 ---
	        CriteriaQuery<Coupon> cq = cb.createQuery(Coupon.class);
	        Root<Coupon> couponRoot = cq.from(Coupon.class);
	        List<Predicate> predicates = new ArrayList<>();

	        predicates.add(cb.lessThanOrEqualTo(couponRoot.get("startDate"), now)); // 已開始
	        predicates.add(cb.greaterThanOrEqualTo(couponRoot.get("endDate"), now));   // 未結束

	        // *** 修正：明確定義 Join ***
	        if (shopId != null) {
	            // 從 Coupon Join 到 Shop
	            Join<Coupon, Shop> shopJoin = couponRoot.join("shop", JoinType.INNER); // 使用 INNER JOIN
	            predicates.add(cb.equal(shopJoin.get("shopId"), shopId)); // 使用 Join 物件進行篩選
	        }
	        // else: shopId 為 null，不加入商店篩選

	        cq.where(predicates.toArray(new Predicate[0]));
	        cq.orderBy(cb.asc(couponRoot.get("endDate"))); // 可依結束日期排序

	        TypedQuery<Coupon> query = entityManager.createQuery(cq);
	        query.setFirstResult(page * size);
	        query.setMaxResults(size);
	        List<Coupon> coupons = query.getResultList();

	        // --- 計算總數 (使用相同條件) ---
	        long totalCount = countActivePublicCoupons(shopId); // 呼叫獨立的計數方法

	        // --- 轉換為 DTO ---
	        List<PublicCouponDTO> dtoList = coupons.stream()
	                .map(this::convertToPublicDTO)
	                .collect(Collectors.toList());

	        // --- 組合回應 ---
	        Map<String, Object> response = new HashMap<>();
	        response.put("list", dtoList);
	        response.put("currentPage", page);
	        response.put("itemsPerPage", size); // 使用 itemsPerPage 更清晰
	        response.put("totalItems", totalCount);
	        response.put("totalPages", (int) Math.ceil((double) totalCount / size));

	        return response;
	    }


	    /**
	     * 計算符合條件的公開有效優惠券總數。
	     * @param shopId 可選，若提供則只計算該商店的券數。
	     * @return 總數。
	     */
	    public long countActivePublicCoupons(Integer shopId) {
	        log.debug("Counting active public coupons for shopId [{}]", shopId);
	        Date now = new Date();
	        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
	        CriteriaQuery<Long> countCq = cb.createQuery(Long.class);
	        Root<Coupon> countRoot = countCq.from(Coupon.class);
	        countCq.select(cb.count(countRoot));
	        List<Predicate> predicates = new ArrayList<>();

	        predicates.add(cb.lessThanOrEqualTo(countRoot.get("startDate"), now)); // 已開始
	        predicates.add(cb.greaterThanOrEqualTo(countRoot.get("endDate"), now));   // 未結束

	        // *** 修正：明確定義 Join (與查詢列表時一致) ***
	        if (shopId != null) {
	            Join<Coupon, Shop> shopJoin = countRoot.join("shop", JoinType.INNER);
	            predicates.add(cb.equal(shopJoin.get("shopId"), shopId));
	        }

	        countCq.where(predicates.toArray(new Predicate[0]));
	        return entityManager.createQuery(countCq).getSingleResult();
	    }


    /**
     * 根據優惠券代碼驗證並取得有效的公開優惠券資訊 (用於結帳輸入代碼)。
     * @param couponCode 優惠券代碼。
     * @param shopId 可選，若提供則驗證此券是否適用於該商店。
     * @return 有效的 PublicCouponDTO。
     * @throws RuntimeException 若找不到、已過期、不適用等。
     */
	    @Transactional(readOnly = true) // 查詢操作
	    public PublicCouponDTO validateAndGetCouponByCode(String couponCode, Integer shopId) {
	        log.debug("Validating coupon code [{}] for shopId [{}]", couponCode, shopId);
	        if (couponCode == null || couponCode.trim().isEmpty()) {
	            throw new IllegalArgumentException("優惠券代碼不可為空");
	        }

	        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
	        CriteriaQuery<Coupon> cq = cb.createQuery(Coupon.class);
	        Root<Coupon> couponRoot = cq.from(Coupon.class);
	        List<Predicate> predicates = new ArrayList<>();
	        Date now = new Date();

	        predicates.add(cb.equal(couponRoot.get("couponCode"), couponCode));       // 符合代碼
	        predicates.add(cb.lessThanOrEqualTo(couponRoot.get("startDate"), now)); // 已開始
	        predicates.add(cb.greaterThanOrEqualTo(couponRoot.get("endDate"), now));   // 未結束

	        // *** 修正：明確定義 Join ***
	        if (shopId != null) {
	            Join<Coupon, Shop> shopJoin = couponRoot.join("shop", JoinType.INNER);
	            predicates.add(cb.equal(shopJoin.get("shopId"), shopId));
	        }
	        // else: 不篩選 shopId

	        cq.where(predicates.toArray(new Predicate[0]));

	        TypedQuery<Coupon> query = entityManager.createQuery(cq);
	        try {
	            Coupon coupon = query.getSingleResult();
	            log.info("Coupon code [{}] validated successfully for couponId [{}]", couponCode, coupon.getCouponId());
	            return convertToPublicDTO(coupon);
	        } catch (NoResultException e) {
	            log.warn("Coupon code [{}] not found or not valid/active for shopId [{}]", couponCode, shopId);
	            throw new RuntimeException("無效或不適用的優惠券代碼"); // 或自訂 NotFoundException
	        } catch (Exception e) {
	            log.error("Error validating coupon code [{}]: {}", couponCode, e.getMessage(), e);
	            throw new RuntimeException("驗證優惠券代碼時發生錯誤");
	        }
	    }

    // --- Helper Method to convert Coupon to PublicCouponDTO ---
    private PublicCouponDTO convertToPublicDTO(Coupon coupon) {
        if (coupon == null) return null;
        PublicCouponDTO dto = new PublicCouponDTO();
        dto.setCouponId(coupon.getCouponId());
        dto.setCouponName(coupon.getCouponName());
        dto.setDescription(coupon.getDescription());
        dto.setDiscountType(coupon.getDiscountType());
        dto.setDiscountValue(coupon.getDiscountValue());
        dto.setStartDate(DatetimeConverter.toString(coupon.getStartDate(), "yyyy-MM-dd"));
        dto.setEndDate(DatetimeConverter.toString(coupon.getEndDate(), "yyyy-MM-dd"));
        // Optionally add shop info if needed
        // dto.setShopId(coupon.getShop() != null ? coupon.getShop().getShopId() : null);
        // dto.setShopName(coupon.getShop() != null ? coupon.getShop().getShopName() : null);
        return dto;
    }
    public AdminCouponDTO convertToAdminDTO(Coupon coupon) {
        if (coupon == null) return null;
        AdminCouponDTO dto = new AdminCouponDTO();
        dto.setCouponId(coupon.getCouponId());
        dto.setCouponCode(coupon.getCouponCode());
        dto.setCouponName(coupon.getCouponName());
        dto.setDescription(coupon.getDescription());
        dto.setDiscountType(coupon.getDiscountType());
        dto.setDiscountValue(coupon.getDiscountValue());
        dto.setStartDate(DatetimeConverter.toString(coupon.getStartDate(), "yyyy-MM-dd"));
        dto.setEndDate(DatetimeConverter.toString(coupon.getEndDate(), "yyyy-MM-dd"));
        dto.setUsageLimit(coupon.getUsageLimit());
        dto.setUsagePerUser(coupon.getUsagePerUser());
        dto.setRedeemed(coupon.isRedeemed());
        dto.setCreatedAt(DatetimeConverter.toString(coupon.getCreatedAt(), "yyyy-MM-dd HH:mm:ss"));
        dto.setUpdatedAt(DatetimeConverter.toString(coupon.getUpdatedAt(), "yyyy-MM-dd HH:mm:ss"));
        if (coupon.getShop() != null) {
            dto.setShopId(coupon.getShop().getShopId());
            // Assuming Shop entity has shopName getter
            dto.setShopName(coupon.getShop().getShopName());
        }
        return dto;
    }

    // Helper to convert CouponApplication to AdminCouponApplicationDTO
    // IMPORTANT: Needs access to UserRepository/ShopRepository if seller/shop names are required.
    // Call within @Transactional.
    public AdminCouponApplicationDTO convertToAdminAppDTO(CouponApplication app) {
         if (app == null) return null;
         AdminCouponApplicationDTO dto = new AdminCouponApplicationDTO();
         dto.setApplicationId(app.getApplicationId());
         dto.setApplicationType(app.getApplicationType());
         dto.setRequestedBySellerId(app.getRequestedBySellerId());
         dto.setRequestedShopId(app.getRequestedShopId());
         dto.setTargetCouponId(app.getTargetCouponId());
         dto.setStatus(app.getStatus());
         dto.setApplicationDate(DatetimeConverter.toString(app.getApplicationDate(), "yyyy-MM-dd HH:mm:ss"));
         dto.setAdminNotes(app.getAdminNotes());
         dto.setResultingCouponId(app.getResultingCouponId());
         // Populate proposed values relevant for list display
         dto.setCouponName(app.getCouponName());
         dto.setCouponCode(app.getCouponCode());

         // --- Fetch related names (requires Repositories) ---
         // This demonstrates why mapping might happen in the service where repos are available
         try {
            shopRepository.findById(app.getRequestedShopId())
                .ifPresent(shop -> dto.setRequestedShopName(shop.getShopName()));
         } catch (Exception e) { log.warn("Could not find shop name for shopId {}", app.getRequestedShopId()); }

          try {
            userRepository.findByUserId(app.getRequestedBySellerId()) // Assuming findByUserId exists
                .ifPresent(user -> dto.setRequestedBySellerName(user.getUsername()));
          } catch (Exception e) { log.warn("Could not find seller name for userId {}", app.getRequestedBySellerId()); }
          // --- End Fetch related names ---

         return dto;
    }

    // Helper to convert Coupon to SellerCouponDTO
    public SellerCouponDTO convertToSellerDTO(Coupon coupon) {
         if (coupon == null) return null;
         SellerCouponDTO dto = new SellerCouponDTO();
         dto.setCouponId(coupon.getCouponId());
         dto.setCouponCode(coupon.getCouponCode());
         dto.setCouponName(coupon.getCouponName());
         dto.setDescription(coupon.getDescription());
         dto.setDiscountType(coupon.getDiscountType());
         dto.setDiscountValue(coupon.getDiscountValue());
         dto.setStartDate(DatetimeConverter.toString(coupon.getStartDate(), "yyyy-MM-dd"));
         dto.setEndDate(DatetimeConverter.toString(coupon.getEndDate(), "yyyy-MM-dd"));
         dto.setUsageLimit(coupon.getUsageLimit());
         dto.setUsagePerUser(coupon.getUsagePerUser());
         dto.setRedeemed(coupon.isRedeemed());
         return dto;
    }

    // Modify methods that return entities to return DTOs or lists of DTOs
    // Example modification for getPendingApplications:
    public List<AdminCouponApplicationDTO> getPendingApplicationDTOs() {
        log.debug("Admin fetching pending applications as DTOs.");
        List<String> pendingStatuses = List.of("PENDING_CREATE", "PENDING_UPDATE", "PENDING_DELETE");
        List<CouponApplication> applications = applicationRepository.findByStatusInOrderByApplicationDateDesc(pendingStatuses);
        // Convert list of entities to list of DTOs
        return applications.stream()
                           .map(this::convertToAdminAppDTO) // Use the conversion helper
                           .collect(Collectors.toList());
    }

    // Example modification for findCouponByIdInternal:
    public AdminCouponDTO findCouponByIdInternalDTO(Integer couponId) {
        log.debug("Admin fetching coupon by ID [{}] internally as DTO.", couponId);
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new RuntimeException("找不到優惠券 ID: " + couponId));
        return convertToAdminDTO(coupon); // Convert to DTO
    }

    // Example modification for findCouponsInternal:
    public Map<String, Object> findCouponsInternalDTO(Map<String, Object> criteria, int page, int size) {
         log.debug("Admin finding coupons internally as DTOs with criteria: {}", criteria);
         int start = page * size;
         List<Coupon> coupons = findCouponsInternal(criteria, start, size); // Use existing internal method
         long totalCount = countCouponsInternal(criteria); // Use existing internal method

         List<AdminCouponDTO> dtoList = coupons.stream()
                                             .map(this::convertToAdminDTO)
                                             .collect(Collectors.toList());

         Map<String, Object> response = new HashMap<>();
         response.put("list", dtoList);
         response.put("currentPage", page);
         response.put("itemsPerPage", size);
         response.put("totalItems", totalCount);
         response.put("totalPages", (int) Math.ceil((double) totalCount / size));
         return response;
    }

    // Example modification for findSellerActiveCoupons:
     public List<SellerCouponDTO> findSellerActiveCouponDTOs(Integer sellerId, Integer shopId) {
        log.debug("Seller [{}] fetching active coupons for shop [{}] as DTOs", sellerId, shopId);
        validateSellerShop(sellerId, shopId); // Permission check
        List<Coupon> coupons = couponRepository.findByShop_ShopIdAndEndDateGreaterThanEqualOrderByEndDateAsc(shopId, new Date());
        return coupons.stream()
                      .map(this::convertToSellerDTO)
                      .collect(Collectors.toList());
     }

     /**
      * 取得最近幾個月的優惠券統計數據。
      * @param monthsToGoBack 要回溯的月數 (例如 6 代表包含當月在內的最近 6 個月)。
      * @return Map 包含 "labels" (月份字串列表), "newCounts" (每月新增數列表), "currentCounts" (每月月底有效數列表)。
      */
     @Transactional(readOnly = true) // 查詢操作
     public Map<String, Object> getMonthlyCouponStats(int monthsToGoBack) {
         log.debug("Fetching monthly coupon stats for the last {} months.", monthsToGoBack);
         if (monthsToGoBack <= 0) {
             monthsToGoBack = 6; // 預設值
         }

         List<String> labels = new ArrayList<>();
         List<Long> newCountsList = new ArrayList<>();
         List<Long> currentCountsList = new ArrayList<>();

         // 1. 確定日期範圍和月份列表
         Calendar calendar = Calendar.getInstance();
         // 將時間設為當月最後一天，確保計算當月月底數據
         calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
         setEndOfDay(calendar); // 將時間設為 23:59:59.999
         Date endDateExclusive = calendar.getTime(); // 當月最後一天的結束，用於 newCounts 查詢

         calendar.add(Calendar.MONTH, -(monthsToGoBack - 1)); // 回溯到起始月份
         calendar.set(Calendar.DAY_OF_MONTH, 1); // 設定為該月第一天
         setStartOfDay(calendar); // 將時間設為 00:00:00.000
         Date startDateInclusive = calendar.getTime(); // 起始日期的開始

         List<YearMonth> months = IntStream.range(0, monthsToGoBack)
                 .mapToObj(i -> YearMonth.from(startDateInclusive.toInstant().atZone(ZoneId.systemDefault())).plusMonths(i))
                 .collect(Collectors.toList());

         DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");

         // 2. 查詢期間內每月新增數量
         List<Map<String, Object>> monthlyNewCountsData = couponRepository.findMonthlyCreationCounts(startDateInclusive, endDateExclusive);
         Map<String, Long> newCountsMap = monthlyNewCountsData.stream()
                 .collect(Collectors.toMap(
                         map -> (String) map.get("monthYear"), // key: "yyyy-MM"
                         map -> ((Number) map.get("count")).longValue() // value: count (轉為 Long)
                 ));

         // 3. 迭代月份列表，查詢月底有效數量並組合結果
         for (YearMonth month : months) {
             String monthLabel = month.format(formatter);
             labels.add(monthLabel); // 加入標籤

             // 取得該月新增數量 (若無則為 0)
             newCountsList.add(newCountsMap.getOrDefault(monthLabel, 0L));

             // 計算該月最後一天的日期
             Calendar monthEndCal = Calendar.getInstance();
             monthEndCal.set(Calendar.YEAR, month.getYear());
             monthEndCal.set(Calendar.MONTH, month.getMonthValue() - 1); // Calendar 月份從 0 開始
             monthEndCal.set(Calendar.DAY_OF_MONTH, monthEndCal.getActualMaximum(Calendar.DAY_OF_MONTH));
             setEndOfDay(monthEndCal); // 設定為該日結束時間
             Date monthEndDate = monthEndCal.getTime();

             // 查詢該月月底的有效數量
             long activeCount = couponRepository.countActiveAtDate(monthEndDate);
             currentCountsList.add(activeCount);
         }

         // 4. 組合最終結果 Map
         Map<String, Object> stats = new HashMap<>();
         stats.put("labels", labels);
         stats.put("newCounts", newCountsList);
         stats.put("currentCounts", currentCountsList); // 代表 "月底有效數量"

         log.debug("Monthly stats result: {}", stats);
         return stats;
     }

     // Helper to set Calendar to end of day
     private void setEndOfDay(Calendar cal) {
         cal.set(Calendar.HOUR_OF_DAY, 23);
         cal.set(Calendar.MINUTE, 59);
         cal.set(Calendar.SECOND, 59);
         cal.set(Calendar.MILLISECOND, 999);
     }

     // Helper to set Calendar to start of day
     private void setStartOfDay(Calendar cal) {
         cal.set(Calendar.HOUR_OF_DAY, 0);
         cal.set(Calendar.MINUTE, 0);
         cal.set(Calendar.SECOND, 0);
         cal.set(Calendar.MILLISECOND, 0);
     }
     
     /**
      * 獲取待審核的優惠券申請數量。
      * @return 待審核申請的總數。
      */
     @Transactional(readOnly = true) // 查詢操作
     public long getPendingApplicationCount() {
         log.debug("Fetching pending coupon application count.");
         List<String> pendingStatuses = List.of("PENDING_CREATE", "PENDING_UPDATE", "PENDING_DELETE");
         return applicationRepository.countByStatusIn(pendingStatuses);
     }

    
}
