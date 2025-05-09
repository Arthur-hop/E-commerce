package ourpkg.user_role_permission;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.security.core.GrantedAuthority;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import ourpkg.user_role_permission.user.User;

@Entity
@Table(name = "[Role]")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Role implements GrantedAuthority {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
    public String getAuthority() {
        return roleName; // Spring Security 會用這個來檢查權限
    }

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "[id]")
	private Integer id;

	@Column(name = "[role_name]", nullable = false, unique = true)
	private String roleName;

	@ManyToMany(mappedBy = "role")
	@JsonIgnore
	private List<User> user;

	@ManyToMany
	@JoinTable(name = "[Role_Permission]", joinColumns = @JoinColumn(name = "[role_id]"), inverseJoinColumns = @JoinColumn(name = "[permission_id]"))
	private Set<Permission> permission = new HashSet<>();

}
