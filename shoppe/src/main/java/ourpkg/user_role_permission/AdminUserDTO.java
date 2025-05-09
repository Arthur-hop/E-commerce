package ourpkg.user_role_permission;

import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ourpkg.user_role_permission.user.User.UserStatus;

@Getter
@Setter
@NoArgsConstructor
public class AdminUserDTO {
    private Integer userId;
    private String userName;
    private String email;
    private String phone;
    List<String> roles;
    private Boolean shopIsActive;
    private UserStatus status; // Added status field


    // 透過 User 實體建構 DTO
    public AdminUserDTO(Integer id, String userName, String email, String phone, List<String> roles, Boolean shopIsActive, UserStatus status) {    	  
    	  this.userId = id;
          this.userName = userName;
          this.email = email;
          this.phone = phone;
          this.roles = roles;
          this.shopIsActive = shopIsActive;
          this.status = status;
    }
}
