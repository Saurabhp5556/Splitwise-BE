package splitwise.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import splitwise.service.AdminService;

import java.util.Base64;
import java.util.Map;

/**
 * AdminController - Provides administrative operations for the Splitwise application
 * 
 * This controller handles sensitive administrative operations that require authentication.
 * Currently supports:
 * - Database cleanup (delete all data)
 * 
 * Security: Uses Basic Authentication with fixed credentials (admin/admin)
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);
    
    // Fixed admin credentials
    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "admin";

    @Autowired
    private AdminService adminService;

    /**
     * Deletes all data from the database.
     * 
     * This is a destructive operation that removes all:
     * - Users
     * - Groups
     * - Expenses
     * - User pairs (balances)
     * - Transactions
     * 
     * Requires Basic Authentication with username: admin, password: admin
     * 
     * @param authHeader Authorization header with Basic authentication
     * @return Success message if operation completes
     */
    @DeleteMapping("/database/clear")
    public ResponseEntity<Map<String, Object>> clearDatabase(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        logger.warn("Database clear operation requested");
        
        // Validate authentication
        if (!isValidAuth(authHeader)) {
            logger.warn("Unauthorized database clear attempt");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "error", "Unauthorized",
                            "message", "Valid admin credentials required",
                            "hint", "Use Basic Auth with username: admin, password: admin"
                    ));
        }
        
        try {
            // Perform the database cleanup
            Map<String, Integer> deletionStats = adminService.clearAllData();
            
            logger.warn("Database cleared successfully. Stats: {}", deletionStats);
            
            return ResponseEntity.ok(Map.of(
                    "message", "Database cleared successfully",
                    "deletedRecords", deletionStats,
                    "timestamp", java.time.LocalDateTime.now().toString()
            ));
            
        } catch (Exception e) {
            logger.error("Error clearing database", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "Database clear failed",
                            "message", e.getMessage()
                    ));
        }
    }

    /**
     * Validates the Basic Authentication header
     */
    private boolean isValidAuth(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Basic ")) {
            return false;
        }
        
        try {
            // Extract and decode the credentials
            String base64Credentials = authHeader.substring(6);
            String credentials = new String(Base64.getDecoder().decode(base64Credentials));
            String[] parts = credentials.split(":", 2);
            
            if (parts.length != 2) {
                return false;
            }
            
            String username = parts[0];
            String password = parts[1];
            
            return ADMIN_USERNAME.equals(username) && ADMIN_PASSWORD.equals(password);
            
        } catch (Exception e) {
            logger.warn("Error parsing authentication header", e);
            return false;
        }
    }

    /**
     * Health check endpoint for admin operations
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> adminHealth() {
        return ResponseEntity.ok(Map.of(
                "status", "Admin endpoints available",
                "authentication", "Basic Auth required for destructive operations"
        ));
    }
}