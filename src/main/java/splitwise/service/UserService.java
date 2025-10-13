package splitwise.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import splitwise.model.User;
import splitwise.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
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
        userRepository.deleteById(id);
    }
    
    public User updateUser(String id, String name, String email, String mobile) {
        User user = getUser(id);
        
        if (name != null) {
            user.setName(name);
        }
        
        if (email != null) {
            user.setEmail(email);
        }
        
        if (mobile != null) {
            user.setMobile(mobile);
        }
        
        return userRepository.save(user);
    }
}