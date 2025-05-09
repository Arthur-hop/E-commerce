package ourpkg.user_role_permission;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminCreateRequest {
    private String userName;
    
    private String password;

    private String email;

    private String phone;

    private List<String> roles; // 接收多個角色
}
