package ourpkg.review;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import ourpkg.order.OrderItem;
import ourpkg.order.OrderItemRepository;
import ourpkg.product.Product;
import ourpkg.product.ProductRepository;
import ourpkg.product.ProductShopRepository;
import ourpkg.shop.Shop;
import ourpkg.user_role_permission.user.User;
import ourpkg.user_role_permission.user.UserRepository;
import ourpkg.user_role_permission.user.service.UserService;

@Service
public class ReviewService {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductShopRepository productshopRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private UserService UserService;

    /**
     * 新增評價（舊版）
     */
    public Review createReview(Integer userId, Integer shopId, Integer productId, Review reviewData) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Shop shop = productshopRepository.findById(shopId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Shop not found"));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));

        reviewData.setUser(user);
        reviewData.setShop(shop);
        reviewData.setProduct(product);

        return reviewRepository.save(reviewData);
    }

    /**
     * 用 ReviewRequest 建立評論（推薦做法）
     */
    public Review createReviewFromRequest(Integer userId, ReviewRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));

        Shop shop = product.getShop();
        if (shop == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product has no associated shop");
        }

        // 🔍 查詢 orderItem
        OrderItem orderItem = orderItemRepository.findById(request.getOrderItemId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "OrderItem not found"));

        Review review = new Review();
        review.setUser(user);
        review.setProduct(product);
        review.setShop(shop);
        review.setProductName(product.getProductName());
        review.setReviewContent(request.getContent());
        review.setRating(BigDecimal.valueOf(request.getRating()));
        review.setOrderItem(orderItem);
        review.setOrderItemId(request.getOrderItemId());

        return reviewRepository.save(review);
    }

    public List<Review> getAllReviews() {
        return reviewRepository.findAll();
    }

    public Review getReviewById(Integer reviewId) {
        return reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Review not found"));
    }

    public List<Review> getReviewsByProductName(String productName) {
        return reviewRepository.findByProductName(productName);
    }

    public List<Review> getReviewsByProduct(Integer productId) {
        return reviewRepository.findByProductId(productId);
    }

    public List<Review> getReviewsByShop(Integer shopId) {
        List<Review> reviews = reviewRepository.findByShopId(shopId);
        if (reviews.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No reviews found for shopId: " + shopId);
        }
        return reviews;
    }

    public List<Review> getReviewsByUser(Integer userId) {
        return reviewRepository.findByUserId(userId);
    }

    public Review updateReview(Integer reviewId, Review updatedData) {
        Review existingReview = getReviewById(reviewId);

        existingReview.setReviewContent(updatedData.getReviewContent());
        existingReview.setRating(updatedData.getRating());

        return reviewRepository.save(existingReview);
    }

    public boolean deleteReview(Integer reviewId) {
        Review review = getReviewById(reviewId);
        reviewRepository.delete(review);
        return true;
    }

    public Review updateReviewStatus(Integer reviewId, Review.Status status) {
        Review review = getReviewById(reviewId);
        review.setStatus(status);
        return reviewRepository.save(review);
    }

    public ReviewSummaryDTO getReviewSummaryByProductId(Integer productId) {
        List<Review> reviews = reviewRepository.findByProductId(productId);
        if (reviews.isEmpty()) {
            return new ReviewSummaryDTO(0, 0.0);
        }

        int count = reviews.size();
        double avgRating = reviews.stream()
                .mapToDouble(r -> r.getRating().doubleValue())
                .average()
                .orElse(0.0);

        return new ReviewSummaryDTO(count, avgRating);
    }
    
    //賣家回復評價方法
    public Review replyToReview(Integer reviewId, String replyContent) {
        Review review = reviewRepository.findById(reviewId)
            .orElseThrow(() -> new RuntimeException("找不到評論"));

        review.setReplyContent(replyContent);
        review.setReplyTime(LocalDateTime.now());
        return reviewRepository.save(review);
    }

}
