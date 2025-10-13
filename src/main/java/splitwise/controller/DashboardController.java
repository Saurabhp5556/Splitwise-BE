package splitwise.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import splitwise.service.DashboardService;

import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    /**
     * Get complete dashboard data for a user
     */
    @GetMapping("/users/{userId}")
    public ResponseEntity<Map<String, Object>> getUserDashboard(@PathVariable String userId) {
        try {
            Map<String, Object> dashboardData = dashboardService.getDashboardData(userId);
            return ResponseEntity.ok(dashboardData);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get all groups for a user with balance information
     */
    @GetMapping("/users/{userId}/groups")
    public ResponseEntity<Map<String, Object>> getUserGroups(@PathVariable String userId) {
        try {
            Map<String, Object> groupsData = dashboardService.getUserGroupsWithBalances(userId);
            return ResponseEntity.ok(groupsData);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get all users with whom the current user has balances
     */
    @GetMapping("/users/{userId}/balances")
    public ResponseEntity<Map<String, Object>> getUserBalances(@PathVariable String userId) {
        try {
            Map<String, Object> balancesData = dashboardService.getUserBalances(userId);
            return ResponseEntity.ok(balancesData);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}