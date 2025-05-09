package ourpkg.review;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.Valid;
import ourpkg.user_role_permission.user.service.UserService;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/review")
@Validated
public class ReviewController {

	@Autowired
	private ReviewService reviewService;

	@Autowired
	private UserService userService;

	/**
	 * 訂單完成後新增評價（簡化版，用 ReviewRequest）
	 */
	@PostMapping("/submit")
	public ResponseEntity<Review> submitReview(@RequestParam Integer userId,
			@Valid @RequestBody ReviewRequest request) {
		System.out.println("收到的 userId: " + userId);
		System.out.println("ReviewRequest.orderItemId = " + request.getOrderItemId());
		System.out.println("ReviewRequest.productId = " + request.getProductId());
		System.out.println("ReviewRequest.content = " + request.getContent());
		System.out.println("ReviewRequest.rating = " + request.getRating());

		Review review = reviewService.createReviewFromRequest(userId, request);
		return ResponseEntity.status(HttpStatus.CREATED).body(review);
	}

	/**
	 * 新增評價
	 */
	@PostMapping
	public ResponseEntity<Review> createReview(@RequestParam Integer userId, @RequestParam Integer shopId,
			@RequestParam Integer productId, @Valid @RequestBody Review reviewData) {
		Review createdReview = reviewService.createReview(userId, shopId, productId, reviewData);
		return ResponseEntity.status(HttpStatus.CREATED).body(createdReview);
	}

    /**
     * 取得所有評價 (管理員專用)
     */
    // 測試完畢
    @GetMapping("/allreview")
    public ResponseEntity<List<ReviewDto>> getAllReviews() {
        List<Review> reviews = reviewService.getAllReviews();

        List<ReviewDto> result = reviews.stream().map(r ->
            new ReviewDto(
                r.getReviewId(),
                r.getProduct().getProductId(),
                r.getProductName(),
                r.getUser().getUserName(),
                r.getReviewContent(),
                r.getRating().intValue(),
                r.getStatus().name(),
                r.getCreatedAt().toString().substring(0, 10)
            )
        ).toList();

        return result.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(result);
    }


	/**
	 * 透過評價 ID 取得單筆評價
	 */
	// 測試完畢
	@GetMapping("/{reviewId}")
	public ResponseEntity<Review> getReviewById(@PathVariable Integer reviewId) {
		Review review = reviewService.getReviewById(reviewId);
		if (review == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Review not found");
		}
		return ResponseEntity.ok(review);
	}

	/**
	 * 透過商品 ID 查詢評價
	 */
	// 測試完畢
	@GetMapping("/product/{productId}")
	public ResponseEntity<List<Map<String, Object>>> getReviewsByProduct(@PathVariable Integer productId) {
		List<Review> reviews = reviewService.getReviewsByProduct(productId);
		List<Map<String, Object>> result = reviews.stream().map(review -> {
			Map<String, Object> map = new HashMap<>();
			map.put("reviewId", review.getReviewId());
			map.put("userName", review.getUser() != null ? review.getUser().getUserName() : "匿名用戶");
			map.put("content", review.getReviewContent());
			map.put("rating", review.getRating().intValue());

			// ✅ 加入留言時間（假設你有 createdAt 欄位）
			map.put("createdAt", review.getCreatedAt());

			// ✅ 加入賣家回覆資訊（假設你有 replyContent 和 replyTime）
			map.put("replyContent", review.getReplyContent());
			map.put("replyTime", review.getReplyTime());

			return map;
		}).toList();

		return result.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(result);
//        return reviews.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(reviews);
	}

	/**
	 * 透過店舖 ID 查詢評價
	 */
	// 測試完畢
	@GetMapping("/shop/{shopId}")
	public ResponseEntity<?> getReviewsByShop(@PathVariable Integer shopId) {
		try {
			System.out.println("📌 API 接收到 shopId: " + shopId);
			List<Review> reviews = reviewService.getReviewsByShop(shopId);
			System.out.println("📌 查詢結果：" + reviews.size() + " 筆評價");
			return reviews.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(reviews);
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("伺服器內部錯誤：" + e.getMessage());
		}
	}

	// 賣家回復評價功能
	@PutMapping("/{reviewId}/reply")
	public ResponseEntity<?> replyToReview(@PathVariable Integer reviewId, @RequestBody Map<String, String> body) {

		String replyContent = body.get("replyContent");
		if (replyContent == null || replyContent.trim().isEmpty()) {
			return ResponseEntity.badRequest().body("回覆內容不可為空");
		}

		Review updated = reviewService.replyToReview(reviewId, replyContent);
		return ResponseEntity.ok(updated);
	}

	/**
	 * 透過使用者 ID 查詢評價
	 */
	// 測試完畢
	@GetMapping("/user/{userId}")
	public ResponseEntity<List<Review>> getReviewsByUser(@PathVariable Integer userId) {
		List<Review> reviews = reviewService.getReviewsByUser(userId);
		return reviews.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(reviews);
	}

	/**
	 * 更新評價內容與評分 (買家可用)
	 */
	// 測試完畢
	@PutMapping("/{reviewId}")
	public ResponseEntity<Review> updateReview(@PathVariable Integer reviewId, @Valid @RequestBody Review updatedData) {
		Review updatedReview = reviewService.updateReview(reviewId, updatedData);
		if (updatedReview == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Review not found");
		}
		return ResponseEntity.ok(updatedReview);
	}

	/**
	 * 刪除評價 (管理員/買家)
	 */
	// 測試完畢
	@DeleteMapping("/{reviewId}")
	public ResponseEntity<Map<String, String>> deleteReview(@PathVariable Integer reviewId) {
		boolean deleted = reviewService.deleteReview(reviewId);
		if (!deleted) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Review not found");
		}
		return ResponseEntity.ok(Map.of("message", "刪除成功"));
	}
    
    @PatchMapping("/{reviewId}/status")
    public ResponseEntity<Review> updateReviewStatus(@PathVariable Integer reviewId, @RequestParam String status) {
        try {
            Review.Status reviewStatus = Review.Status.valueOf(status.toUpperCase());
            Review updatedReview = reviewService.updateReviewStatus(reviewId, reviewStatus);
            return ResponseEntity.ok(updatedReview);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status value");
        }
    }

	@GetMapping("/summary/product/{productId}")
	public ReviewSummaryDTO getReviewSummary(@PathVariable Integer productId) {
		return reviewService.getReviewSummaryByProductId(productId);
	}

	/**
	 * (管理員) 更新評價狀態 (審核)
	 */
	// 待測試討論
	// @PatchMapping("/{reviewId}/status")
	// public ResponseEntity<Review> updateReviewStatus(@PathVariable Integer
	// reviewId, @RequestParam String status) {
	// try {
	// Review.Status reviewStatus = Review.Status.valueOf(status.toUpperCase()); //
	// ✅ 轉換字串到 Enum
	// Review updatedReview = reviewService.updateReviewStatus(reviewId,
	// reviewStatus);
	// return ResponseEntity.ok(updatedReview);
	// } catch (IllegalArgumentException e) {
	// throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status
	// value");
	// }
	// }
}