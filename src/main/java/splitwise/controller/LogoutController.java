package splitwise.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class LogoutController {

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(Authentication authentication) {
        // In a stateless JWT system, logout is handled client-side by:
        // 1. Deleting the access token from client storage
        // 2. Deleting the refresh token from client storage
        // 3. Clearing any cached user data
        
        String userId = authentication != null ? authentication.getName() : "unknown";
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Logout successful. Please delete tokens from client storage.");
        response.put("userId", userId);
        
        return ResponseEntity.ok(response);
    }
}