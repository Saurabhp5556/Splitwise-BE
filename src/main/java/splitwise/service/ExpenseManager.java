package splitwise.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import splitwise.event.ExpenseAddedEvent;
import splitwise.event.ExpenseUpdatedEvent;
import splitwise.model.Expense;
import splitwise.repository.ExpenseRepository;

import java.util.List;

@Service
public class ExpenseManager {
    
    @Autowired
    private ExpenseRepository expenseRepository;
    
    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Transactional
    public void addExpense(Expense expense) {
        expenseRepository.save(expense);
        eventPublisher.publishEvent(new ExpenseAddedEvent(this, expense));
    }

    @Transactional
    public void updateExpense(Expense expense) {
        if (!expenseRepository.existsById(expense.getId())) {
            throw new IllegalArgumentException("Expense with ID " + expense.getId() + " not found");
        }
        expenseRepository.save(expense);
        eventPublisher.publishEvent(new ExpenseUpdatedEvent(this, expense));
    }

    public List<Expense> getAllExpenses() {
        return expenseRepository.findAll();
    }
    
    public Expense getExpenseById(String id) {
        return expenseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Expense with ID " + id + " not found"));
    }
    
    @Transactional
    public void deleteExpense(String id) {
        if (!expenseRepository.existsById(id)) {
            throw new IllegalArgumentException("Expense with ID " + id + " not found");
        }
        expenseRepository.deleteById(id);
    }
}
