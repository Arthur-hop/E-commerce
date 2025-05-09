package ourpkg.user_role_permission.user.service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import ourpkg.auth.SignUpResponse;
import ourpkg.shop.Shop;
import ourpkg.user_role_permission.AdminUserDTO;
import ourpkg.user_role_permission.Role;
import ourpkg.user_role_permission.RoleRepository;
import ourpkg.user_role_permission.RoleService;
import ourpkg.user_role_permission.user.User;
import ourpkg.user_role_permission.user.UserRepository;
import ourpkg.user_role_permission.user.dto.UserProfileDTO;
import ourpkg.user_role_permission.user.dto.UserProfilePhotoDTO;

@Service
@Transactional
public class UserService {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private RoleRepository roleRepository;

	@Autowired
	private RoleService roleService;

	public List<User> getAllUsers() {
		return userRepository.findAll();
	}

	public Optional<User> getUserById(Integer id) {
		return userRepository.findByUserId(id);
	}

	public Optional<User> findByUsername(String username) {
		return userRepository.findByUserName(username);
	}

	public User createUser(User user) {
		return userRepository.save(user);
	}

	public User updateUser(Integer id, User userDetail) {
		return userRepository.findByUserId(id).map(user -> {
			String encoded = passwordEncoder.encode(userDetail.getPassword());

			// user.setUserName(userDetail.getUserName());
			if (userDetail.getUsername() != null) {
				user.setUserName(userDetail.getUsername());
			}
			user.setPassword(encoded);
			user.setEmail(userDetail.getEmail());
			user.setPhone(userDetail.getPhone());

			return userRepository.save(user);
		}).orElseThrow(() -> new RuntimeException("User not found"));
	}

	public User getUserById2(Integer userId) {
		return userRepository.findById(userId).orElse(null);
	}

	public void saveUser(User user) {
		userRepository.save(user);

	}

	public boolean deleteUser(Integer id) {
		if (id != null) {
			Optional<User> optional = userRepository.findById(id);
			if (optional.isPresent()) {
				userRepository.deleteByUserId(id);
				return true;
			}
		}
		return false;
	}

	public User getCurrentUser() {
		String username = ((UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal())
				.getUsername();
		return userRepository.findByUserName(username).orElseThrow(() -> new RuntimeException("使用者不存在"));
	}

	public boolean changePassword(Integer userId, String currentPassword, String newPassword) {
		Optional<User> userOpt = userRepository.findById(userId);

		if (userOpt.isEmpty()) {
			return false;
		}

		User user = userOpt.get();

		// 檢查舊密碼是否正確
		if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
			return false;
		}

		// 設定新密碼
		user.setPassword(passwordEncoder.encode(newPassword));
		userRepository.save(user);
		return true;
	}
	
