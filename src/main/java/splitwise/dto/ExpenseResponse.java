package splitwise.dto;

import lombok.Data;
import splitwise.util.SplitTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class ExpenseResponse {
    private String id;
    private String title;
    private String description;
    private Double amount;
    private SplitTypes splitType;
    private UserSummaryDTO payer;
    private List<UserSummaryDTO> participants;
    private Map<String, Double> shares;
    private LocalDateTime timestamp;
    private String groupId;
    private String groupName;
}