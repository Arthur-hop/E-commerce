package ourpkg.review;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReviewRequest {
    private Integer productId;
    private Integer orderItemId; // 新增這行
    private String content;
    private Integer rating;
}
