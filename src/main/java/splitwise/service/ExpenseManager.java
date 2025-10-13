package splitwise.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import splitwise.model.Expense;
import splitwise.repository.ExpenseRepository;
import splitwise.util.ExpenseObserver;
import splitwise.util.ExpenseSubject;

import java.util.ArrayList;
import java.util.List;

@Service
public class ExpenseManager implements ExpenseSubject {

    private List<ExpenseObserver> observers = new ArrayList<>();
    
    @Autowired
    private ExpenseRepository expenseRepository;

    @Override
    public void addObserver(ExpenseObserver observer) {
        observers.add(observer);
    }

    @Override
    public void removeObserver(ExpenseObserver observer) {
        observers.remove(observer);
    }

    @Override
    public void notifyExpenseAdded(Expense expense) {
        for (ExpenseObserver observer : observers)
            observer.onExpenseAdded(expense);
    }

    @Override
    public void notifyExpenseUpdated(Expense expense) {
        for (ExpenseObserver observer : observers)
            observer.onExpenseUpdated(expense);
    }

    public void addExpense(Expense expense) {
        expenseRepository.save(expense);
        notifyExpenseAdded(expense);
    }

    public void updateExpense(Expense expense) {
        if (!expenseRepository.existsById(expense.getId())) {
            throw new IllegalArgumentException("Expense with ID " + expense.getId() + " not found");
        }
        expenseRepository.save(expense);
        notifyExpenseUpdated(expense);
    }

    public List<Expense> getAllExpenses() {
        return expenseRepository.findAll();
    }
    
    public Expense getExpenseById(String id) {
        return expenseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Expense with ID " + id + " not found"));
    }
    
    public void deleteExpense(String id) {
        if (!expenseRepository.existsById(id)) {
            throw new IllegalArgumentException("Expense with ID " + id + " not found");
        }
        expenseRepository.deleteById(id);
    }
}
