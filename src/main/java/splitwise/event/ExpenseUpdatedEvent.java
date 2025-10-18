package splitwise.event;

import org.springframework.context.ApplicationEvent;
import splitwise.model.Expense;

public class ExpenseUpdatedEvent extends ApplicationEvent {
    private final Expense expense;

    public ExpenseUpdatedEvent(Object source, Expense expense) {
        super(source);
        this.expense = expense;
    }

    public Expense getExpense() {
        return expense;
    }
}