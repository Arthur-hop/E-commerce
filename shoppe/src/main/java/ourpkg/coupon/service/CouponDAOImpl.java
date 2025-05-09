package ourpkg.coupon.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;

import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import ourpkg.coupon.entity.Coupon;
import ourpkg.coupon.util.DatetimeConverter;
import ourpkg.shop.Shop;
import ourpkg.user_role_permission.user.UserRepository;

public class CouponDAOImpl implements CouponDAO {

	@PersistenceContext
	private Session session;

	public Session getSession() {
		return session;
	}

	@Autowired
	private UserRepository userRepository;

	@Override
	public long count(JSONObject obj) {
		Integer couponId = obj.isNull("couponId") ? null : obj.getInt("couponId");
		String couponCode = obj.isNull("couponCode") ? null : obj.getString("couponCode");
		String couponName = obj.isNull("couponName") ? null : obj.getString("couponName");
		String discountType = obj.isNull("discountType") ? null : obj.getString("discountType");
		BigDecimal discountValue = obj.isNull("discountValue") ? null : obj.getBigDecimal("discountValue");
		String startDate = obj.isNull("startDate") ? null : obj.getString("startDate");
		String endDate = obj.isNull("endDate") ? null : obj.getString("endDate");
		Integer usageLimit = obj.isNull("usageLimit") ? null : obj.getInt("usageLimit");
		Integer usagePerUser = obj.isNull("usagePerUser") ? null : obj.getInt("usagePerUser");
		String createdAt = obj.isNull("createdAt") ? null : obj.getString("createdAt");
		String updatedAt = obj.isNull("updatedAt") ? null : obj.getString("updatedAt");
		Integer shopId = obj.isNull("shopId") ? null : obj.getInt("shopId");

		// 取得透過 Shop 取得 userId 條件 (從前端傳入)
		Integer userId = obj.isNull("userId") ? null : obj.getInt("userId");

		CriteriaBuilder criteriaBuilder = this.getSession().getCriteriaBuilder();
		CriteriaQuery<Long> criteriaQuery = criteriaBuilder.createQuery(Long.class);
		Root<Coupon> root = criteriaQuery.from(Coupon.class);
		criteriaQuery = criteriaQuery.select(criteriaBuilder.count(root));

		List<Predicate> predicates = new ArrayList<>();
		if (couponId != null) {
			predicates.add(criteriaBuilder.equal(root.get("couponId"), couponId));
		}
		if (couponName != null && !couponName.isEmpty()) {
			predicates.add(criteriaBuilder.like(root.get("couponName"), "%" + couponName + "%"));
		}
		if (couponCode != null && !couponCode.isEmpty()) {
			predicates.add(criteriaBuilder.like(root.get("couponCode"), "%" + couponCode + "%"));
		}
		if (discountType != null && !discountType.isEmpty()) {
			predicates.add(criteriaBuilder.like(root.get("discountType"), "%" + discountType + "%"));
		}
		if (discountValue != null) {
			predicates.add(criteriaBuilder.equal(root.get("discountValue"), discountValue));
		}
		if (startDate != null && !startDate.isEmpty()) {
			java.util.Date date = DatetimeConverter.parse(startDate, "yyyy-MM-dd");
			predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("startDate"), date));
		}
		if (endDate != null && !endDate.isEmpty()) {
			java.util.Date date = DatetimeConverter.parse(endDate, "yyyy-MM-dd");
			predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("endDate"), date));
		}
		if (usageLimit != null) {
			predicates.add(criteriaBuilder.equal(root.get("usageLimit"), usageLimit));
		}
		if (usagePerUser != null) {
			predicates.add(criteriaBuilder.equal(root.get("usagePerUser"), usagePerUser));
		}
		if (createdAt != null && !createdAt.isEmpty()) {
			java.util.Date date = DatetimeConverter.parse(createdAt, "yyyy-MM-dd");
			predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), date));
		}
		if (updatedAt != null && !updatedAt.isEmpty()) {
			java.util.Date date = DatetimeConverter.parse(updatedAt, "yyyy-MM-dd");
			predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("updatedAt"), date));
		}
		if (shopId != null) {
			predicates.add(criteriaBuilder.equal(root.get("shop").get("shopId"), shopId));
		}

		// 透過 Shop 取得 User，從 Shop 直接獲取 userId
		Join<Coupon, Shop> shopJoin = root.join("shop");
		Join<Shop, ourpkg.user_role_permission.user.User> userJoin = shopJoin.join("user");

		// 若有指定 userId 條件，就進行過濾
		if (userId != null) {
			predicates.add(criteriaBuilder.equal(userJoin.get("userId"), userId));
		}
		// 避免 NullPointerException
		predicates.add(criteriaBuilder.isNotNull(userJoin.get("userId")));

		if (!predicates.isEmpty()) {
			criteriaQuery = criteriaQuery.where(predicates.toArray(new Predicate[0]));
		}
		TypedQuery<Long> typedQuery = this.getSession().createQuery(criteriaQuery);
		return typedQuery.getSingleResult();
	}

	@Override
	public List<Coupon> find(JSONObject obj) {
		int start = obj.isNull("start") ? 0 : obj.getInt("start");
		int rows = obj.isNull("rows") ? 0 : obj.getInt("rows");

		Integer couponId = obj.isNull("couponId") ? null : obj.getInt("couponId");
		String couponCode = obj.isNull("couponCode") ? null : obj.getString("couponCode");
		String couponName = obj.isNull("couponName") ? null : obj.getString("couponName");
		String discountType = obj.isNull("discountType") ? null : obj.getString("discountType");
		BigDecimal discountValue = obj.isNull("discountValue") ? null : obj.getBigDecimal("discountValue");
		Integer shopId = obj.isNull("shopId") ? null : obj.getInt("shopId");
		Integer userId = obj.isNull("userId") ? null : obj.getInt("userId"); // 從前端傳入的 userId

		CriteriaBuilder criteriaBuilder = this.getSession().getCriteriaBuilder();
		CriteriaQuery<Coupon> criteriaQuery = criteriaBuilder.createQuery(Coupon.class);
		Root<Coupon> root = criteriaQuery.from(Coupon.class);

		List<Predicate> predicates = new ArrayList<>();
		if (couponId != null) {
			predicates.add(criteriaBuilder.equal(root.get("couponId"), couponId));
		}
		if (couponName != null && !couponName.isEmpty()) {
			predicates.add(criteriaBuilder.like(root.get("couponName"), "%" + couponName + "%"));
		}
		if (couponCode != null && !couponCode.isEmpty()) {
			predicates.add(criteriaBuilder.like(root.get("couponCode"), "%" + couponCode + "%"));
		}
		if (discountType != null && !discountType.isEmpty()) {
			predicates.add(criteriaBuilder.like(root.get("discountType"), "%" + discountType + "%"));
		}
		if (discountValue != null) {
			predicates.add(criteriaBuilder.equal(root.get("discountValue"), discountValue));
		}
		if (shopId != null) {
			predicates.add(criteriaBuilder.equal(root.get("shop").get("shopId"), shopId));
		}

		// 透過 Shop 取得 User，從 Shop 直接獲取 userId
		Join<Coupon, Shop> shopJoin = root.join("shop");
		Join<Shop, ourpkg.user_role_permission.user.User> userJoin = shopJoin.join("user");

		// 若有指定 userId 條件，就進行過濾
		if (userId != null) {
			predicates.add(criteriaBuilder.equal(userJoin.get("userId"), userId));
		}
		// 避免 NullPointerException
		predicates.add(criteriaBuilder.isNotNull(userJoin.get("userId")));

		if (!predicates.isEmpty()) {
			criteriaQuery = criteriaQuery.where(predicates.toArray(new Predicate[0]));
		}

		TypedQuery<Coupon> typedQuery = this.getSession().createQuery(criteriaQuery).setFirstResult(start);
		if (rows != 0) {
			typedQuery = typedQuery.setMaxResults(rows);
		}

		List<Coupon> result = typedQuery.getResultList();
		return result != null && !result.isEmpty() ? result : new ArrayList<>();
	}
}