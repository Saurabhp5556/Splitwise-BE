package splitwise.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import splitwise.event.ExpenseAddedEvent;
import splitwise.event.ExpenseUpdatedEvent;
import splitwise.model.Expense;
import splitwise.model.Transaction;
import splitwise.model.User;
import splitwise.model.UserPair;
import splitwise.repository.TransactionRepository;
import splitwise.repository.UserPairRepository;

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
public class BalanceSheet {

    @Autowired
    private UserPairRepository userPairRepository;
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    /**
     * Event listener for expense added events.
     * Automatically updates balances when a new expense is created.
     */
    @EventListener
    @Transactional
    public void handleExpenseAdded(ExpenseAddedEvent event) {
        updateBalances(event.getExpense());
    }

    /**
     * Event listener for expense updated events.
     * Automatically updates balances when an expense is modified.
     */
    @EventListener
    @Transactional
    public void handleExpenseUpdated(ExpenseUpdatedEvent event) {
        updateBalances(event.getExpense());
    }

    /**
     * Updates user balances when an expense is added or modified.
     * For each participant who didn't pay, creates or updates a UserPair record
     * indicating how much they owe the payer.
     */
    @Transactional
    public void updateBalances(Expense expense) {
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
    @Transactional
    @CacheEvict(value = "balances", allEntries = true)
    public void updateUserPairBalance(User debtor, User creditor, Double amount) {
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

    @Cacheable(value = "balances", key = "#u1.userId + '_' + #u2.userId")
    public double getBalance(User u1, User u2) {
        // Check if u1 owes u2 (u1 is user1, u2 is user2)
        Optional<UserPair> pair = userPairRepository.findByUser1AndUser2(u1, u2);
        if (pair.isPresent()) {
            return -pair.get().getBalance(); // Negative because u1 owes u2
        }
        
        // Check if u2 owes u1 (u2 is user1, u1 is user2)
        Optional<UserPair> reversePair = userPairRepository.findByUser1AndUser2(u2, u1);
        if (reversePair.isPresent()) {
            return reversePair.get().getBalance(); // Positive because u2 owes u1
        }
        
        return 0.0;
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
     * Reverses the balance changes caused by an expense.
     * This is used when deleting expenses to undo their impact on user balances.
     *
     * @param expense The expense whose balance changes should be reversed
     */
    @Transactional
    @CacheEvict(value = "balances", allEntries = true)
    public void reverseBalances(Expense expense) {
        User payer = expense.getPayer();
        Map<User, Double> shares = expense.getShares();

        for (Map.Entry<User, Double> entry : shares.entrySet()) {
            User participant = entry.getKey();
            Double amount = entry.getValue();

            // Skip the payer - they don't owe themselves
            if (!participant.equals(payer)) {
                // Reverse the balance by subtracting the amount
                reverseUserPairBalance(participant, payer, amount);
            }
        }
    }

    /**
     * Reverses a UserPair balance by subtracting the specified amount.
     */
    @Transactional
    public void reverseUserPairBalance(User debtor, User creditor, Double amount) {
        Optional<UserPair> existingPair = userPairRepository.findByUser1AndUser2(debtor, creditor);
        
        if (existingPair.isPresent()) {
            UserPair pair = existingPair.get();
            double newBalance = pair.getBalance() - amount;
            
            // If balance becomes zero or very close to zero, delete the UserPair
            if (Math.abs(newBalance) < 0.001) {
                userPairRepository.delete(pair);
            } else {
                pair.setBalance(newBalance);
                userPairRepository.save(pair);
            }
        }
        // If no existing pair found, this means the balance was already zero
        // No action needed for reversal
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
    @Transactional
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
     * Optimized settlement calculation using greedy algorithm
     * Time Complexity: O(n log n)
     * Space Complexity: O(n)
     *
     * This replaces the inefficient O(n!) recursive algorithm with a greedy approach
     * that achieves near-optimal results in linear-logarithmic time.
     */
    @Transactional
    public List<Transaction> getOptimizedSettlements() {
        Map<User, Double> netBalances = calculateNetBalancesOptimized();

        // Separate creditors and debtors using priority queues
        java.util.PriorityQueue<UserBalance> creditors = new java.util.PriorityQueue<>((a, b) ->
            Double.compare(b.amount, a.amount)); // Max heap
        java.util.PriorityQueue<UserBalance> debtors = new java.util.PriorityQueue<>((a, b) ->
            Double.compare(b.amount, a.amount)); // Max heap (absolute values)

        for (Map.Entry<User, Double> entry : netBalances.entrySet()) {
            double balance = entry.getValue();
            if (balance > 0.01) { // User is owed money
                creditors.offer(new UserBalance(entry.getKey(), balance));
            } else if (balance < -0.01) { // User owes money
                debtors.offer(new UserBalance(entry.getKey(), -balance));
            }
        }

        List<Transaction> settlements = new ArrayList<>();

        // Greedy matching: always match highest debtor with highest creditor
        while (!creditors.isEmpty() && !debtors.isEmpty()) {
            UserBalance creditor = creditors.poll();
            UserBalance debtor = debtors.poll();

            double settlementAmount = Math.min(creditor.amount, debtor.amount);

            Transaction transaction = new Transaction(
                debtor.user,    // from
                creditor.user,  // to
                settlementAmount
            );
            transaction = transactionRepository.save(transaction);
            settlements.add(transaction);

            // Add back remaining balance if any
            if (creditor.amount - settlementAmount > 0.01) {
                creditor.amount -= settlementAmount;
                creditors.offer(creditor);
            }
            if (debtor.amount - settlementAmount > 0.01) {
                debtor.amount -= settlementAmount;
                debtors.offer(debtor);
            }
        }

        return settlements;
    }

    /**
     * Calculate net balance for each user using optimized query with aggregation
     */
    private Map<User, Double> calculateNetBalancesOptimized() {
        Map<User, Double> netBalances = new HashMap<>();

        // Use single query with aggregation instead of loading all pairs
        List<Object[]> balanceSums = userPairRepository.getNetBalances();

        for (Object[] row : balanceSums) {
            User user = (User) row[0];
            Double owedAmount = (Double) row[1];  // What others owe this user
            Double owesAmount = (Double) row[2];  // What this user owes others

            double netBalance = (owedAmount != null ? owedAmount : 0.0) -
                               (owesAmount != null ? owesAmount : 0.0);

            if (Math.abs(netBalance) > 0.01) { // Ignore tiny amounts
                netBalances.put(user, netBalance);
            }
        }

        return netBalances;
    }

    // Helper class for priority queue
    private static class UserBalance {
        User user;
        double amount;

        UserBalance(User user, double amount) {
            this.user = user;
            this.amount = amount;
        }
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
