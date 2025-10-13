package splitwise.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import splitwise.model.Group;
import splitwise.model.User;
import splitwise.model.UserPair;
import splitwise.repository.GroupRepository;
import splitwise.repository.UserPairRepository;
import splitwise.repository.UserRepository;

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
            groupData.put("group", group);
            
            double groupBalance = 0.0;
            List<Map<String, Object>> memberBalances = new ArrayList<>();
            
            for (User member : group.getUserList()) {
                if (!member.equals(user)) {
                    double balance = balanceSheet.getBalance(user, member);
                    
                    Map<String, Object> memberData = new HashMap<>();
                    memberData.put("user", member);
                    memberData.put("balance", balance);
                    memberBalances.add(memberData);
                    
                    groupBalance += balance;
                }
            }
            
            groupData.put("memberBalances", memberBalances);
            groupData.put("totalBalance", groupBalance);
            groupsWithBalances.add(groupData);
            
            totalBalance += groupBalance;
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("groups", groupsWithBalances);
        result.put("totalBalance", totalBalance);
        
        return result;
    }
    
    /**
     * Get all users with whom the current user has balances
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
            userBalances.add(userData);
            
            totalBalance += balance;
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("users", userBalances);
        result.put("totalBalance", totalBalance);
        
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
        dashboard.put("totalBalance", groupBalance + userBalance);
        
        return dashboard;
    }
}