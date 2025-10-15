package splitwise.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import splitwise.model.Expense;
import splitwise.service.ExpenseService;
import splitwise.util.SplitTypes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {

    @Autowired
    private ExpenseService expenseService;

    @GetMapping
    public ResponseEntity<List<Expense>> getAllExpenses() {
        return ResponseEntity.ok(expenseService.getAllExpenses());
    }

    @GetMapping("/{expenseId}")
    public ResponseEntity<Expense> getExpenseById(@PathVariable("expenseId") String expenseId) {
        try {
            Expense expense = expenseService.getExpenseById(expenseId);
            return ResponseEntity.ok(expense);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<Expense> addExpense(@RequestBody Map<String, Object> request) {
        try {
            String title = (String) request.get("title");
            String description = (String) request.get("description");
            Double amount = Double.parseDouble(request.get("amount").toString());
            String payerId = (String) request.get("payerId");
            @SuppressWarnings("unchecked")
            List<String> participantIds = (List<String>) request.get("participantIds");
            String splitTypeStr = (String) request.get("splitType");
            SplitTypes splitType = SplitTypes.valueOf(splitTypeStr);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> splitDetails = request.containsKey("splitDetails") ? 
                (Map<String, Object>) request.get("splitDetails") : new HashMap<>();

            Expense expense = expenseService.addExpense(title, description, amount, payerId, 
                                                      participantIds, splitType, splitDetails);
            return ResponseEntity.status(HttpStatus.CREATED).body(expense);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(null);
        }
    }

    @PostMapping("/group")
    public ResponseEntity<Expense> addGroupExpense(@RequestBody Map<String, Object> request) {
        try {
            String title = (String) request.get("title");
            String description = (String) request.get("description");
            Double amount = Double.parseDouble(request.get("amount").toString());
            String payerId = (String) request.get("payerId");
            Long groupId = Long.parseLong(request.get("groupId").toString());
            String splitTypeStr = (String) request.get("splitType");
            SplitTypes splitType = SplitTypes.valueOf(splitTypeStr);
            
            @SuppressWarnings("unchecked")
            List<String> participantIds = request.containsKey("participantIds") ?
                (List<String>) request.get("participantIds") : null;
            
            @SuppressWarnings("unchecked")
            Map<String, Object> splitDetails = request.containsKey("splitDetails") ?
                (Map<String, Object>) request.get("splitDetails") : new HashMap<>();

            Expense expense = expenseService.addGroupExpense(title, description, amount, payerId,
                                                           groupId, participantIds, splitType, splitDetails);
            return ResponseEntity.status(HttpStatus.CREATED).body(expense);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(null);
        }
    }

    @PutMapping("/{expenseId}")
    public ResponseEntity<Expense> editExpense(@PathVariable("expenseId") String expenseId,
                                             @RequestBody Map<String, Object> request) {
        try {
            String title = (String) request.get("title");
            String description = (String) request.get("description");
            Double amount = request.containsKey("amount") ? 
                Double.parseDouble(request.get("amount").toString()) : null;
            String payerId = (String) request.get("payerId");
            
            @SuppressWarnings("unchecked")
            List<String> participantIds = request.containsKey("participantIds") ? 
                (List<String>) request.get("participantIds") : null;
            
            SplitTypes splitType = null;
            if (request.containsKey("splitType")) {
                String splitTypeStr = (String) request.get("splitType");
                splitType = SplitTypes.valueOf(splitTypeStr);
            }
            
            @SuppressWarnings("unchecked")
            Map<String, Object> splitDetails = request.containsKey("splitDetails") ? 
                (Map<String, Object>) request.get("splitDetails") : null;

            Expense expense = expenseService.editExpense(expenseId, title, description, amount, 
                                                      payerId, participantIds, splitType, splitDetails);
            return ResponseEntity.ok(expense);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(null);
        }
    }

    @DeleteMapping("/{expenseId}")
    public ResponseEntity<Void> deleteExpense(@PathVariable("expenseId") String expenseId) {
        try {
            expenseService.deleteExpense(expenseId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/group/{groupId}")
    public ResponseEntity<List<Expense>> getAllGroupExpenses(@PathVariable("groupId") Long groupId) {
        return ResponseEntity.ok(expenseService.getExpensesByGroup(groupId));
    }
}