	public UserProfileDTO getUserProfileById(Integer userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "找不到該用戶"));

        return new UserProfileDTO(user);
    }

	// ----------------------------------------------------------------------------------
	// Email重設密碼相關
	public User resetPassword(User user) {
		return userRepository.save(user);
	}

	// admin管理admin相關
	public Page<AdminUserDTO> getUsersByRoleAndName(String roleName, String userName, Pageable pageable) {
	    Page<User> users;

	    if (userName == null || userName.trim().isEmpty()) {
	        users = userRepository.findByRoleRoleName(roleName, pageable);
	    } else {
	        users = userRepository.findByRoleRoleNameAndUserNameContaining(roleName, userName, pageable);
	    }

	    return users.map(user -> {
	        Boolean shopIsActive = Optional.ofNullable(user.getShop()).map(Shop::getIsActive).orElse(null);

	        return new AdminUserDTO(
	            user.getUserId(), 
	            user.getUserName(), 
	            user.getEmail(), 
	            user.getPhone(),
	            user.getRole().stream().map(Role::getRoleName).collect(Collectors.toList()), 
	            shopIsActive,
	            user.getStatus() // Pass the user status to the DTO constructor
	        );
	    });
	}

	public SignUpResponse addAnyAdmin(String username, String email, String password, String phone,
			List<String> roles) {
		// 檢查 email 是否已被註冊
		if (userRepository.findByEmail(email).isPresent()) {
			return new SignUpResponse(false, "Email已被註冊");
		}

		// 檢查 userName 是否已被使用
		if (userRepository.findByUserName(username).isPresent()) {
			return new SignUpResponse(false, "此用戶名稱已被使用");
		}

		// 檢查 phone 是否已被使用
		if (userRepository.findByPhone(phone).isPresent()) {
			return new SignUpResponse(false, "此電話號碼已被使用");
		}

		Set<Role> rolesConverted = new HashSet<>(roleRepository.findByRoleNameIn(roles));
		if (roles.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "角色不存在");
		}

		User newUser = new User();
		newUser.setUserName(username);
		newUser.setEmail(email);
		newUser.setPhone(phone);

		String encodedPassword = passwordEncoder.encode(password);
		newUser.setPassword(encodedPassword);

		newUser.setRole(rolesConverted);

		userRepository.save(newUser);

		return new SignUpResponse(true, "新增成功");
	}

	// 以下為admin管理使用者相關api
	public SignUpResponse addAnyUser(String username, String email, String password, String phone) {
		// 檢查 email 是否已被註冊
		if (userRepository.findByEmail(email).isPresent()) {
			return new SignUpResponse(false, "Email已被註冊");
		}

		// 檢查 userName 是否已被使用
		if (userRepository.findByUserName(username).isPresent()) {
			return new SignUpResponse(false, "此用戶名稱已被使用");
		}

		// 檢查 phone 是否已被使用
		if (userRepository.findByPhone(phone).isPresent()) {
			return new SignUpResponse(false, "此電話號碼已被使用");
		}

		User newUser = new User();
		newUser.setUserName(username);
		newUser.setEmail(email);
		newUser.setPhone(phone);

		String encodedPassword = passwordEncoder.encode(password);
		newUser.setPassword(encodedPassword);

		Role defaultRole = roleService.findByRoleName("user");

		if (defaultRole == null) {
			return new SignUpResponse(false, "新增失敗，user角色不存在");
		}

		// 初始化role值避免null
		if (newUser.getRole() == null) {
			newUser.setRole(new HashSet<>());
		}

		newUser.getRole().add(defaultRole);
		userRepository.save(newUser);

		return new SignUpResponse(true, "新增成功");
	}

	public boolean updateAnyUser(Integer id, User edited) {
		Optional<User> optional = userRepository.findByUserId(id);

		if (!optional.isPresent()) {
			return false;
		}

		User user = optional.get();
		user.setUserName(edited.getUserName());
		user.setEmail(edited.getEmail());
		user.setPhone(edited.getPhone());

		userRepository.save(user);

		return true;

	}

	public boolean deleteAnyUser(Integer id) {
		if (id == null) {
			return false;
		}

		Optional<User> optional = userRepository.findById(id);
		if (!optional.isPresent()) {
			return false;
		}

		userRepository.deleteByUserId(id);
		return true;

	}

	// shop會用到 3/24新增
	public Integer getUserIdByUsername(String username) {
		return userRepository.findByUserName(username).map(User::getUserId) // 假設 User 實體的主鍵是 `id`
				.orElse(null);
	}

	public Integer getCurrentUserId() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		System.out.println("🔍 目前的身份驗證資訊：" + authentication);

		if (authentication == null || !authentication.isAuthenticated()) {
			throw new UsernameNotFoundException("User not found");
		}

		String username = authentication.getName();
		System.out.println("✅ 解析出的 Username：" + username);

		return userRepository.findByUserName(username).map(User::getUserId)
				.orElseThrow(() -> new UsernameNotFoundException("User not found"));
	}

	public Optional<User> findAnyByEmail(String email) {
		return userRepository.findByEmail(email);
	}
	// 4/3新增 管理user status
	public boolean updateUserStatus(Integer userId, String statusString) {
	    Optional<User> userOpt = userRepository.findById(userId);
	    
	    if (userOpt.isEmpty()) {
	        return false;
	    }
	    
	    User user = userOpt.get();
	    User.UserStatus newStatus;
	    
	    try {
	        newStatus = User.UserStatus.valueOf(statusString);
	    } catch (IllegalArgumentException e) {
	        return false;  // 無效的狀態值
	    }
	    
	    user.setStatus(newStatus);
	    userRepository.save(user);
	    return true;
	}
	
	   /**
     * 檢查指定的電子郵件是否已被註冊
     * 
     * @param email 要檢查的電子郵件
     * @return 如果電子郵件已被使用則返回 true，否則返回 false
     */
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
    
    /**
     * 更新管理員資料
     * 
     * @param admin 要更新的管理員對象
     * @return 更新後的管理員對象
     */
    @Transactional
    public User updateAdmin(User admin) {
        if (admin == null || admin.getUserId() == null) {
            throw new IllegalArgumentException("管理員或其ID不能為空");
        }
        
        // 檢查管理員是否存在
        User existingAdmin = userRepository.findById(admin.getUserId())
            .orElseThrow(() -> new IllegalArgumentException("找不到要更新的管理員: " + admin.getUserId()));
        
        // 檢查是否是管理員角色
        boolean isAdmin = existingAdmin.getRole().stream()
            .anyMatch(role -> role.getRoleName().equals("ADMIN") || role.getRoleName().equals("SUPER_ADMIN"));
            
        if (!isAdmin) {
            throw new IllegalArgumentException("該用戶不是管理員, 不能使用此方法更新");
        }
        
        // 更新欄位 (只更新允許的欄位)
        existingAdmin.setUserName(admin.getUserName());
        existingAdmin.setEmail(admin.getEmail());
        existingAdmin.setPhone(admin.getPhone());
        
        // 如果有更新密碼
        if (admin.getPassword() != null && !admin.getPassword().isEmpty()) {
            existingAdmin.setPassword(admin.getPassword());
        }
        
        // 保存並返回更新後的對象
        return userRepository.save(existingAdmin);
    }
    
    /**
     * 根據用戶名查找用戶
     * 
     * @param userName 用戶名
     * @return 包含用戶的 Optional 對象
     */
    public Optional<User> findByUserName(String userName) {
        return userRepository.findByUserName(userName);
    }

	// --------------------------------------
	// 以下為確保該商店屬於該商家(user)邏輯

	public boolean isShopOwnedBySeller(Shop shop, User user) {
		if (shop == null || user == null) {
			return false;
		}
		// 假設 Shop 裡面有 getUser()，而且 userId 就是賣家的 id
		if (shop.getUser() == null) {
			return false;
		}
		return shop.getUser().getUserId().equals(user.getUserId());
	}

	// 新增：根據 userId 查詢用戶
	public Optional<User> findById(Integer userId) {
		return userRepository.findById(userId);
	}

	// 新增：根據 userId 查詢用戶並包含 Shop 資訊
	public Optional<User> findByIdWithShop(Integer userId) {
		return userRepository.findById(userId).map(user -> {
			if (user.getShop() != null) {
				user.getShop().getShopId(); // 觸發 Lazy 加載
			}
			return user;
		});
	}
	
	//-----------------------
	// 在 UserService 中
	public String getUserProfilePhotoUrl(User user) {
	    if (user.getProfilePhotoUrl() == null || user.getProfilePhotoUrl().isEmpty()) {
	        return "/uploads/default.jpg";
	    }
	    return user.getProfilePhotoUrl();
	}
	
	// 在 UserService 中
	public Map<String, Object> getNewUsersStatistics(int days) {
	    Date endDate = new Date();
	    Calendar calendar = Calendar.getInstance();
	    calendar.setTime(endDate);
	    calendar.add(Calendar.DAY_OF_MONTH, -days);
	    Date startDate = calendar.getTime();
	    
	    // 獲取日期範圍內每天的新用戶數量
	    List<Object[]> dailyStats = userRepository.countNewUsersByDateRange(startDate, endDate);
	    
	    // 獲取總用戶數 - 這裡需要修改
	    Integer totalNewUsers = userRepository.countTotalNewUsersByDateRange(startDate, endDate);
	    
	    // 將 Integer 轉換為 Long，如果需要的話
	    Long totalNewUsersLong = totalNewUsers == null ? 0L : totalNewUsers.longValue();
	    
	    // 轉換為前端所需格式
	    List<String> labels = new ArrayList<>();
	    List<Long> data = new ArrayList<>();
	    
	    SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd");
	    
	    for (Object[] stat : dailyStats) {
	        Date date = (Date) stat[0];
	        Long count = ((Number) stat[1]).longValue(); // 使用 Number 進行安全轉換
	        
	        labels.add(dateFormat.format(date));
	        data.add(count);
	    }
	    
	    Map<String, Object> result = new HashMap<>();
	    result.put("labels", labels);
	    result.put("data", data);
	    result.put("total", totalNewUsersLong); // 使用轉換後的值
	    
	    return result;
	}
	
	@Value("${upload.avatar-dir:uploads/profile}")
    private String uploadDir;
	
	public UserProfilePhotoDTO saveUserAvatar(User user, MultipartFile file) throws IOException {
	    if (file.isEmpty()) throw new IOException("空的檔案");

	    File uploadPath = new File(System.getProperty("user.dir"), uploadDir);
	    if (!uploadPath.exists()) uploadPath.mkdirs();

	    String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
	    File destFile = new File(uploadPath, fileName);
	    file.transferTo(destFile);

	    String profilePhotoUrl = "/" + uploadDir + "/" + fileName;

	    // ✅ 只更新這個欄位
	    userRepository.updateUserProfilePhoto(user.getUserId(), profilePhotoUrl);

	    return new UserProfilePhotoDTO(profilePhotoUrl);
	}
}
