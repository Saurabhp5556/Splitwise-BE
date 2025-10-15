package splitwise.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import splitwise.model.User;
import splitwise.service.UserService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Healthy");
    }

    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody Map<String, String> request) {
        logger.info("Creating user with request: {}", request);
        
        String id = request.get("userId");
        String name = request.get("name");
        String email = request.get("email");
        String mobile = request.get("mobile");

        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID is required and cannot be empty");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name is required and cannot be empty");
        }
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required and cannot be empty");
        }

        User user = userService.createUser(id, name, email, mobile);
        logger.info("Successfully created user with ID: {}", user.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        logger.info("Fetching all users");
        List<User> users = userService.getAllUsers();
        logger.info("Found {} users", users.size());
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<User> getUserById(@PathVariable String userId) {
        logger.info("Fetching user with ID: {}", userId);
        User user = userService.getUser(userId);
        logger.info("Successfully found user: {}", user.getName());
        return ResponseEntity.ok(user);
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable("userId") String userId) {
        logger.info("Deleting user with ID: {}", userId);
        userService.deleteUser(userId);
        logger.info("Successfully deleted user with ID: {}", userId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{userId}")
    public ResponseEntity<User> updateUser(@PathVariable("userId") String userId, @RequestBody Map<String, String> request) {
        logger.info("Updating user with ID: {} with request: {}", userId, request);
        
        String name = request.get("name");
        String email = request.get("email");
        String mobile = request.get("mobile");

        User user = userService.updateUser(userId, name, email, mobile);
        logger.info("Successfully updated user with ID: {}", userId);
        return ResponseEntity.ok(user);
    }
}
