package ourpkg.user_role_permission.user;

import java.util.List;
import java.util.stream.Collectors;

import ourpkg.address.UserAddress;
import ourpkg.address.dto.AddressTypeCorrespondDTO;
import ourpkg.address.dto.UserAddressDTO;
import ourpkg.user_role_permission.user.dto.UserDTO;

public class UserMapper {
	public static UserDTO toDTO(User user) {
        List<UserAddressDTO> userAddressDTOs = user.getUserAddress()
            .stream()
            .map(UserMapper::mapUserAddress)
            .collect(Collectors.toList());

        return new UserDTO(
            user.getUserId(),
            user.getEmail(),
            user.getPhone(),
            userAddressDTOs
        );
    }

    private static UserAddressDTO mapUserAddress(UserAddress userAddress) {
        AddressTypeCorrespondDTO addressTypeDTO = new AddressTypeCorrespondDTO(
            userAddress.getAddressTypeCorrespond().getId(),
            userAddress.getAddressTypeCorrespond().getName()
        );

        return new UserAddressDTO(
            userAddress.getUserAddressId(),
            userAddress.getCity(),
            userAddress.getDistrict(),
            userAddress.getStreetEtc(),
            userAddress.getZipCode(),
            userAddress.getRecipientName(),
            userAddress.getRecipientPhone()
        );
    }
}
