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

    public Group createGroup(String name, String description, List<String> userIds) {
        Group group = new Group();
        
        // Generate a unique group ID
        String groupId = generateGroupId();
        group.setGroupId(groupId);
        group.setName(name);
        group.setDescription(description);

        List<User> users = new ArrayList<>();
        for (String userId : userIds) {
            users.add(userService.getUser(userId));
        }
        group.setUserList(users);

        return groupRepository.save(group);
    }
    
    private String generateGroupId() {
        // Find the highest existing group ID number
        List<Group> allGroups = groupRepository.findAll();
        int maxId = 0;
        
        for (Group group : allGroups) {
            String groupId = group.getGroupId();
            if (groupId != null && groupId.startsWith("g")) {
                try {
                    int id = Integer.parseInt(groupId.substring(1));
                    maxId = Math.max(maxId, id);
                } catch (NumberFormatException e) {
                    // Ignore invalid group IDs
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
    
    private void validateUserCanLeaveGroup(User user, Group group) {
        List<String> balanceIssues = new ArrayList<>();
        double totalGroupBalance = 0.0;
        
        for (User otherUser : group.getUserList()) {
            if (!otherUser.equals(user)) {
                double balance = balanceSheet.getBalance(user, otherUser);
                if (Math.abs(balance) > 0.001) {
                    String balanceDescription;
                    if (balance > 0) {
                        balanceDescription = String.format("%s owes you Rs. %.2f", otherUser.getName(), balance);
                    } else {
                        balanceDescription = String.format("You owe %s Rs. %.2f", otherUser.getName(), Math.abs(balance));
                    }
                    balanceIssues.add(balanceDescription);
                    totalGroupBalance += balance;
                }
            }
        }
        
        if (!balanceIssues.isEmpty()) {
            String errorMessage = String.format(
                "Cannot remove user '%s' from group '%s' due to unsettled balances:\n%s\n\nTotal balance: Rs. %.2f\n\nPlease settle all balances before leaving the group.",
                user.getName(),
                group.getName(),
                String.join("\n", balanceIssues),
                totalGroupBalance
            );
            throw new IllegalArgumentException(errorMessage);
        }
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