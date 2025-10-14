package splitwise;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import splitwise.model.Expense;
import splitwise.model.Transaction;
import splitwise.model.User;
import splitwise.service.BalanceSheet;
import splitwise.service.ExpenseManager;
import splitwise.util.Split;
import splitwise.util.SplitFactory;
import splitwise.util.SplitTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SpringBootTest
public class SplitwiseApplicationTests {

    @Test
    void testExpenseManagement() {
        User alice = new User("u1", "Alice", "alice@gmail.com");
        User bob = new User("u2", "Bob", "bob@gmail.com");
        User charlie = new User("u3", "Charlie", "charlie@gmail.com");
        User ram = new User("u4", "Ram", "ram@gmail.com");

        ExpenseManager expenseManager = new ExpenseManager();
        BalanceSheet balanceSheet = new BalanceSheet();

        expenseManager.addObserver(balanceSheet);

        // Create list of users for new expense
        List<User> dinnerParticipants = new ArrayList<>();
        dinnerParticipants.add(alice);
        dinnerParticipants.add(bob);
        dinnerParticipants.add(charlie);
        dinnerParticipants.add(ram);

        Split equalSplit = SplitFactory.createSplit(SplitTypes.EQUAL_SPLIT);
        Map<String, Object> splitDetails = new HashMap<>();
        Map<User, Double> dinnerShares = equalSplit.calculateSplit(1200.0, dinnerParticipants, splitDetails);

        Expense dinnerExpense = new Expense("e1", "Dinner @ Bhandara",SplitTypes.EQUAL_SPLIT, 1200.0, ram, dinnerParticipants, dinnerShares, LocalDateTime.now());

        expenseManager.addExpense(dinnerExpense);

        System.out.println("Individual Balances:");
        System.out.println("Alice total balance: " + balanceSheet.getTotalBalance(alice));
        System.out.println("Bob total balance: " + balanceSheet.getTotalBalance(bob));
        System.out.println("Charlie total balance: " + balanceSheet.getTotalBalance(charlie));
        System.out.println("Ram total balance: " + balanceSheet.getTotalBalance(ram));

        System.out.println("\nPairwise balances");
        System.out.println("Alice & Bob : " + balanceSheet.getBalance(alice, bob));
        System.out.println("Alice & Charlie : " + balanceSheet.getBalance(alice, charlie));
        System.out.println("Alice & Ram : " + balanceSheet.getBalance(alice, ram));
        System.out.println("Bob & Alice : " + balanceSheet.getBalance(bob, alice));
        System.out.println("Bob & Charlie : " + balanceSheet.getBalance(bob, charlie));
        System.out.println("Bob & Ram : " + balanceSheet.getBalance(bob, ram));
        System.out.println("Charlie & Alice : " + balanceSheet.getBalance(charlie, alice));
        System.out.println("Charlie & Bob : " + balanceSheet.getBalance(charlie, bob));
        System.out.println("Charlie & Ram : " + balanceSheet.getBalance(charlie, ram));
        System.out.println("Ram & Alice : " + balanceSheet.getBalance(ram, alice));
        System.out.println("Ram & Bob : " + balanceSheet.getBalance(ram, bob));
        System.out.println("Ram & Charlie : " + balanceSheet.getBalance(ram, charlie));

        List<User> movieParticipants = new ArrayList<>();
        movieParticipants.add(alice);
        movieParticipants.add(bob);
        movieParticipants.add(charlie);

        Map<String, Object> movieSplitDetails = new HashMap<>();
        Map<User, Double> percentages = new HashMap<>();
        percentages.put(alice, 50.0);
        percentages.put(bob, 25.0);
        percentages.put(charlie, 25.0);
        movieSplitDetails.put("percentages", percentages);

        Split percentageSplit = SplitFactory.createSplit(SplitTypes.SPLIT_BY_PERCENTAGES);
        Map<User, Double> movieShares = percentageSplit.calculateSplit(800.0, movieParticipants, movieSplitDetails);

        Expense movieExpense = new Expense("e2", "Movie",SplitTypes.SPLIT_BY_PERCENTAGES, 800.0, bob, movieParticipants, movieShares, LocalDateTime.now());

        expenseManager.addExpense(movieExpense);

        System.out.println("Individual Balances:");
        System.out.println("Alice total balance: " + balanceSheet.getTotalBalance(alice));
        System.out.println("Bob total balance: " + balanceSheet.getTotalBalance(bob));
        System.out.println("Charlie total balance: " + balanceSheet.getTotalBalance(charlie));
        System.out.println("Ram total balance: " + balanceSheet.getTotalBalance(ram));

        System.out.println("\nPairwise balances");
        System.out.println("Alice & Bob : " + balanceSheet.getBalance(alice, bob));
        System.out.println("Alice & Charlie : " + balanceSheet.getBalance(alice, charlie));
        System.out.println("Alice & Ram : " + balanceSheet.getBalance(alice, ram));
        System.out.println("Bob & Alice : " + balanceSheet.getBalance(bob, alice));
        System.out.println("Bob & Charlie : " + balanceSheet.getBalance(bob, charlie));
        System.out.println("Bob & Ram : " + balanceSheet.getBalance(bob, ram));
        System.out.println("Charlie & Alice : " + balanceSheet.getBalance(charlie, alice));
        System.out.println("Charlie & Bob : " + balanceSheet.getBalance(charlie, bob));
        System.out.println("Charlie & Ram : " + balanceSheet.getBalance(charlie, ram));
        System.out.println("Ram & Alice : " + balanceSheet.getBalance(ram, alice));
        System.out.println("Ram & Bob : " + balanceSheet.getBalance(ram, bob));
        System.out.println("Ram & Charlie : " + balanceSheet.getBalance(ram, charlie));

        List<Transaction> settlements = balanceSheet.getSimplifiedSettlements();

        System.out.println("\nOptimal Minimum Settlements ===");
        int optimalSettlements = balanceSheet.getSubOptimalMinimumSettlements();
        System.out.println(optimalSettlements);

        System.out.println("\nSimpliefied Settlements");
        for (Transaction transaction : settlements) {
            System.out.println(transaction.getFrom().getName() + " pays " +
                    transaction.getTo().getName() + " Rs. " +
                    transaction.getAmount());
        }
    }
}