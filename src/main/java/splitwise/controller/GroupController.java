package splitwise.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import splitwise.model.Group;
import splitwise.service.GroupService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/groups")
public class GroupController {

    @Autowired
    private GroupService groupService;

    @PostMapping
    public ResponseEntity<Group> createGroup(@RequestBody Map<String, Object> request) {
        try {
            String name = (String) request.get("name");
            String description = (String) request.get("description");
            @SuppressWarnings("unchecked")
            List<String> userIds = (List<String>) request.get("userIds");

            if (name == null || userIds == null || userIds.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            Group group = groupService.createGroup(name, description, userIds);
            return ResponseEntity.status(HttpStatus.CREATED).body(group);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping
    public ResponseEntity<List<Group>> getAllGroups() {
        return ResponseEntity.ok(groupService.getAllGroups());
    }

    @GetMapping("/{groupId}")
    public ResponseEntity<Group> getGroupById(@PathVariable("groupId") Long groupId) {
        try {
            Group group = groupService.getGroup(groupId);
            return ResponseEntity.ok(group);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{groupId}/users")
    public ResponseEntity<Group> addUserToGroup(@PathVariable("groupId") Long groupId, @RequestBody Map<String, String> request) {
        try {
            String userId = request.get("userId");
            if (userId == null) {
                System.out.println("userId is null");
                return ResponseEntity.badRequest().build();
            }

            Group group = groupService.addUserToGroup(groupId, userId);
            return ResponseEntity.ok(group);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    @DeleteMapping("/{groupId}/users/{userId}")
    public ResponseEntity<?> removeUserFromGroup(@PathVariable("groupId") Long groupId, @PathVariable("userId") String userId) {
        try {
            Group group = groupService.removeUserFromGroup(groupId, userId);
            return ResponseEntity.ok(group);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{groupId}")
    public ResponseEntity<Void> deleteGroup(@PathVariable("groupId") Long groupId) {
        try {
            groupService.deleteGroup(groupId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{groupId}")
    public ResponseEntity<Group> updateGroup(@PathVariable("groupId") Long groupId, @RequestBody Map<String, String> request) {
        try {
            String name = request.get("name");
            String description = request.get("description");

            Group group = groupService.updateGroup(groupId, name, description);
            return ResponseEntity.ok(group);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
