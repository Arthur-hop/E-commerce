package ourpkg.user_role_permission;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminProfileUpdateRequest {
    private String userName;
    private String email;
    private String phone;
    private String profilePhotoUrl; // 添加頭像 URL 欄位

}