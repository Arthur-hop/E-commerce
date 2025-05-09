package ourpkg.user_role_permission.user.dto;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import ourpkg.address.AddressTypeCorrespond;
import ourpkg.address.dto.UserAddressDTO;
import ourpkg.user_role_permission.user.User;

@Data
@ToString
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserDTO {

	private Integer userId;

	private String userName;

	private String email;

	private String phone;

	private List<UserAddressDTO> userAddresses;

	private Integer shopId; // 新增 shopId 欄位

	public UserDTO(User user) {
		this.userId = user.getUserId();
		this.userName = user.getUsername();
		this.email = user.getEmail();
		this.phone = user.getPhone();
		this.userAddresses = user.getUserAddress().stream().map(UserAddressDTO::new).collect(Collectors.toList());
		if (user.getShop() != null) {
			this.shopId = user.getShop().getShopId(); // 從 User 實體中獲取 shopId
		}
	}

	public UserDTO(Integer userId2, String email2, String phone2, List<UserAddressDTO> userAddressDTOs) {
	}

}