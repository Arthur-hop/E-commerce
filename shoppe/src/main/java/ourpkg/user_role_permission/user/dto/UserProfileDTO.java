package ourpkg.user_role_permission.user.dto;

import java.util.List;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import ourpkg.address.dto.UserAddressDTO;
import ourpkg.user_role_permission.user.User;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserProfileDTO {

	private Integer userId;

	private String userName;

	private String email;

	private String phone;

	private List<UserAddressDTO> userAddresses;

	public UserProfileDTO(User user) {
		this.userId = user.getUserId();
		this.userName = user.getUsername();
		this.email = user.getEmail();
		this.phone = user.getPhone();
		this.userAddresses = user.getUserAddress().stream().map(UserAddressDTO::new).collect(Collectors.toList());

	}

	public UserProfileDTO(Integer userId2, String email2, String phone2, List<UserAddressDTO> userAddressDTOs) {
	}

}