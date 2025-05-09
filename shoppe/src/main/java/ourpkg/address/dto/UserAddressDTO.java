package ourpkg.address.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import ourpkg.address.UserAddress;
import ourpkg.user_role_permission.user.dto.UserDTO;

@Data
@ToString
@Getter
@Setter
@NoArgsConstructor
public class UserAddressDTO {

	private Integer userAddressId;
    private String city;
    private String district;
    private String streetEtc;
    private String zipCode;
    private String recipientName;
    private String recipientPhone;
    private AddressTypeCorrespondDTO addressType;
    
    public UserAddressDTO(UserAddress userAddress) {
        this.userAddressId = userAddress.getUserAddressId();
        this.city = userAddress.getCity();
        this.district = userAddress.getDistrict();
        this.streetEtc = userAddress.getStreetEtc();
        this.zipCode = userAddress.getZipCode();
        this.recipientName = userAddress.getRecipientName();
        this.recipientPhone = userAddress.getRecipientPhone();
    }

    public UserAddressDTO(Integer userAddressId, String city, String district, String streetEtc, String zipCode, String recipientName, String recipientPhone) {
        this.userAddressId = userAddressId;
        this.city = city;
        this.district = district;
        this.streetEtc = streetEtc;
        this.zipCode = zipCode;
        this.recipientName = recipientName;
        this.recipientPhone = recipientPhone;
    }

    public UserAddressDTO(String city, String district, String zipCode, String streetEtc, String recipientName, String recipientPhone, Integer userAddressId) {
        this.city = city;
        this.district = district;
        this.zipCode = zipCode;
        this.streetEtc = streetEtc;
        this.recipientName = recipientName;
        this.recipientPhone = recipientPhone;
        this.userAddressId = userAddressId;
    }

}
