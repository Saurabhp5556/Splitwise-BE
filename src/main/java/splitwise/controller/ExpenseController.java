package splitwise.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(ExpenseController.class);

    @Autowired
    private ExpenseService expenseService;

    @GetMapping
    public ResponseEntity<List<Expense>> getAllExpenses() {
        logger.info("Fetching all expenses");
        List<Expense> expenses = expenseService.getAllExpenses();
        logger.info("Found {} expenses", expenses.size());
        return ResponseEntity.ok(expenses);
    }

    @GetMapping("/{expenseId}")
    public ResponseEntity<Expense> getExpenseById(@PathVariable("expenseId") String expenseId) {
        logger.info("Fetching expense with ID: {}", expenseId);
        Expense expense = expenseService.getExpenseById(expenseId);
        logger.info("Successfully found expense: {}", expense.getTitle());
        return ResponseEntity.ok(expense);
    }

    @PostMapping
    public ResponseEntity<Expense> addExpense(@RequestBody Map<String, Object> request) {
        logger.info("Adding new expense with request: {}", request);
        
        // Validate required fields
        validateExpenseRequest(request, false);
        
        String title = (String) request.get("title");
        String description = (String) request.get("description");
        Double amount = Double.parseDouble(request.get("amount").toString());
        String payerId = (String) request.get("payerId");
        @SuppressWarnings("unchecked")
        List<String> participantIds = (List<String>) request.get("participantIds");
        String splitTypeStr = (String) request.get("splitType");
        
        SplitTypes splitType;
        try {
            splitType = SplitTypes.valueOf(splitTypeStr);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid split type: " + splitTypeStr + ". Valid types are: EQUAL_SPLIT, SPLIT_BY_PERCENTAGES, SHARES_SPLIT, EXACT_AMOUNT_SPLIT, ADJUSTMENT_SPLIT");
        }
        
        @SuppressWarnings("unchecked")
        Map<String, Object> splitDetails = request.containsKey("splitDetails") ? 
            (Map<String, Object>) request.get("splitDetails") : new HashMap<>();

        Expense expense = expenseService.addExpense(title, description, amount, payerId, 
                                                  participantIds, splitType, splitDetails);
        logger.info("Successfully created expense with ID: {}", expense.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(expense);
    }

    @PostMapping("/group")
    public ResponseEntity<Expense> addGroupExpense(@RequestBody Map<String, Object> request) {
        logger.info("Adding new group expense with request: {}", request);
        
        // Validate required fields
        validateGroupExpenseRequest(request);
        
        String title = (String) request.get("title");
        String description = (String) request.get("description");
        Double amount = Double.parseDouble(request.get("amount").toString());
        String payerId = (String) request.get("payerId");
        String groupId = request.get("groupId").toString();
        String splitTypeStr = (String) request.get("splitType");
        
        SplitTypes splitType;
        try {
            splitType = SplitTypes.valueOf(splitTypeStr);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid split type: " + splitTypeStr + ". Valid types are: EQUAL_SPLIT, SPLIT_BY_PERCENTAGES, SHARES_SPLIT, EXACT_AMOUNT_SPLIT, ADJUSTMENT_SPLIT");
        }
        
        @SuppressWarnings("unchecked")
        List<String> participantIds = request.containsKey("participantIds") ? 
            (List<String>) request.get("participantIds") : null;
        
        @SuppressWarnings("unchecked")
        Map<String, Object> splitDetails = request.containsKey("splitDetails") ? 
            (Map<String, Object>) request.get("splitDetails") : new HashMap<>();

        Expense expense = expenseService.addGroupExpense(title, description, amount, payerId, 
                                                       groupId, participantIds, splitType, splitDetails);
        logger.info("Successfully created group expense with ID: {}", expense.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(expense);
    }

    @PutMapping("/{expenseId}")
    public ResponseEntity<Expense> editExpense(@PathVariable("expenseId") String expenseId,
                                             @RequestBody Map<String, Object> request) {
        logger.info("Editing expense with ID: {} with request: {}", expenseId, request);
        
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
            try {
                splitType = SplitTypes.valueOf(splitTypeStr);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid split type: " + splitTypeStr + ". Valid types are: EQUAL_SPLIT, SPLIT_BY_PERCENTAGES, SHARES_SPLIT, EXACT_AMOUNT_SPLIT, ADJUSTMENT_SPLIT");
            }
        }
        
        @SuppressWarnings("unchecked")
        Map<String, Object> splitDetails = request.containsKey("splitDetails") ? 
            (Map<String, Object>) request.get("splitDetails") : null;

        Expense expense = expenseService.editExpense(expenseId, title, description, amount, 
                                                  payerId, participantIds, splitType, splitDetails);
        logger.info("Successfully updated expense with ID: {}", expenseId);
        return ResponseEntity.ok(expense);
    }

    @DeleteMapping("/{expenseId}")
    public ResponseEntity<Void> deleteExpense(@PathVariable("expenseId") String expenseId) {
        logger.info("Deleting expense with ID: {}", expenseId);
        expenseService.deleteExpense(expenseId);
        logger.info("Successfully deleted expense with ID: {}", expenseId);
        return ResponseEntity.noContent().build();
    }
    
    private void validateExpenseRequest(Map<String, Object> request, boolean isUpdate) {
        if (!isUpdate) {
            if (request.get("title") == null || request.get("title").toString().trim().isEmpty()) {
                throw new IllegalArgumentException("Title is required and cannot be empty");
            }
            if (request.get("amount") == null) {
                throw new IllegalArgumentException("Amount is required");
            }
            if (request.get("payerId") == null || request.get("payerId").toString().trim().isEmpty()) {
                throw new IllegalArgumentException("Payer ID is required and cannot be empty");
            }
            if (request.get("participantIds") == null) {
                throw new IllegalArgumentException("Participant IDs are required");
            }
            if (request.get("splitType") == null || request.get("splitType").toString().trim().isEmpty()) {
                throw new IllegalArgumentException("Split type is required and cannot be empty");
            }
        }
        
        // Validate amount if provided
        if (request.get("amount") != null) {
            try {
                double amount = Double.parseDouble(request.get("amount").toString());
                if (amount <= 0) {
                    throw new IllegalArgumentException("Amount must be greater than 0");
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Amount must be a valid number");
            }
        }
    }
    
    private void validateGroupExpenseRequest(Map<String, Object> request) {
        if (request.get("title") == null || request.get("title").toString().trim().isEmpty()) {
            throw new IllegalArgumentException("Title is required and cannot be empty");
        }
        if (request.get("amount") == null) {
            throw new IllegalArgumentException("Amount is required");
        }
        if (request.get("payerId") == null || request.get("payerId").toString().trim().isEmpty()) {
            throw new IllegalArgumentException("Payer ID is required and cannot be empty");
        }
        if (request.get("groupId") == null) {
            throw new IllegalArgumentException("Group ID is required");
        }
        if (request.get("splitType") == null || request.get("splitType").toString().trim().isEmpty()) {
            throw new IllegalArgumentException("Split type is required and cannot be empty");
        }
        
        // Validate amount
        try {
            double amount = Double.parseDouble(request.get("amount").toString());
            if (amount <= 0) {
                throw new IllegalArgumentException("Amount must be greater than 0");
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Amount must be a valid number");
        }
        
        // Validate group ID
        String groupId = request.get("groupId").toString();
        if (groupId == null || groupId.trim().isEmpty()) {
            throw new IllegalArgumentException("Group ID must be a valid string");
        }
    }

    @GetMapping("/group/{groupId}")
    public ResponseEntity<List<Expense>> getAllGroupExpenses(@PathVariable("groupId") String groupId) {
        return ResponseEntity.ok(expenseService.getExpensesByGroup(groupId));
    }
}
