package ourpkg.user_role_permission;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

//用於獲取管理員資料的 DTO
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminProfileDTO {
    private Integer userId;
    private String userName;
    private String email;
    private String phone;
    private List<String> roles; // 新增角色列表
    private String profilePhotoUrl; // 添加頭像 URL 欄位

}