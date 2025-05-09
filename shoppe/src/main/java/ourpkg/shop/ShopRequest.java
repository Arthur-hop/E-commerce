package ourpkg.shop;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ShopRequest {

	private boolean success;
	
	private String message;
	
	private ShopDTO shopDTO;
	
	
}
