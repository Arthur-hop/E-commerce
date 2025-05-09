package ourpkg.user_role_permission;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoleRepository extends JpaRepository<Role, Integer> {
	
	public Optional<Role> findByRoleName(String roleName);

	@Query("SELECT r FROM Role r WHERE r.roleName IN :roleNames")
	List<Role> findByRoleNameIn(@Param("roleNames") List<String> roleNames);
}
