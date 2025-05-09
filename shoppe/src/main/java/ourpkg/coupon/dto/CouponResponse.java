package ourpkg.coupon.dto;

import java.util.List;

import ourpkg.coupon.entity.Coupon;

public class CouponResponse {
	private Boolean success;
	private String message;
	private Long count;
	private List<Coupon> list;
	@Override
	public String toString() {
		return "CouponResponse [success=" + success + ", message=" + message + ", count=" + count + ", list=" + list
				+ "]";
	}
	public Boolean getSuccess() {
		return success;
	}
	public void setSuccess(Boolean success) {
		this.success = success;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	public Long getCount() {
		return count;
	}
	public void setCount(Long count) {
		this.count = count;
	}
	public List<Coupon> getList() {
		return list;
	}
	public void setList(List<Coupon> list) {
		this.list = list;
	}
}
