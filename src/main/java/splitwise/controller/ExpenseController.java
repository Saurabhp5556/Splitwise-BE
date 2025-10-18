package splitwise.controller;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import splitwise.dto.CreateExpenseRequest;
import splitwise.dto.ExpenseResponse;
import splitwise.model.Expense;
import splitwise.service.DtoMapperService;
import splitwise.service.ExpenseService;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {

    private static final Logger logger = LoggerFactory.getLogger(ExpenseController.class);

    @Autowired
    private ExpenseService expenseService;

    @Autowired
    private DtoMapperService dtoMapper;

    @GetMapping
    public ResponseEntity<List<ExpenseResponse>> getAllExpenses() {
        logger.info("Fetching all expenses");
        List<Expense> expenses = expenseService.getAllExpenses();
        List<ExpenseResponse> response = expenses.stream()
                .map(dtoMapper::toExpenseResponse)
                .collect(Collectors.toList());
        logger.info("Found {} expenses", expenses.size());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{expenseId}")
    public ResponseEntity<ExpenseResponse> getExpenseById(@PathVariable("expenseId") String expenseId) {
        logger.info("Fetching expense with ID: {}", expenseId);
        Expense expense = expenseService.getExpenseById(expenseId);
        ExpenseResponse response = dtoMapper.toExpenseResponse(expense);
        logger.info("Successfully found expense: {}", expense.getTitle());
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<ExpenseResponse> addExpense(@Valid @RequestBody CreateExpenseRequest request) {
        logger.info("Adding new expense: {}", request.getTitle());
        
        Expense expense = expenseService.addExpense(
                request.getTitle(),
                request.getDescription(),
                request.getAmount(),
                request.getPayerId(),
                request.getParticipantIds(),
                request.getSplitType(),
                request.getSplitDetails(),
                request.getIsSettleUp()
        );
        
        ExpenseResponse response = dtoMapper.toExpenseResponse(expense);
        logger.info("Successfully created expense with ID: {}", expense.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/group")
    public ResponseEntity<ExpenseResponse> addGroupExpense(@Valid @RequestBody CreateExpenseRequest request) {
        logger.info("Adding new group expense: {}", request.getTitle());
        
        if (request.getGroupId() == null || request.getGroupId().trim().isEmpty()) {
            throw new IllegalArgumentException("Group ID is required for group expenses");
        }
        
        Expense expense = expenseService.addGroupExpense(
                request.getTitle(),
                request.getDescription(),
                request.getAmount(),
                request.getPayerId(),
                request.getGroupId(),
                request.getParticipantIds(),
                request.getSplitType(),
                request.getSplitDetails(),
                request.getIsSettleUp()
        );
        
        ExpenseResponse response = dtoMapper.toExpenseResponse(expense);
        logger.info("Successfully created group expense with ID: {}", expense.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{expenseId}")
    public ResponseEntity<ExpenseResponse> editExpense(
            @PathVariable("expenseId") String expenseId,
            @Valid @RequestBody CreateExpenseRequest request) {
        logger.info("Editing expense with ID: {}", expenseId);
        
        Expense expense = expenseService.editExpense(
                expenseId,
                request.getTitle(),
                request.getDescription(),
                request.getAmount(),
                request.getPayerId(),
                request.getParticipantIds(),
                request.getSplitType(),
                request.getSplitDetails(),
                request.getIsSettleUp()
        );
        
        ExpenseResponse response = dtoMapper.toExpenseResponse(expense);
        logger.info("Successfully updated expense with ID: {}", expenseId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{expenseId}")
    public ResponseEntity<Void> deleteExpense(@PathVariable("expenseId") String expenseId) {
        logger.info("Deleting expense with ID: {}", expenseId);
        expenseService.deleteExpense(expenseId);
        logger.info("Successfully deleted expense with ID: {}", expenseId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/group/{groupId}")
    public ResponseEntity<List<ExpenseResponse>> getAllGroupExpenses(@PathVariable("groupId") String groupId) {
        logger.info("Fetching expenses for group: {}", groupId);
        List<Expense> expenses = expenseService.getExpensesByGroup(groupId);
        List<ExpenseResponse> response = expenses.stream()
                .map(dtoMapper::toExpenseResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }
}
