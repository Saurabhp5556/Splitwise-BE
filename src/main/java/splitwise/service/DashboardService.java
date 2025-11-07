package splitwise.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import splitwise.model.Expense;
import splitwise.model.Group;
import splitwise.model.User;
import splitwise.model.UserPair;
import splitwise.repository.ExpenseRepository;
import splitwise.repository.UserPairRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardService {

    @Autowired
    private UserService userService;

    @Autowired
    private GroupService groupService;

    @Autowired
    private BalanceSheet balanceSheet;

    @Autowired
    private UserPairRepository userPairRepository;
    
    @Autowired
    private ExpenseRepository expenseRepository;

    /**
     * Get all groups for a user with balance information
     */
    public Map<String, Object> getUserGroupsWithBalances(String userId) {
        User user = userService.getUser(userId);
        List<Group> userGroups = groupService.getGroupsByUserId(userId);
        
        List<Map<String, Object>> groupsWithBalances = new ArrayList<>();
        double totalBalance = 0.0;
        
        for (Group group : userGroups) {
            Map<String, Object> groupData = new HashMap<>();
            
            // Calculate balances based only on expenses within this group
            Map<User, Double> memberBalancesMap = calculateGroupBalances(user, group);
            
            double groupBalance = 0.0;
            List<Map<String, Object>> memberBalances = new ArrayList<>();
            
            for (Map.Entry<User, Double> entry : memberBalancesMap.entrySet()) {
                User member = entry.getKey();
                double balance = entry.getValue();
                
                Map<String, Object> memberData = new HashMap<>();
                memberData.put("user", member);
                memberData.put("balance", balance);
                
                // Add descriptive information
                if (balance > 0) {
                    memberData.put("balanceType", "gets_back");
                    memberData.put("description", member.getName() + " owes you");
                } else if (balance < 0) {
                    memberData.put("balanceType", "owes");
                    memberData.put("description", "You owe " + member.getName());
                } else {
                    memberData.put("balanceType", "settled");
                    memberData.put("description", "Settled up");
                }
                
                memberBalances.add(memberData);
                groupBalance += balance;

            }
            
            groupData.put("memberBalances", memberBalances);
            groupData.put("totalBalance", groupBalance);

            group.setUserList(group.getUserList());

            groupData.put("group", group);
            
            // Add descriptive information for group total balance
            if (groupBalance > 0) {
                groupData.put("balanceType", "gets_back");
                groupData.put("description", "You get back from this group");
            } else if (groupBalance < 0) {
                groupData.put("balanceType", "owes");
                groupData.put("description", "You owe to this group");
            } else {
                groupData.put("balanceType", "settled");
                groupData.put("description", "All settled in this group");
            }
            
            groupsWithBalances.add(groupData);
            totalBalance += groupBalance;
        }
        
        // Use BalanceSheet to get the correct overall balance from UserPair table
        double overallBalance = balanceSheet.getTotalBalance(user);
        
        Map<String, Object> result = new HashMap<>();
        result.put("groups", groupsWithBalances);
        result.put("totalBalance", overallBalance);  // Overall balance from UserPair table
        
        // Add descriptive information for overall balance
        if (overallBalance > 0) {
            result.put("balanceType", "gets_back");
            result.put("description", "You get back overall");
        } else if (overallBalance < 0) {
            result.put("balanceType", "owes");
            result.put("description", "You owe overall");
        } else {
            result.put("balanceType", "settled");
            result.put("description", "All settled");
        }
        
        return result;
    }
    
    /**
     * Get all friends with aggregated balance breakdown across groups and non-group expenses
     * Uses UserPair table for accurate balance calculation and properly categorizes expenses
     */
    public Map<String, Object> getUserFriendsWithTransactions(String userId) {
        User user = userService.getUser(userId);
        List<UserPair> userPairs = userPairRepository.findByUser(user);
        
        // Map to store friend data: friendUserId -> friend data
        Map<String, Map<String, Object>> friendsMap = new HashMap<>();
        
        // Build friend map from UserPair (this has the correct total balances)
        for (UserPair pair : userPairs) {
            User friend;
            double totalBalance;
            
            if (pair.getUser1().equals(user)) {
                friend = pair.getUser2();
                totalBalance = -pair.getBalance(); // Negative because user1 owes user2
            } else {
                friend = pair.getUser1();
                totalBalance = pair.getBalance(); // Positive because user2 owes user1
            }
            
            // Skip if balance is essentially zero
            if (Math.abs(totalBalance) < 0.01) {
                continue;
            }
            
            Map<String, Object> friendData = new HashMap<>();
            friendData.put("name", friend.getName());
            friendData.put("userId", friend.getUserId());
            friendData.put("totalBalance", totalBalance);
            friendData.put("groupBalances", new HashMap<String, Double>());
            friendData.put("nonGroupBalance", 0.0);
            
            friendsMap.put(friend.getUserId(), friendData);
        }
        
        // Get ALL expenses involving this user to properly categorize them
        List<Expense> allExpenses = expenseRepository.findByParticipantId(userId);
        
        // Process each expense and categorize by group or non-group
        for (Expense expense : allExpenses) {
            User payer = expense.getPayer();
            Map<User, Double> shares = expense.getShares();
            Group group = expense.getGroup();
            
            // Determine the friend and balance for this expense
            User friend = null;
            double balance = 0.0;
            
            if (payer.equals(user)) {
                // Current user is payer, find who owes them
                for (Map.Entry<User, Double> entry : shares.entrySet()) {
                    if (!entry.getKey().equals(user)) {
                        friend = entry.getKey();
                        balance = entry.getValue(); // Positive: they owe user
                        break;
                    }
                }
            } else if (shares.containsKey(user)) {
                // Current user owes the payer
                friend = payer;
                balance = -shares.get(user); // Negative: user owes them
            }
            
            // If we found a friend relationship in this expense
            if (friend != null && Math.abs(balance) >= 0.01) {
                Map<String, Object> friendData = friendsMap.get(friend.getUserId());
                if (friendData != null) {
                    @SuppressWarnings("unchecked")
                    Map<String, Double> friendGroupBalances = (Map<String, Double>) friendData.get("groupBalances");
                    
                    if (group != null) {
                        // This is a group expense - add to group balances
                        String groupName = group.getName();
                        friendGroupBalances.put(groupName,
                            friendGroupBalances.getOrDefault(groupName, 0.0) + balance);
                    } else {
                        // This is a non-group expense - add to non-group balance
                        double currentNonGroupBalance = (double) friendData.get("nonGroupBalance");
                        friendData.put("nonGroupBalance", currentNonGroupBalance + balance);
                    }
                }
            }
        }
        
        // Convert map to list and build transaction list from aggregated data
        List<Map<String, Object>> friendsList = new ArrayList<>();
        double overallBalance = 0.0;
        
        for (Map<String, Object> friendData : friendsMap.values()) {
            // Recalculate total balance from actual transactions to ensure accuracy
            @SuppressWarnings("unchecked")
            Map<String, Double> friendGroupBalances = (Map<String, Double>) friendData.get("groupBalances");
            double nonGroupBalance = (double) friendData.get("nonGroupBalance");
            
            // Calculate actual total from group and non-group balances
            double calculatedTotal = nonGroupBalance;
            for (double groupBalance : friendGroupBalances.values()) {
                calculatedTotal += groupBalance;
            }
            
            // Use calculated total instead of UserPair total for accuracy
            double totalBalance = calculatedTotal;
            friendData.put("totalBalance", totalBalance);
            
            // Build transactions list from aggregated data
            List<Map<String, Object>> transactions = new ArrayList<>();
            
            // Add group transactions
            for (Map.Entry<String, Double> groupEntry : friendGroupBalances.entrySet()) {
                double groupBalance = groupEntry.getValue();
                if (Math.abs(groupBalance) >= 0.01) {
                    Map<String, Object> transaction = new HashMap<>();
                    transaction.put("type", "group");
                    transaction.put("balance", Math.abs(groupBalance));
                    transaction.put("balanceType", groupBalance > 0 ? "gets_back" : "owes");
                    transaction.put("name", groupEntry.getKey());
                    transactions.add(transaction);
                }
            }
            
            // Add non-group transaction if exists
            if (Math.abs(nonGroupBalance) >= 0.01) {
                Map<String, Object> transaction = new HashMap<>();
                transaction.put("type", "non-group");
                transaction.put("balance", Math.abs(nonGroupBalance));
                transaction.put("balanceType", nonGroupBalance > 0 ? "gets_back" : "owes");
                transaction.put("name", null);
                transactions.add(transaction);
            }
            
            friendData.put("transactions", transactions);
            
            // Remove temporary aggregation maps
            friendData.remove("groupBalances");
            friendData.remove("nonGroupBalance");
            
            // Add balance type for the friend
            if (totalBalance > 0) {
                friendData.put("balanceType", "gets_back");
            } else if (totalBalance < 0) {
                friendData.put("balanceType", "owes");
                friendData.put("totalBalance", Math.abs(totalBalance));
            } else {
                friendData.put("balanceType", "settled");
            }
            
            friendsList.add(friendData);
            overallBalance += totalBalance;
        }
        
        // Build result
        Map<String, Object> result = new HashMap<>();
        result.put("users", friendsList);
        result.put("totalBalance", overallBalance);
        
        if (overallBalance > 0) {
            result.put("balanceType", "gets_back");
            result.put("description", "You get back overall");
        } else if (overallBalance < 0) {
            result.put("balanceType", "owes");
            result.put("description", "You owe overall");
        } else {
            result.put("balanceType", "settled");
            result.put("description", "All settled");
        }
        
        return result;
    }
    
    /**
     * Get all users with whom the current user has balances (legacy method)
     */
    public Map<String, Object> getUserBalances(String userId) {
        User user = userService.getUser(userId);
        List<UserPair> userPairs = userPairRepository.findByUser(user);
        
        List<Map<String, Object>> userBalances = new ArrayList<>();
        double totalBalance = 0.0;
        
        for (UserPair pair : userPairs) {
            Map<String, Object> userData = new HashMap<>();
            
            User otherUser;
            double balance;
            
            if (pair.getUser1().equals(user)) {
                otherUser = pair.getUser2();
                balance = -pair.getBalance(); // Negative because user1 owes user2
            } else {
                otherUser = pair.getUser1();
                balance = pair.getBalance(); // Positive because user2 owes user1
            }
            
            userData.put("user", otherUser);
            userData.put("balance", balance);
            
            // Add descriptive information
            if (balance > 0) {
                userData.put("balanceType", "gets_back");
                userData.put("description", otherUser.getName() + " owes you");
            } else if (balance < 0) {
                userData.put("balanceType", "owes");
                userData.put("description", "You owe " + otherUser.getName());
            } else {
                userData.put("balanceType", "settled");
                userData.put("description", "Settled up");
            }
            
            userBalances.add(userData);
            totalBalance += balance;
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("users", userBalances);
        result.put("totalBalance", totalBalance);
        
        // Add descriptive information for overall balance
        if (totalBalance > 0) {
            result.put("balanceType", "gets_back");
            result.put("description", "You get back overall");
        } else if (totalBalance < 0) {
            result.put("balanceType", "owes");
            result.put("description", "You owe overall");
        } else {
            result.put("balanceType", "settled");
            result.put("description", "All settled");
        }
        
        return result;
    }
    
    /**
     * Get complete dashboard data for a user
     */
    public Map<String, Object> getDashboardData(String userId) {
        Map<String, Object> dashboard = new HashMap<>();
        
        // Get user details
        User user = userService.getUser(userId);
        dashboard.put("user", user);
        
        // Get group balances
        Map<String, Object> groupData = getUserGroupsWithBalances(userId);
        dashboard.put("groups", groupData.get("groups"));
        
        // Get user balances
        Map<String, Object> userData = getUserBalances(userId);
        dashboard.put("users", userData.get("users"));
        
        // Calculate overall balance
        double groupBalance = (double) groupData.get("totalBalance");
        double userBalance = (double) userData.get("totalBalance");
        double overallBalance = groupBalance + userBalance;
        dashboard.put("totalBalance", overallBalance);
        
        // Add descriptive information for overall balance
        if (overallBalance > 0) {
            dashboard.put("balanceType", "gets_back");
            dashboard.put("description", "You get back overall");
        } else if (overallBalance < 0) {
            dashboard.put("balanceType", "owes");
            dashboard.put("description", "You owe overall");
        } else {
            dashboard.put("balanceType", "settled");
            dashboard.put("description", "All settled");
        }
        
        return dashboard;
    }
    
    /**
     * Calculate balances for a user within a specific group based only on expenses in that group.
     * This ensures group balances are isolated and don't include expenses from other contexts.
     *
     * @param user The current user
     * @param group The group to calculate balances for
     * @return Map of other users to their balance with the current user (positive = they owe user, negative = user owes them)
     */
    private Map<User, Double> calculateGroupBalances(User user, Group group) {
        Map<User, Double> balances = new HashMap<>();
        
        // Get all expenses for this group
        List<Expense> groupExpenses = expenseRepository.findByGroup(group);
        
        // Calculate balances from each expense
        for (Expense expense : groupExpenses) {
            User payer = expense.getPayer();
            Map<User, Double> shares = expense.getShares();
            
            // If current user is the payer, others owe them
            if (payer.equals(user)) {
                for (Map.Entry<User, Double> entry : shares.entrySet()) {
                    User participant = entry.getKey();
                    Double amount = entry.getValue();
                    
                    // Skip the payer themselves
                    if (!participant.equals(user)) {
                        balances.put(participant, balances.getOrDefault(participant, 0.0) + amount);
                    }
                }
            }
            // If current user is a participant (not payer), they owe the payer
            else if (shares.containsKey(user)) {
                Double amountOwed = shares.get(user);
                balances.put(payer, balances.getOrDefault(payer, 0.0) - amountOwed);
            }
        }
        
        return balances;
    }
}