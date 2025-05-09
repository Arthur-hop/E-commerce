package ourpkg.user_role_permission;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class RoleController {

	@Autowired
	private RoleService roleService;

	@GetMapping("/role/add")
	public String addPage() {
		return "role/newRolePage";
	}

	@PostMapping("/role/addPost")
	public String insertRolePost(String roleName, Model model) {
		roleService.insertRole(roleName);

		model.addAttribute("okMsg", "新增OK");
		return "role/newRolePage";
	}

	@ResponseBody
	@GetMapping("/api/role")
	public List<Role> findAllRole() {
		return roleService.findAllRole();
	}

	@ResponseBody
	@GetMapping("/api/role/{id}")
	public Role findRole(@PathVariable Integer id) {
		return roleService.findRoleById(id);
	}

	@GetMapping("/role/update")
	public String updatePage(@RequestParam Integer id, Model model) {
		Role role = roleService.findRoleById(id);

		model.addAttribute("role", role);

		return "role/editRolePage";
	}

	@PostMapping("/role/updatePost")
	public String updateRole(@ModelAttribute Role role, Model model) {

		roleService.updateRole(role);

		return "redirect:/api/role";
	}
	
	@GetMapping("/role/delete")
	public String deleteRole(@RequestParam Integer id) {
		roleService.deleteRoleById(id);
		return "redirect:/api/role";
	}

}
