package ourpkg.review;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductRatingSummaryDTO {
    private BigDecimal averageRating;
    private Long reviewCount;
}
