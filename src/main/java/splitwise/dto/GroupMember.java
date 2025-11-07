package splitwise.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import splitwise.util.validators.EitherPhoneOrEmailRequired;

@Data
@EitherPhoneOrEmailRequired
public class GroupMember {

    private String userId;

    @NotBlank(message = "Name is required")
    private String name;

    private String mobile;

    private String email;

}
