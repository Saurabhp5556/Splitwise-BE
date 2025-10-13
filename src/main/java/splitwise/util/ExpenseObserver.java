package splitwise.util;

import splitwise.model.Expense;

public interface ExpenseObserver {

    void onExpenseAdded(Expense expense);

    void onExpenseUpdated(Expense expense);
}
