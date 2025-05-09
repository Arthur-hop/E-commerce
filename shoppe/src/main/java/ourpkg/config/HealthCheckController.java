package ourpkg.config;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthCheckController {

    @GetMapping("/api/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("✅ Healthy");
    }
}
