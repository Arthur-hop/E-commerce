package ourpkg.user_role_permission.user.controller;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.bind.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import ourpkg.shop.SellerShopRepository;
import ourpkg.shop.Shop;
import ourpkg.user_role_permission.UserResponse;
import ourpkg.user_role_permission.user.User;
import ourpkg.user_role_permission.user.UserMapper;
import ourpkg.user_role_permission.user.UserRepository;
import ourpkg.user_role_permission.user.dto.ChangePasswordRequest;
import ourpkg.user_role_permission.user.dto.UserDTO;
import ourpkg.user_role_permission.user.dto.UserProfileDTO;
import ourpkg.user_role_permission.user.dto.UserProfilePhotoDTO;
import ourpkg.user_role_permission.user.service.UserService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user")
// @PreAuthorize("hasAuthority('SUPERADMIN:ALL')")
public class UserController {

	@Autowired
	private UserService userService;

	@Autowired
	private UserRepository userRepository;
	
//新增==============================
	@Autowired
	private SellerShopRepository sellershopRepository;
//新增==============================
	
	@Autowired
	private BCryptPasswordEncoder passwordEncoder;
	
	

//	@Autowired
//    public UserController(UserRepository userRepository) {
//        this.userRepository = userRepository;
//    }
//
//	public UserController(UserService userService) {
//		this.userService = userService;
//	}

	// @GetMapping
	// public ResponseEntity<List<User>> findAllUsers() {
	// return ResponseEntity.ok(userService.getAllUsers());
	// }

	// @GetMapping("/{id}")
	// public ResponseEntity<User> getUserById(@PathVariable Integer id) {
	// return
	// userService.getUserById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
	// }

	@GetMapping("/check/{userId}")
	public ResponseEntity<UserDTO> getUserById(@PathVariable Integer userId) {
	    Optional<User> userOptional = userService.findByIdWithShop(userId); // 使用 findByIdWithShop 方法
	    if (userOptional.isPresent()) {
	        User user = userOptional.get();
	        UserDTO userDTO = new UserDTO();
	        userDTO.setUserId(user.getUserId());
	        userDTO.setUserName(user.getUsername());
	        if (user.getShop() != null) {
	            userDTO.setShopId(user.getShop().getShopId()); // 將 shopId 設定到 DTO 中
	        }
	        return ResponseEntity.ok(userDTO);
	    } else {
	        return ResponseEntity.notFound().build();
	    }
	}

	@PostMapping
	public ResponseEntity<User> createUser(@RequestBody User user) {
		return ResponseEntity.ok(userService.createUser(user));
	}

	@PutMapping(value = "/{id}", consumes = "application/json")
	public ResponseEntity<?> updateUser(@PathVariable Integer id, @RequestBody User user) {
		User updated = userService.updateUser(id, user);
		UserProfileDTO dto = new UserProfileDTO();
		dto.setUserId(updated.getUserId());
		dto.setEmail(updated.getEmail());
		dto.setUserName(updated.getUsername());
		dto.setPhone(updated.getPhone());

		return ResponseEntity.ok(dto);
	}
	
	@PutMapping(value = "/update/{userId}", consumes = {"application/json", "application/json;charset=UTF-8"})
	public ResponseEntity<?> updateUser2(@PathVariable Integer userId, @RequestBody User updateUser) {
	    User existingUser = userService.getUserById2(userId);

	    if (existingUser == null) {
	        return ResponseEntity.notFound().build();
	    }

	    existingUser.setEmail(updateUser.getEmail());
	    existingUser.setPhone(updateUser.getPhone());
	    existingUser.setUserName(updateUser.getUserName());

	    userService.saveUser(existingUser);

	    // ✅ 回傳 DTO，避免回傳包含 GrantedAuthority 的 User
	    return ResponseEntity.ok(new UserProfileDTO(existingUser));
	}

	
	@GetMapping("/username/{username}")
	public ResponseEntity<?> getUserByUsername(@PathVariable String username) {
	    Optional<User> user = userService.findByUsername(username);
	    if (user == null) {
	        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
	    }
	    return ResponseEntity.ok(user);
	}

	
	

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteUser(@PathVariable Integer id) {
		userService.deleteUser(id);
		return ResponseEntity.noContent().build();
	}
	
	@PutMapping("/{userId}/change-password")
	public ResponseEntity<?> changePassword(
	        @PathVariable Integer userId,
	        @RequestBody ChangePasswordRequest request) {

		User user = userService.getUserById2(userId);
		if (user == null) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("找不到使用者");
		}

		// 驗證舊密碼是否正確
		if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
			return ResponseEntity.badRequest().body("舊密碼錯誤");
		}

		// 驗證新密碼格式（至少8碼，含大小寫與數字）
	    String newPassword = request.getNewPassword();
	    if (!newPassword.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$")) {
	        return ResponseEntity.badRequest().body("密碼必須至少 8 碼，包含大小寫與數字");
	    }
	    
		// 設定新密碼（加密）
		user.setPassword(passwordEncoder.encode(request.getNewPassword()));
		userService.saveUser(user);

		return ResponseEntity.ok("密碼變更成功");
	
	}
	
	 @GetMapping("/membercenter/id/{userId}")
	    public ResponseEntity<UserProfileDTO> getUserProfile(@PathVariable Integer userId) {
	        UserProfileDTO dto = userService.getUserProfileById(userId);
	        return ResponseEntity.ok(dto);
	    }


	// ----------------------------------------------------------------------------------

	// ----------------------------------------------------------------------------------
	
	@GetMapping("/{id}")
	public ResponseEntity<UserResponse> getUser(@PathVariable Integer id) {
		
		Optional<User> optional = userService.getUserById(id);
		
		if(!optional.isPresent()) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new UserResponse(false, "查無使用者", null));
		}
		
		User user = optional.get();

		return ResponseEntity.status(HttpStatus.OK).body(new UserResponse(true, "刪除成功", new UserDTO(user)));
	}
	
//新增==============================
	@GetMapping("/{userId}/shop")
	public ResponseEntity<Map<String, Integer>> getUserShopId(@PathVariable Integer userId) {
		Optional<Shop> shop = sellershopRepository.findByUserUserId(userId);
	    Map<String, Integer> response = new HashMap<>();
	    
	    if (shop != null) {
	        response.put("shopId", shop.get().getShopId());
	        return ResponseEntity.ok(response);
	    } else {
	        return ResponseEntity.notFound().build();
	    }
	}
//新增==============================
	
	@GetMapping("/profile")
	public ResponseEntity<?> getUserProfile(Authentication authentication) {
	    User user = (User) authentication.getPrincipal();
	    
	    Map<String, Object> response = new HashMap<>();
	    response.put("userId", user.getUserId());
	    response.put("userName", user.getUserName());
	    response.put("email", user.getEmail());
	    // 這裡調用 getUserProfilePhotoUrl 方法獲取頭像 URL
	    response.put("profilePhoto", userService.getUserProfilePhotoUrl(user));
	    
	    return ResponseEntity.ok(response);
	}
	
	@PostMapping("/upload-avatar")
	public ResponseEntity<UserProfilePhotoDTO> uploadAvatar(
	        @RequestParam("file") MultipartFile file,
	        Authentication authentication) throws IOException {

	    if (file.isEmpty()) {
	        return ResponseEntity.badRequest().build();
	    }

	    // 取得當前登入使用者
	    User user = (User) authentication.getPrincipal();
	    
	    UserProfilePhotoDTO dto = userService.saveUserAvatar(user, file);
	    return ResponseEntity.ok(dto);
	}



}
