package ourpkg.user_role_permission;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ourpkg.user_role_permission.user.service.UserService;

@RestController
@RequestMapping("/api/stats")
public class UserStatsController {

    @Autowired
    private UserService userService;
    
    @GetMapping("/new-users")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> getNewUsersStats(
            @RequestParam(defaultValue = "30") int days) {
        
        Map<String, Object> stats = userService.getNewUsersStatistics(days);
        return ResponseEntity.ok(stats);
    }
}