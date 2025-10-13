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
        group.setName(name);
        group.setDescription(description);

        List<User> users = new ArrayList<>();
        for (String userId : userIds) {
            users.add(userService.getUser(userId));
        }
        group.setUserList(users);

        return groupRepository.save(group);
    }

    public Group getGroup(Long groupId) {
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group with ID " + groupId + " not found"));
    }

    public List<Group> getAllGroups() {
        return groupRepository.findAll();
    }

    public Group addUserToGroup(Long groupId, String userId) {
        Group group = getGroup(groupId);
        User user = userService.getUser(userId);

        // Check if user is already in the group
        if (group.getUserList().contains(user)) {
            throw new IllegalArgumentException("User is already a member of this group");
        }

        group.getUserList().add(user);
        return groupRepository.save(group);
    }

    public Group removeUserFromGroup(Long groupId, String userId) {
        Group group = getGroup(groupId);
        User user = userService.getUser(userId);

        // Check if user is in the group
        if (!group.getUserList().contains(user)) {
            throw new IllegalArgumentException("User is not a member of this group");
        }

        // Check if user has zero balance in the group
        for (User otherUser : group.getUserList()) {
            if (!otherUser.equals(user)) {
                double balance = balanceSheet.getBalance(user, otherUser);
                if (Math.abs(balance) > 0.001) {
                    throw new IllegalArgumentException("Cannot remove user from group as they have non-zero balance");
                }
            }
        }

        group.getUserList().remove(user);
        return groupRepository.save(group);
    }

    public void deleteGroup(Long groupId) {
        if (!groupRepository.existsById(groupId)) {
            throw new IllegalArgumentException("Group with ID " + groupId + " not found");
        }
        groupRepository.deleteById(groupId);
    }

    public Group updateGroup(Long groupId, String name, String description) {
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