package ourpkg.user_role_permission;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminProfilePhotoUpdateRequest {
    private String profilePhotoUrl;
}
