package ourpkg.user_role_permission;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

@Entity
@Table(name = "User_Role")
@IdClass(UserRoleId.class)
public class UserRole {
	@Id
    @Column(name = "user_id")
    private Integer userId;

    @Id
    @Column(name = "role_id")
    private Integer roleId;
}
