package splitwise.exception;

public class ExpenseNotFoundException extends SplitwiseException {
    public ExpenseNotFoundException(String expenseId) {
        super("Expense not found: " + expenseId, "EXPENSE_NOT_FOUND");
    }
}