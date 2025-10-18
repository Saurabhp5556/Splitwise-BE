package splitwise.controller;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import splitwise.dto.UpdateUserRequest;
import splitwise.dto.UserResponse;
import splitwise.model.User;
import splitwise.service.DtoMapperService;
import splitwise.service.UserService;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private DtoMapperService dtoMapper;

    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        logger.info("Fetching all users");
        List<User> users = userService.getAllUsers();
        List<UserResponse> response = users.stream()
                .map(dtoMapper::toUserResponse)
                .collect(Collectors.toList());
        logger.info("Found {} users", users.size());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable String userId) {
        logger.info("Fetching user with ID: {}", userId);
        User user = userService.getUser(userId);
        UserResponse response = dtoMapper.toUserResponse(user);
        logger.info("Successfully found user: {}", user.getName());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(Authentication authentication) {
        String userId = authentication.getName();
        logger.info("Fetching current user with ID: {}", userId);
        User user = userService.getUser(userId);
        UserResponse response = dtoMapper.toUserResponse(user);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{userId}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable("userId") String userId,
            @Valid @RequestBody UpdateUserRequest request,
            Authentication authentication) {
        
        // Users can only update their own profile unless they're admin
        String currentUserId = authentication.getName();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        
        if (!currentUserId.equals(userId) && !isAdmin) {
            throw new SecurityException("You can only update your own profile");
        }
        
        logger.info("Updating user with ID: {}", userId);
        User user = userService.updateUser(userId, request.getName(), request.getEmail(), request.getMobile());
        UserResponse response = dtoMapper.toUserResponse(user);
        logger.info("Successfully updated user with ID: {}", userId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(
            @PathVariable("userId") String userId,
            Authentication authentication) {
        
        // Users can only delete their own account unless they're admin
        String currentUserId = authentication.getName();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        
        if (!currentUserId.equals(userId) && !isAdmin) {
            throw new SecurityException("You can only delete your own account");
        }
        
        logger.info("Deleting user with ID: {}", userId);
        userService.deleteUser(userId);
        logger.info("Successfully deleted user with ID: {}", userId);
        return ResponseEntity.noContent().build();
    }
}
