package ourpkg.user_role_permission;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RoleUpdateRequest {
    private List<String> roles;
}
