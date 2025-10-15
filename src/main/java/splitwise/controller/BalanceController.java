package splitwise.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import splitwise.model.Transaction;
import splitwise.model.User;
import splitwise.service.BalanceSheet;
import splitwise.service.UserService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/balances")
public class BalanceController {

    private static final Logger logger = LoggerFactory.getLogger(BalanceController.class);

    @Autowired
    private BalanceSheet balanceSheet;

    @Autowired
    private UserService userService;

    /**
     * Get balance between two users
     */
    @GetMapping("/users")
    public ResponseEntity<Map<String, Object>> getBalanceBetweenUsers(
            @RequestParam String user1Id,
            @RequestParam String user2Id) {
        
        logger.info("Fetching balance between users: {} and {}", user1Id, user2Id);
        
        if (user1Id == null || user1Id.trim().isEmpty()) {
            throw new IllegalArgumentException("User1 ID is required and cannot be empty");
        }
        if (user2Id == null || user2Id.trim().isEmpty()) {
            throw new IllegalArgumentException("User2 ID is required and cannot be empty");
        }
        if (user1Id.equals(user2Id)) {
            throw new IllegalArgumentException("User1 and User2 cannot be the same");
        }
        
        User user1 = userService.getUser(user1Id);
        User user2 = userService.getUser(user2Id);

        double balance = balanceSheet.getBalance(user1, user2);
        
        Map<String, Object> response = new HashMap<>();
        response.put("user1", user1);
        response.put("user2", user2);
        response.put("balance", balance);
        
        logger.info("Successfully retrieved balance between {} and {}: {}", user1Id, user2Id, balance);
        return ResponseEntity.ok(response);
    }

    /**
     * Get total balance for a user
     */
    @GetMapping("/users/{userId}/total")
    public ResponseEntity<Map<String, Object>> getTotalBalance(@PathVariable String userId) {
        logger.info("Fetching total balance for user: {}", userId);
        
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID is required and cannot be empty");
        }
        
        User user = userService.getUser(userId);
        double totalBalance = balanceSheet.getTotalBalance(user);
        
        Map<String, Object> response = new HashMap<>();
        response.put("user", user);
        response.put("totalBalance", totalBalance);
        
        logger.info("Successfully retrieved total balance for user {}: {}", userId, totalBalance);
        return ResponseEntity.ok(response);
    }

    /**
     * Get simplified settlements for all users
     */
    @GetMapping("/settlements")
    public ResponseEntity<Map<String, Object>> getSimplifiedSettlements() {
        logger.info("Fetching simplified settlements for all users");
        
        List<Transaction> settlements = balanceSheet.getSimplifiedSettlements();
        int minTransactions = balanceSheet.getSubOptimalMinimumSettlements();
        
        Map<String, Object> response = new HashMap<>();
        response.put("settlements", settlements);
        response.put("minTransactions", minTransactions);
        
        logger.info("Successfully retrieved {} settlements with {} minimum transactions", 
                   settlements.size(), minTransactions);
        return ResponseEntity.ok(response);
    }

    /**
     * Get simplified settlements for a specific user
     */
    @GetMapping("/users/{userId}/settlements")
    public ResponseEntity<Map<String, Object>> getUserSettlements(@PathVariable String userId) {
        logger.info("Fetching settlements for user: {}", userId);
        
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID is required and cannot be empty");
        }
        
        User user = userService.getUser(userId);
        List<Transaction> allSettlements = balanceSheet.getSimplifiedSettlements();
        
        // Filter settlements that involve the user
        List<Transaction> userSettlements = allSettlements.stream()
                .filter(t -> t.getFrom().equals(user) || t.getTo().equals(user))
                .toList();
        
        Map<String, Object> response = new HashMap<>();
        response.put("user", user);
        response.put("settlements", userSettlements);
        
        logger.info("Successfully retrieved {} settlements for user: {}", userSettlements.size(), userId);
        return ResponseEntity.ok(response);
    }
}