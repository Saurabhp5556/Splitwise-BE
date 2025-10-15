package splitwise.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import splitwise.service.DashboardService;

import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);

    @Autowired
    private DashboardService dashboardService;

    /**
     * Get complete dashboard data for a user
     */
    @GetMapping("/users/{userId}")
    public ResponseEntity<Map<String, Object>> getUserDashboard(@PathVariable String userId) {
        logger.info("Fetching dashboard data for user: {}", userId);
        
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID is required and cannot be empty");
        }
        
        Map<String, Object> dashboardData = dashboardService.getDashboardData(userId);
        logger.info("Successfully retrieved dashboard data for user: {}", userId);
        return ResponseEntity.ok(dashboardData);
    }

    /**
     * Get all groups for a user with balance information
     */
    @GetMapping("/users/{userId}/groups")
    public ResponseEntity<Map<String, Object>> getUserGroups(@PathVariable String userId) {
        logger.info("Fetching groups with balances for user: {}", userId);
        
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID is required and cannot be empty");
        }
        
        Map<String, Object> groupsData = dashboardService.getUserGroupsWithBalances(userId);
        logger.info("Successfully retrieved groups data for user: {}", userId);
        return ResponseEntity.ok(groupsData);
    }

    /**
     * Get all users with whom the current user has balances
     */
    @GetMapping("/users/{userId}/balances")
    public ResponseEntity<Map<String, Object>> getUserBalances(@PathVariable String userId) {
        logger.info("Fetching user balances for user: {}", userId);
        
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID is required and cannot be empty");
        }
        
        Map<String, Object> balancesData = dashboardService.getUserBalances(userId);
        logger.info("Successfully retrieved balances data for user: {}", userId);
        return ResponseEntity.ok(balancesData);
    }
}