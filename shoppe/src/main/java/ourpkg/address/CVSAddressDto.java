package ourpkg.address;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class CVSAddressDto {

	 private Integer userId;
	    private Integer addressTypeId;
	    private Boolean isDefault;
	    private String city;
	    private String district;
	    private String zipCode;
	    private String streetEtc;
	    private String recipientName; // ✅ 一定要有
	    private String recipientPhone; // ✅ 一定要有
}
