package splitwise.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import splitwise.model.Group;
import splitwise.model.User;
import splitwise.repository.GroupRepository;
import splitwise.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private GroupRepository groupRepository;
    
    @Autowired
    private BalanceSheet balanceSheet;
    
    public User createUser(String id, String name, String email, String mobile) {
        if (userRepository.existsById(id)) {
            throw new IllegalArgumentException("User with ID " + id + " already exists");
        }
        
        User user = new User(id, name, email);
        user.setMobile(mobile);
        return userRepository.save(user);
    }
    
    public User getUser(String id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User with ID " + id + " not found"));
    }
    
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
    
    public void deleteUser(String id) {
        if (!userRepository.existsById(id)) {
            throw new IllegalArgumentException("User with ID " + id + " not found");
        }
        
        User user = getUser(id);
        
        // Check if user has unsettled balances
        validateUserCanBeDeleted(user);
        
        userRepository.deleteById(id);
    }
    
    private void validateUserCanBeDeleted(User user) {
        List<String> issues = new ArrayList<>();
        
        // Check for unsettled balances
        double totalBalance = balanceSheet.getTotalBalance(user);
        if (Math.abs(totalBalance) > 0.001) {
            issues.add("User has unsettled balance of Rs. " + String.format("%.2f", totalBalance));
        }
        
        // Check for group memberships
        List<Group> userGroups = groupRepository.findGroupsByUserId(user.getUserId());
        if (!userGroups.isEmpty()) {
            List<String> groupNames = userGroups.stream()
                    .map(Group::getName)
                    .toList();
            issues.add("User is a member of " + userGroups.size() + " group(s): " + String.join(", ", groupNames));
        }
        
        if (!issues.isEmpty()) {
            String errorMessage = "Cannot delete user '" + user.getName() + "'. Issues found:\n" +
                    String.join("\n", issues) +
                    "\n\nPlease settle all balances and remove the user from all groups before deletion.";
            throw new IllegalArgumentException(errorMessage);
        }
    }
    
    public User updateUser(String id, String name, String email, String mobile) {
        User user = getUser(id);
        
        if (name != null && !name.equals(user.getName())) {
            user.setName(name);
        }
        
        if (email != null && !email.equals(user.getEmail())) {
            // Check if the new email is already taken by another user
            if (userRepository.findByEmail(email).isPresent()) {
                throw new IllegalArgumentException("Email " + email + " is already taken by another user");
            }
            user.setEmail(email);
        }
        
        if (mobile != null && !mobile.equals(user.getMobile())) {
            user.setMobile(mobile);
        }
        
        return userRepository.save(user);
    }
}