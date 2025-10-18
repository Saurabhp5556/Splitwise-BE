package splitwise.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import splitwise.util.SplitTypes;

import java.util.List;
import java.util.Map;

@Data
public class CreateExpenseRequest {
    
    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private Double amount;

    @NotBlank(message = "Title is required")
    private String title;
    
    private String description;

    @NotNull(message = "Payer ID is required")
    private String payerId;

    @NotEmpty(message = "At least one participant is required")
    private List<String> participantIds;

    @NotNull(message = "Split type is required")
    private SplitTypes splitType;

    private Map<String, Object> splitDetails;
    
    private String groupId;
    
    private Boolean isSettleUp;
}