package ourpkg.coupon.service;

import java.util.List;

import org.json.JSONObject;

import ourpkg.coupon.entity.Coupon;

public interface CouponDAO {
	public long count(JSONObject obj);
	public abstract List<Coupon> find(JSONObject obj);
}
