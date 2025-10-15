package splitwise.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import splitwise.model.Expense;
import splitwise.model.Transaction;
import splitwise.model.User;
import splitwise.model.UserPair;
import splitwise.repository.TransactionRepository;
import splitwise.repository.UserPairRepository;
import splitwise.util.ExpenseObserver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * BalanceSheet Service - Manages financial balances between users
 *
 * This service is responsible for:
 * 1. Tracking who owes money to whom (UserPair records)
 * 2. Calculating net balances for users
 * 3. Generating simplified settlement transactions
 * 4. Automatically updating balances when expenses are created/modified
 *
 * Key Concepts:
 * - UserPair: Represents a debt relationship where user1 owes user2 a certain amount
 * - Observer Pattern: Automatically updates balances when expenses change
 * - Net Balance: Overall amount a user owes or is owed across all relationships
 */
@Service
public class BalanceSheet implements ExpenseObserver {

    @Autowired
    private UserPairRepository userPairRepository;
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    @Autowired
    @Lazy
    private ExpenseManager expenseManager;
    
    @PostConstruct
    public void init() {
        expenseManager.addObserver(this);
    }

    @Override
    public void onExpenseAdded(Expense expense) {
        updateBalances(expense);
    }

    @Override
    public void onExpenseUpdated(Expense expense) {
        updateBalances(expense);
    }

    /**
     * Updates user balances when an expense is added or modified.
     * For each participant who didn't pay, creates or updates a UserPair record
     * indicating how much they owe the payer.
     */
    private void updateBalances(Expense expense) {
        User payer = expense.getPayer();
        Map<User, Double> shares = expense.getShares();

        for (Map.Entry<User, Double> entry : shares.entrySet()) {
            User participant = entry.getKey();
            Double amount = entry.getValue();

            // Skip the payer - they don't owe themselves
            if (!participant.equals(payer)) {
                updateUserPairBalance(participant, payer, amount);
            }
        }
    }

    /**
     * Updates or creates a UserPair record for the balance between two users.
     */
    private void updateUserPairBalance(User debtor, User creditor, Double amount) {
        Optional<UserPair> existingPair = userPairRepository.findByUser1AndUser2(debtor, creditor);
        
        if (existingPair.isPresent()) {
            // Update existing balance
            UserPair pair = existingPair.get();
            pair.setBalance(pair.getBalance() + amount);
            userPairRepository.save(pair);
        } else {
            // Create new UserPair
            UserPair newPair = new UserPair(debtor, creditor);
            newPair.setBalance(amount);
            userPairRepository.save(newPair);
        }
    }

    public double getBalance(User u1, User u2) {
        Optional<UserPair> pair = userPairRepository.findByUser1AndUser2(u1, u2);
        return pair.isPresent() ? pair.get().getBalance() : 0.0;
    }

    public double getTotalBalance(User user) {
        double total = 0.0;
        List<UserPair> userPairs = userPairRepository.findByUser(user);

        for (UserPair pair : userPairs) {
            if (pair.getUser1().equals(user)) {
                total -= pair.getBalance(); // Money Owed by the user
            } else if (pair.getUser2().equals(user)) {
                total += pair.getBalance(); // Money Owed to the user
            }
        }

        return total;
    }

    /**
     * Calculates the net balance for each user across all their relationships.
     * Positive balance means the user is owed money, negative means they owe money.
     */
    private Map<User, Double> calculateNetBalances() {
        Map<User, Double> netBalances = new HashMap<>();
        List<UserPair> allPairs = userPairRepository.findAll();
        
        for (UserPair pair : allPairs) {
            User debtor = pair.getUser1();
            User creditor = pair.getUser2();
            Double amount = pair.getBalance();
            
            netBalances.put(debtor, netBalances.getOrDefault(debtor, 0.0) - amount);
            netBalances.put(creditor, netBalances.getOrDefault(creditor, 0.0) + amount);
        }
        
        return netBalances;
    }

    /**
     * Calculates simplified settlements to minimize the number of transactions needed
     * to settle all balances between users.
     */
    public List<Transaction> getSimplifiedSettlements() {
        Map<User, Double> netBalances = calculateNetBalances();

        List<User> debtorsList = new ArrayList<>();
        List<User> creditorsList = new ArrayList<>();
        for (Map.Entry<User, Double> entry : netBalances.entrySet()) {
            User user = entry.getKey();
            double balance = entry.getValue();

            if (balance < 0) {
                debtorsList.add(user);
            } else if (balance > 0) {
                creditorsList.add(user);
            }
        }

        List<Transaction> transactions = new ArrayList<>();
        int debtorIndex = 0;
        int creditorIndex = 0;
        while (debtorIndex < debtorsList.size() && creditorIndex < creditorsList.size()) {
            User debtor = debtorsList.get(debtorIndex);
            User creditor = creditorsList.get(creditorIndex);

            double debtorBalance = netBalances.get(debtor);
            double creditorBalance = netBalances.get(creditor);

            double transferAmount = Math.min(Math.abs(debtorBalance), creditorBalance);

            Transaction transaction = new Transaction(debtor, creditor, transferAmount);
            transaction = transactionRepository.save(transaction);
            transactions.add(transaction);

            netBalances.put(debtor, debtorBalance + transferAmount);
            netBalances.put(creditor, creditorBalance - transferAmount);

            if (Math.abs(netBalances.get(debtor)) < 0.001) {
                debtorIndex++;
            }
            if (Math.abs(netBalances.get(creditor)) < 0.001) {
                creditorIndex++;
            }
        }
        return transactions;
    }

    /**
     * Calculates the minimum number of transactions needed to settle all balances
     * using a sub-optimal but efficient algorithm.
     */
    public int getSubOptimalMinimumSettlements() {
        Map<User, Double> netBalances = calculateNetBalances();

        List<Double> creditorList = new ArrayList<>();

        for (Map.Entry<User, Double> entry : netBalances.entrySet()) {
            if (Math.abs(entry.getValue()) > 0.001) {
                creditorList.add(entry.getValue());
            }
        }

        int n = creditorList.size();
        return subOptimalDfs(0, creditorList, n);
    }

    private int subOptimalDfs(int currentUserIndex, List<Double> creditorList, int n) {
        // skip already settled users (those with zero balance)
        while (currentUserIndex < n && creditorList.get(currentUserIndex) == 0) {
            currentUserIndex++;
        }

        // Base Case: If all users have zero balance, no further transactions are needed
        if (currentUserIndex == n) {
            return 0;
        }

        int cost = Integer.MAX_VALUE; // variable to track min no. of transactions

        // try to settle currentUserBalance with future user having opposite balance
        for (int nextIndex = currentUserIndex + 1; nextIndex < n; nextIndex++) {
            // ensure we only settle debts between opposite balances
            if (creditorList.get(nextIndex) * creditorList.get(currentUserIndex) < 0) {
                // transfer current user balance to next user
                creditorList.set(nextIndex, creditorList.get(nextIndex) + creditorList.get(currentUserIndex));

                // recursively settle remaining balances
                cost = Math.min(cost, 1 + subOptimalDfs(currentUserIndex + 1, creditorList, n));

                // Backtrack : undo transaction to explore other possibilities
                creditorList.set(nextIndex, creditorList.get(nextIndex) - creditorList.get(currentUserIndex));
            }
        }
        return cost;
    }
}
