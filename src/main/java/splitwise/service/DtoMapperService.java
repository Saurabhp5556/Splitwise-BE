package splitwise.service;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import splitwise.dto.ExpenseResponse;
import splitwise.dto.UserResponse;
import splitwise.dto.UserSummaryDTO;
import splitwise.model.Expense;
import splitwise.model.User;

import java.util.stream.Collectors;

@Service
public class DtoMapperService {

    @Autowired
    private ModelMapper modelMapper;

    public UserResponse toUserResponse(User user) {
        if (user == null) {
            return null;
        }
        UserResponse dto = new UserResponse();
        dto.setUserId(user.getUserId());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setMobile(user.getMobile());
        dto.setRole(user.getRole());
        return dto;
    }

    public UserSummaryDTO toUserSummaryDTO(User user) {
        if (user == null) {
            return null;
        }
        UserSummaryDTO dto = new UserSummaryDTO();
        dto.setUserId(user.getUserId());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        return dto;
    }

    public ExpenseResponse toExpenseResponse(Expense expense) {
        if (expense == null) {
            return null;
        }
        
        ExpenseResponse response = new ExpenseResponse();
        response.setId(expense.getId());
        response.setTitle(expense.getTitle());
        response.setDescription(expense.getDescription());
        response.setAmount(expense.getAmount());
        response.setSplitType(expense.getSplitType());
        response.setTimestamp(expense.getTimestamp());
        response.setGroupId(expense.getGroup() != null ? expense.getGroup().getGroupId() : null);
        response.setIsSettleUp(expense.getIsSettleUp());
        response.setSplitDetails(expense.getSplitDetails());

        // Map payer
        if (expense.getPayer() != null) {
            response.setPayer(toUserSummaryDTO(expense.getPayer()));
        }
        
        // Map participants
        if (expense.getParticipants() != null) {
            response.setParticipants(
                expense.getParticipants().stream()
                    .map(this::toUserSummaryDTO)
                    .collect(Collectors.toList())
            );
        }
        
        // Map shares - convert User keys to String (userId)
        if (expense.getShares() != null) {
            response.setShares(expense.getSharesForJson());
        }
        
        // Map group name if available
        if (expense.getGroup() != null) {
            response.setGroupName(expense.getGroup().getName());
        }
        
        return response;
    }
}