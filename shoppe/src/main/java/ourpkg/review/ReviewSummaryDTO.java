package ourpkg.review;

public class ReviewSummaryDTO {
    private int totalReviews;
    private double averageRating;

    public ReviewSummaryDTO(int totalReviews, double averageRating) {
        this.totalReviews = totalReviews;
        this.averageRating = averageRating;
    }

	public int getTotalReviews() {
		return totalReviews;
	}

	public void setTotalReviews(int totalReviews) {
		this.totalReviews = totalReviews;
	}

	public double getAverageRating() {
		return averageRating;
	}

	public void setAverageRating(double averageRating) {
		this.averageRating = averageRating;
	}

    // getters & setters
}
