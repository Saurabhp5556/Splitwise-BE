package splitwise.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import splitwise.model.Group;
import splitwise.service.GroupService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/groups")
public class GroupController {

    private static final Logger logger = LoggerFactory.getLogger(GroupController.class);

    @Autowired
    private GroupService groupService;

    @PostMapping
    public ResponseEntity<Group> createGroup(@RequestBody Map<String, Object> request, Authentication authentication) {
        logger.info("Creating group with request: {}", request);
        
        String name = (String) request.get("name");
        String description = (String) request.get("description");
        @SuppressWarnings("unchecked")
        List<String> userIds = (List<String>) request.get("userIds");

        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Group name is required and cannot be empty");
        }

        String currentUserId = authentication.getName();
        userIds = (userIds == null) ? new ArrayList<>() : new ArrayList<>(userIds);
        if (!userIds.contains(currentUserId)) {
            userIds.add(currentUserId);
        }

        Group group = groupService.createGroup(name, description, userIds,currentUserId);
        logger.info("Successfully created group with ID: {} and name: {}", group.getGroupId(), group.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(group);
    }

    @GetMapping
    public ResponseEntity<List<Group>> getAllGroups() {
        logger.info("Fetching all groups");
        List<Group> groups = groupService.getAllGroups();
        logger.info("Found {} groups", groups.size());
        return ResponseEntity.ok(groups);
    }

    @GetMapping("/{groupId}")
    public ResponseEntity<Group> getGroupById(@PathVariable String groupId) {
        logger.info("Fetching group with ID: {}", groupId);
        Group group = groupService.getGroup(groupId);
        logger.info("Successfully found group: {}", group.getName());
        return ResponseEntity.ok(group);
    }

    @PostMapping("/{groupId}/users")
    public ResponseEntity<Group> addUserToGroup(@PathVariable String groupId, @RequestBody Map<String, String> request) {
        logger.info("Adding user to group {} with request: {}", groupId, request);
        
        String userId = request.get("userId");
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID is required and cannot be empty");
        }

        Group group = groupService.addUserToGroup(groupId, userId);
        logger.info("Successfully added user {} to group {}", userId, groupId);
        return ResponseEntity.ok(group);
    }

    @DeleteMapping("/{groupId}/users/{userId}")
    public ResponseEntity<Group> removeUserFromGroup(@PathVariable String groupId, @PathVariable String userId) {
        logger.info("Removing user {} from group {}", userId, groupId);
        
        Group group = groupService.removeUserFromGroup(groupId, userId);
        logger.info("Successfully removed user {} from group {}", userId, groupId);
        return ResponseEntity.ok(group);
    }

    @DeleteMapping("/{groupId}")
    public ResponseEntity<Void> deleteGroup(@PathVariable String groupId) {
        logger.info("Deleting group with ID: {}", groupId);
        groupService.deleteGroup(groupId);
        logger.info("Successfully deleted group with ID: {}", groupId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{groupId}")
    public ResponseEntity<Group> updateGroup(@PathVariable String groupId, @RequestBody Map<String, String> request) {
        logger.info("Updating group {} with request: {}", groupId, request);
        
        String name = request.get("name");
        String description = request.get("description");

        Group group = groupService.updateGroup(groupId, name, description);
        logger.info("Successfully updated group with ID: {}", groupId);
        return ResponseEntity.ok(group);
    }
}
