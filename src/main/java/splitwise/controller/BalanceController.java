package splitwise.controller;

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
        try {
            User user1 = userService.getUser(user1Id);
            User user2 = userService.getUser(user2Id);

            double balance = balanceSheet.getBalance(user1, user2);
            
            Map<String, Object> response = new HashMap<>();
            response.put("user1", user1);
            response.put("user2", user2);
            response.put("balance", balance);
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get total balance for a user
     */
    @GetMapping("/users/{userId}/total")
    public ResponseEntity<Map<String, Object>> getTotalBalance(@PathVariable String userId) {
        try {
            User user = userService.getUser(userId);
            double totalBalance = balanceSheet.getTotalBalance(user);
            
            Map<String, Object> response = new HashMap<>();
            response.put("user", user);
            response.put("totalBalance", totalBalance);
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get simplified settlements for all users
     */
    @GetMapping("/settlements")
    public ResponseEntity<Map<String, Object>> getSimplifiedSettlements() {
        List<Transaction> settlements = balanceSheet.getSimplifiedSettlements();
        int minTransactions = balanceSheet.getSubOptimalMinimumSettlements();
        
        Map<String, Object> response = new HashMap<>();
        response.put("settlements", settlements);
        response.put("minTransactions", minTransactions);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get simplified settlements for a specific user
     */
    @GetMapping("/users/{userId}/settlements")
    public ResponseEntity<Map<String, Object>> getUserSettlements(@PathVariable String userId) {
        try {
            User user = userService.getUser(userId);
            List<Transaction> allSettlements = balanceSheet.getSimplifiedSettlements();
            
            // Filter settlements that involve the user
            List<Transaction> userSettlements = allSettlements.stream()
                    .filter(t -> t.getFrom().equals(user) || t.getTo().equals(user))
                    .toList();
            
            Map<String, Object> response = new HashMap<>();
            response.put("user", user);
            response.put("settlements", userSettlements);
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}