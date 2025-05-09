package ourpkg.review;

public class ReviewDto {
    private int reviewId;
    private int productId;
    private String productName;
    private String userName;
    private String content;
    private int rating;
    private String status;
    private String createdAt;

    public ReviewDto(int reviewId, int productId, String productName, String userName,
                     String content, int rating, String status, String createdAt) {
        this.reviewId = reviewId;
        this.productId = productId;
        this.productName = productName;
        this.userName = userName;
        this.content = content;
        this.rating = rating;
        this.status = status;
        this.createdAt = createdAt;
    }

    public int getReviewId() {
        return reviewId;
    }

    public void setReviewId(int reviewId) {
        this.reviewId = reviewId;
    }

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
