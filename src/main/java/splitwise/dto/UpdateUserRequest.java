package splitwise.dto;

import jakarta.validation.constraints.Email;
import lombok.Data;

@Data
public class UpdateUserRequest {
    
    private String name;
    
    @Email(message = "Email must be valid")
    private String email;
    
    private String mobile;
}