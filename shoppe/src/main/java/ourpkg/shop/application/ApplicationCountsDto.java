package ourpkg.shop.application;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ApplicationCountsDto {
    private long pending;
    private long approved;
    private long rejected;
}
