package splitwise.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import splitwise.model.Expense;
import splitwise.model.Transaction;
import splitwise.model.User;
import splitwise.model.UserPair;
import splitwise.repository.TransactionRepository;
import splitwise.repository.UserPairRepository;
import splitwise.util.ExpenseObserver;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class BalanceSheet implements ExpenseObserver {

    @Autowired
    private UserPairRepository userPairRepository;
    
    @Autowired
    private TransactionRepository transactionRepository;

    @Override
    public void onExpenseAdded(Expense expense) {
        updateBalances(expense);
    }

    @Override
    public void onExpenseUpdated(Expense expense) {
        updateBalances(expense);
    }

    private void updateBalances(Expense expense) {
        User payer = expense.getPayer();
        Map<User, Double> shares = expense.getShares();

        for (Map.Entry<User, Double> entry : shares.entrySet()) {
            User participant = entry.getKey();
            Double amount = entry.getValue();

            if (!participant.equals(payer)) {
                UserPair userPair = new UserPair(participant, payer);
                
                // Find existing user pair or create a new one
                Optional<UserPair> existingPair = userPairRepository.findByUser1AndUser2(participant, payer);
                
                if (existingPair.isPresent()) {
                    UserPair pair = existingPair.get();
                    pair.setBalance(pair.getBalance() + amount);
                    userPairRepository.save(pair);
                } else {
                    userPair.setBalance(amount);
                    userPairRepository.save(userPair);
                }
                
                System.out.println(userPair.getUser1().getName() + " Will pay " + userPair.getUser2().getName() + " Rs. " + amount);
            }
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

    public List<Transaction> getSimplifiedSettlements() {
        Map<User, Double> netBalances = new HashMap<>();
        List<UserPair> allPairs = userPairRepository.findAll();
        
        for (UserPair pair : allPairs) {
            User debtor = pair.getUser1();
            User creditor = pair.getUser2();
            Double amount = pair.getBalance();
            
            System.out.println("Debtor: " + debtor.getName() + ", Creditor: " + creditor.getName() + ", Amount: " + amount);
            netBalances.put(debtor, netBalances.getOrDefault(debtor, 0.0) - amount);
            netBalances.put(creditor, netBalances.getOrDefault(creditor, 0.0) + amount);
        }

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

    public int getSubOptimalMinimumSettlements() {
        Map<User, Double> netBalances = new HashMap<>();
        List<UserPair> allPairs = userPairRepository.findAll();
        
        for (UserPair pair : allPairs) {
            User debtor = pair.getUser1();
            User creditor = pair.getUser2();
            Double amount = pair.getBalance();

            netBalances.put(debtor, netBalances.getOrDefault(debtor, 0.0) - amount);
            netBalances.put(creditor, netBalances.getOrDefault(creditor, 0.0) + amount);
        }

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
