package ourpkg.user_role_permission;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.persistence.EntityNotFoundException;
import ourpkg.auth.SignUpRequest;
import ourpkg.auth.SignUpResponse;
import ourpkg.auth.util.SignUpValidateUtil;
import ourpkg.user_role_permission.user.User;
import ourpkg.user_role_permission.user.dto.UserDTO;
import ourpkg.user_role_permission.user.service.UserService;

@RestController
@RequestMapping("/api/admin")
public class AdminUserController {

	@Autowired
	private UserService userService;

	@Autowired
	private RoleService roleService;

	// Sa管理admin相關api-----------------------------------------------------------------

	@PreAuthorize("hasAnyRole('SUPER_ADMIN')")
	@GetMapping("/any/sa")
	 public Page<AdminUserDTO> getUsersByRoleAndName(
	            @RequestParam String roleName,
	            @RequestParam(required = false) String userName,
	            Pageable pageable) {
	        return userService.getUsersByRoleAndName(roleName, userName, pageable);
	    }

	@PreAuthorize("hasAnyRole('SUPER_ADMIN')")
	@PostMapping("/any")
	public ResponseEntity<SignUpResponse> createAdmin(@RequestBody AdminCreateRequest request) {

		String userName = request.getUserName();
		String password = request.getPassword();
		String email = request.getEmail();
		String phone = request.getPhone();
		
		
		List<String> roles = request.getRoles();

		SignUpResponse response = userService.addAnyAdmin(userName, email, password, phone, roles);
		
		if (response.isSuccess()) {
			return ResponseEntity.status(HttpStatus.CREATED) // 201 Created
					.body(response);
		} else {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST) // 或 其他適當的錯誤碼
					.body(response);
		}
		
	}

	// 管理角色相關-------------------------------------------------------------------------
	@PreAuthorize("hasAnyRole('SUPER_ADMIN')")
	@GetMapping("/role/all")
	public ResponseEntity<List<RoleDTO>> getAllRoles() {
		List<Role> roles = roleService.findAllRole(); // 獲取所有 Role 實體

		// 將 List<Role> 轉換為 List<RoleDTO> (假設您有一個 RoleDTO)
		List<RoleDTO> roleDTOs = roles.stream().map(role -> new RoleDTO(role.getId(), role.getRoleName())) // 假設 RoleDTO
																											// 有這樣的建構子
				.collect(Collectors.toList());

		return ResponseEntity.ok(roleDTOs); // 返回 JSON 格式的 RoleDTO 列表
	}

	@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
	@PutMapping("/role/{userId}")
	public ResponseEntity<SignUpResponse> updateUserRoles(@PathVariable Integer userId, @RequestBody RoleUpdateRequest request) {
		try {
			roleService.updateUserRoles(userId, request.getRoles());
			return ResponseEntity.status(HttpStatus.OK).body(new SignUpResponse(true, "更新成功"));
		} catch (EntityNotFoundException e) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new SignUpResponse(false, "用戶不存在"));
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new SignUpResponse(true, "更新失敗"));
		}
	}
	
	@PreAuthorize("hasAnyRole('SUPER_ADMIN')")
	@PutMapping("/role/sa/{userId}")
	public ResponseEntity<SignUpResponse> updateAdminRoles(@PathVariable Integer userId, @RequestBody RoleUpdateRequest request) {
		try {
			roleService.updateAdminRoles(userId, request.getRoles());
			return ResponseEntity.status(HttpStatus.OK).body(new SignUpResponse(true, "更新成功"));
		} catch (EntityNotFoundException e) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new SignUpResponse(false, "用戶不存在"));
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new SignUpResponse(true, "更新失敗"));
		}
	}

	// --------------------------------------------------------------------------------

	// 以下為admin管理使用者相關api----------------------------------------------------------
	
	@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
	@GetMapping("/any")
	  public Page<AdminUserDTO> getUsersByRoleAndName2(
	            @RequestParam String roleName,
	            @RequestParam(required = false) String userName,
	            Pageable pageable) {
	        return userService.getUsersByRoleAndName(roleName, userName, pageable);
	    }

	@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
	@PostMapping("/user/any")
	public ResponseEntity<SignUpResponse> addAnyUser(@RequestBody SignUpRequest entity) {

		String username = entity.getUsername();
		String password = entity.getPassword();
		String email = entity.getEmail();
		String phone = entity.getPhone();

		if (username == null || username.isBlank()) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new SignUpResponse(false, "請填寫使用者名稱"));
		}

		if (password == null || password.isBlank()) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new SignUpResponse(false, "請填寫密碼"));
		}

		if (email == null || email.isBlank()) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new SignUpResponse(false, "請填寫電E-mail"));
		}

		if (phone == null || phone.isBlank()) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new SignUpResponse(false, "請填寫電話號碼"));
		}

		if (!SignUpValidateUtil.isValidUsername(username)) {
			return ResponseEntity.badRequest().body(new SignUpResponse(false, "名稱長度需介於6-20字，且不得含有非法字元"));
		}

		if (!SignUpValidateUtil.isValidPassword(password)) {
			return ResponseEntity.badRequest().body(new SignUpResponse(false, "密碼須包含至少一個大寫字母，且長度界於6-12個字"));
		}

		if (!SignUpValidateUtil.isValidEmail(email)) {
			return ResponseEntity.badRequest().body(new SignUpResponse(false, "請輸入正確的E-mail"));
		}

		if (!SignUpValidateUtil.isValidTaiwanPhone(phone)) {
			return ResponseEntity.badRequest().body(new SignUpResponse(false, "請輸入正確電話號碼"));
		}

		SignUpResponse response = userService.addAnyUser(username, email, password, phone);

		if (response.isSuccess()) {
			return ResponseEntity.status(HttpStatus.CREATED) // 201 Created
					.body(response);
		} else {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST) // 或 其他適當的錯誤碼
					.body(response);
		}

	}

	@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
	@PutMapping("/user/any/{id}")
	public ResponseEntity<UserResponse> UpdateAnyUser(@PathVariable Integer id, @RequestBody UserRequest entity) {

		if (entity.getUserId() == null || entity.getUserName() == null || entity.getEmail() == null
				|| entity.getPhone() == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new UserResponse(false, "更新失敗，資料有誤", null));
		}

		User edited = new User();
		edited.setUserId(entity.getUserId());
		edited.setUserName(entity.getUserName());
		edited.setEmail(entity.getEmail());
		edited.setPhone(entity.getPhone());
		userService.updateAnyUser(entity.getUserId(), edited);

		UserDTO dto = new UserDTO();
		dto.setUserId(edited.getUserId());
		dto.setEmail(edited.getEmail());
		dto.setUserName(edited.getUserName());
		dto.setPhone(edited.getPhone());

		return ResponseEntity.status(HttpStatus.OK).body(new UserResponse(true, "更新成功", dto));

	}

	@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
	@DeleteMapping("/user/any/{id}")
	public ResponseEntity<UserResponse> deleteAnyUser(@PathVariable Integer id) {

		if (id == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new UserResponse(false, "查無使用者id:" + id, null));
		}
		boolean result = userService.deleteAnyUser(id);

		if (!result) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new UserResponse(false, "刪除失敗", null));
		}

		return ResponseEntity.status(HttpStatus.OK).body(new UserResponse(true, "刪除成功", null));
	}
	
	@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
	@PutMapping("/user/status/{id}")
	public ResponseEntity<UserResponse> updateUserStatus(@PathVariable Integer id, @RequestBody StatusUpdateRequest request) {
	    try {
	        if (id == null) {
	            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
	                .body(new UserResponse(false, "無效的使用者ID", null));
	        }
	        
	        if (request.getStatus() == null || 
	            (!request.getStatus().equals("ACTIVE") && !request.getStatus().equals("BANNED"))) {
	            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
	                .body(new UserResponse(false, "無效的狀態值", null));
	        }
	        
	        boolean result = userService.updateUserStatus(id, request.getStatus());
	        
	        if (!result) {
	            return ResponseEntity.status(HttpStatus.NOT_FOUND)
	                .body(new UserResponse(false, "找不到使用者，無法更新狀態", null));
	        }
	        
	        return ResponseEntity.status(HttpStatus.OK)
	            .body(new UserResponse(true, "使用者狀態已更新", null));
	            
	    } catch (Exception e) {
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	            .body(new UserResponse(false, "系統錯誤：" + e.getMessage(), null));
	    }
	}
	
	@PreAuthorize("hasRole('SUPER_ADMIN')")
	@PutMapping("/user/sa/status/{id}")
	public ResponseEntity<UserResponse> updateAdminStatus(@PathVariable Integer id, @RequestBody StatusUpdateRequest request) {
	    try {
	        if (id == null) {
	            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
	                .body(new UserResponse(false, "無效的管理員ID", null));
	        }
	        
	        if (request.getStatus() == null || 
	            (!request.getStatus().equals("ACTIVE") && !request.getStatus().equals("BANNED"))) {
	            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
	                .body(new UserResponse(false, "無效的狀態值", null));
	        }
	        
	        // 檢查欲更新的用戶是否為 SUPER_ADMIN
	        Optional<User> userOpt = userService.findById(id);
	        if (userOpt.isPresent()) {
	            User user = userOpt.get();
	            boolean isSuperAdmin = user.getRole().stream()
	                .anyMatch(role -> role.getRoleName().equals("SUPER_ADMIN"));
	                
	            if (isSuperAdmin) {
	                return ResponseEntity.status(HttpStatus.FORBIDDEN)
	                    .body(new UserResponse(false, "不可變更超級管理員的帳號狀態", null));
	            }
	        }
	        
	        boolean result = userService.updateUserStatus(id, request.getStatus());
	        
	        if (!result) {
	            return ResponseEntity.status(HttpStatus.NOT_FOUND)
	                .body(new UserResponse(false, "找不到管理員，無法更新狀態", null));
	        }
	        
	        return ResponseEntity.status(HttpStatus.OK)
	            .body(new UserResponse(true, "管理員狀態已更新", null));
	            
	    } catch (Exception e) {
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	            .body(new UserResponse(false, "系統錯誤：" + e.getMessage(), null));
	    }
	}

}
