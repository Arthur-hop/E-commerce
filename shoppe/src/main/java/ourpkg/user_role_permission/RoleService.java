package ourpkg.user_role_permission;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ourpkg.product.ProductShopRepository;
import ourpkg.shop.Shop;
import ourpkg.user_role_permission.user.User;
import ourpkg.user_role_permission.user.UserRepository;

@Service
public class RoleService {

	@Autowired
	private RoleRepository roleRepository;
	
	@Autowired
	private UserRepository userRepository;
	
	public void updateAdminRoles(Integer userId, List<String> roleNames) {
		Optional<User> op = userRepository.findByUserId(userId);
		User user = op.get();
        // 查詢角色物件
        List<Role> roles = roleRepository.findByRoleNameIn(roleNames);
        
        // 更新使用者角色
        user.setRole(new HashSet<>(roles));
        userRepository.save(user);
    }

	public Role insertRole(String name) {
		Role role = new Role();
		role.setRoleName(name);

		return roleRepository.save(role);
	}

	public List<Role> findAllRole() {
		return roleRepository.findAll();
	}

	public Role findRoleById(Integer id) {
		Optional<Role> op = roleRepository.findById(id);

		if (op.isPresent()) {
			return op.get();
		}

		return null;
	}
	
	public Role findByRoleName(String roleName) {
		Optional<Role> op = roleRepository.findByRoleName(roleName);

		if (op.isPresent()) {
			return op.get();
		}

		return null;
	}

	public Role updateRole(Role role) {
		return roleRepository.save(role);
	}

	public void deleteRoleById(Integer id) {
		roleRepository.deleteById(id);
	}
	
    @Autowired
    private ProductShopRepository ProductshopRepository; 
    
    public void updateUserRoles(Integer userId, List<String> roleNames) {
        Optional<User> op = userRepository.findByUserId(userId);
        if (!op.isPresent()) {
            throw new RuntimeException("用戶不存在");
        }
        User user = op.get();

        // 查詢角色物件
        List<Role> roles = roleRepository.findByRoleNameIn(roleNames);
        if (roles.isEmpty()) {
            throw new RuntimeException("角色名稱不正確");
        }

        // 查詢該用戶是否有商店
        Optional<Shop> opShop = ProductshopRepository.findByUserUserId(user.getUserId());

        if (opShop.isPresent()) {
            Shop shop = opShop.get();
            
            // **如果角色包含 SELLER，則啟用商店；如果沒有 SELLER，則停用**
            boolean isSeller = roleNames.contains("SELLER");
            
            // **只有當 isActive 狀態變更時，才更新資料庫**
            if (shop.getIsActive() != isSeller) {
                shop.setIsActive(isSeller);
                ProductshopRepository.save(shop);
            }
        }

        // 更新使用者角色
        user.setRole(new HashSet<>(roles));
        userRepository.save(user);
    }
    
}
