package ourpkg.user_role_permission;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "[Permission]")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Permission {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "[id]")
	private Integer id;

	@Column(name = "[permission_name]", nullable = false, unique = true)
	private String permissionName;
	
	@ManyToMany(mappedBy = "permission")
	private Set<Role> role = new HashSet<>();
	
}
