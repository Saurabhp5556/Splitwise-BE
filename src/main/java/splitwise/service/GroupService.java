package splitwise.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import splitwise.model.Group;
import splitwise.model.User;
import splitwise.repository.GroupRepository;

import java.util.ArrayList;
import java.util.List;

@Service
public class GroupService {

    @Autowired
    private UserService userService;

    @Autowired
    private BalanceSheet balanceSheet;
    
    @Autowired
    private GroupRepository groupRepository;

    /**
     * Creates a new group with the specified name, description, and initial members.
     * Automatically generates a unique string-based group ID (e.g., "g1", "g2", etc.).
     *
     * @param name The name of the group
     * @param description Optional description of the group
     * @param userIds List of user IDs to add as initial group members
     * @return The created group with generated ID
     */
    public Group createGroup(String name, String description, List<String> userIds) {
        Group group = new Group();
        
        // Generate a unique group ID in format "g1", "g2", etc.
        String groupId = generateGroupId();
        group.setGroupId(groupId);
        group.setName(name);
        group.setDescription(description);

        // Convert user IDs to User objects and add to group
        List<User> users = new ArrayList<>();
        for (String userId : userIds) {
            users.add(userService.getUser(userId));
        }
        group.setUserList(users);

        return groupRepository.save(group);
    }
    
    /**
     * Generates a unique group ID in the format "g{number}" (e.g., "g1", "g2", "g3").
     * Finds the highest existing group ID number and increments it by 1.
     *
     * @return A unique group ID string
     */
    private String generateGroupId() {
        List<Group> allGroups = groupRepository.findAll();
        int maxId = 0;
        
        // Find the highest existing group ID number
        for (Group group : allGroups) {
            String groupId = group.getGroupId();
            if (groupId != null && groupId.startsWith("g")) {
                try {
                    int id = Integer.parseInt(groupId.substring(1));
                    maxId = Math.max(maxId, id);
                } catch (NumberFormatException e) {
                    // Skip invalid group IDs that don't follow the "g{number}" format
                }
            }
        }
        
        return "g" + (maxId + 1);
    }

    public Group getGroup(String groupId) {
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group with ID " + groupId + " not found"));
    }

    public List<Group> getAllGroups() {
        return groupRepository.findAll();
    }

    public Group addUserToGroup(String groupId, String userId) {
        Group group = getGroup(groupId);
        User user = userService.getUser(userId);

        // Check if user is already in the group
        if (group.getUserList().contains(user)) {
            throw new IllegalArgumentException("User is already a member of this group");
        }

        group.getUserList().add(user);
        return groupRepository.save(group);
    }

    public Group removeUserFromGroup(String groupId, String userId) {
        Group group = getGroup(groupId);
        User user = userService.getUser(userId);

        // Check if user is in the group
        if (!group.getUserList().contains(user)) {
            throw new IllegalArgumentException("User is not a member of this group");
        }

        // Check if user has unsettled balances with other group members
        validateUserCanLeaveGroup(user, group);

        group.getUserList().remove(user);
        return groupRepository.save(group);
    }
    
    /**
     * Validates whether a user can leave a group by checking for unsettled balances.
     *
     * A user cannot leave a group if they have any outstanding financial obligations
     * with other group members (either owing money or being owed money).
     *
     * @param user The user attempting to leave the group
     * @param group The group the user wants to leave
     * @throws IllegalArgumentException if the user has unsettled balances
     */
    private void validateUserCanLeaveGroup(User user, Group group) {
        List<String> balanceIssues = new ArrayList<>();
        double totalGroupBalance = 0.0;
        
        // Check balance with each other group member
        for (User otherUser : group.getUserList()) {
            if (!otherUser.equals(user)) {
                double balance = balanceSheet.getBalance(user, otherUser);
                
                // Consider balances above 0.001 as significant (handles floating point precision)
                if (Math.abs(balance) > 0.001) {
                    String balanceDescription = formatBalanceDescription(user, otherUser, balance);
                    balanceIssues.add(balanceDescription);
                    totalGroupBalance += balance;
                }
            }
        }
        
        // If any unsettled balances exist, prevent the user from leaving
        if (!balanceIssues.isEmpty()) {
            String errorMessage = createBalanceErrorMessage(user, group, balanceIssues, totalGroupBalance);
            throw new IllegalArgumentException(errorMessage);
        }
    }
    
    /**
     * Formats a balance description for display in error messages.
     */
    private String formatBalanceDescription(User user, User otherUser, double balance) {
        if (balance > 0) {
            return String.format("%s owes you Rs. %.2f", otherUser.getName(), balance);
        } else {
            return String.format("You owe %s Rs. %.2f", otherUser.getName(), Math.abs(balance));
        }
    }
    
    /**
     * Creates a comprehensive error message when a user cannot leave due to unsettled balances.
     */
    private String createBalanceErrorMessage(User user, Group group, List<String> balanceIssues, double totalBalance) {
        return String.format(
            "Cannot remove user '%s' from group '%s' due to unsettled balances:\n%s\n\nTotal balance: Rs. %.2f\n\nPlease settle all balances before leaving the group.",
            user.getName(),
            group.getName(),
            String.join("\n", balanceIssues),
            totalBalance
        );
    }

    public void deleteGroup(String groupId) {
        if (!groupRepository.existsById(groupId)) {
            throw new IllegalArgumentException("Group with ID " + groupId + " not found");
        }
        groupRepository.deleteById(groupId);
    }

    public Group updateGroup(String groupId, String name, String description) {
        Group group = getGroup(groupId);
        
        if (name != null) {
            group.setName(name);
        }
        
        if (description != null) {
            group.setDescription(description);
        }
        
        return groupRepository.save(group);
    }
    
    public List<Group> getGroupsByUserId(String userId) {
        return groupRepository.findGroupsByUserId(userId);
    }
}