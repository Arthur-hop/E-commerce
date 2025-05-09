package ourpkg.shop;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@AllArgsConstructor
@NoArgsConstructor	
public class ShopListResponse {
	private boolean success;
    private String message;
    private List<ShopDTO> data;
}
