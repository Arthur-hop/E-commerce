package ourpkg.address.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import ourpkg.address.AddressTypeCorrespond;
import ourpkg.user_role_permission.user.dto.UserDTO;

@Data
@ToString
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AddressTypeCorrespondDTO {

	 private Integer id;
	    private String name;

        public AddressTypeCorrespondDTO(AddressTypeCorrespond addressTypeCorrespond) {
            this.id = addressTypeCorrespond.getId();
            this.name = addressTypeCorrespond.getName();
        }
}
