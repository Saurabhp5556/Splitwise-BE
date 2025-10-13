package splitwise.util;

import splitwise.model.Expense;

public interface ExpenseSubject {

    void addObserver(ExpenseObserver observer);
    void removeObserver(ExpenseObserver observer);
    void notifyExpenseAdded(Expense expense);
    void notifyExpenseUpdated(Expense expense);
}
