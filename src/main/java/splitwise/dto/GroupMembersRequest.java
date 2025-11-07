package splitwise.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;


@Data
public class GroupMembersRequest {

    @NotEmpty(message = "At least one member is required")
    private List<GroupMember> members;
}