package ourpkg.product.review;

public class ReviewStatusRequest {
    
    private Boolean reviewStatus;
    private String reviewComment;
    private Integer adminId;
    
    public Boolean getReviewStatus() {
        return reviewStatus;
    }
    
    public void setReviewStatus(Boolean reviewStatus) {
        this.reviewStatus = reviewStatus;
    }
    
    public String getReviewComment() {
        return reviewComment;
    }
    
    public void setReviewComment(String reviewComment) {
        this.reviewComment = reviewComment;
    }
    
    public Integer getAdminId() {
        return adminId;
    }
    
    public void setAdminId(Integer adminId) {
        this.adminId = adminId;
    }
}
