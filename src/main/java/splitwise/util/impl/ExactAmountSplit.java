package splitwise.util.impl;

import splitwise.model.User;
import splitwise.util.Split;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExactAmountSplit implements Split {

    @Override
    public Map<User, Double> calculateSplit(double amount, List<User> participants, Map<String, Object> splitDetails) {
        if (splitDetails == null || !splitDetails.containsKey("exactAmounts")) {
            throw new IllegalArgumentException("Exact amounts must be specified in splitDetails");
        }
        
        @SuppressWarnings("unchecked")
        Map<String, Double> exactAmounts = (Map<String, Double>) splitDetails.get("exactAmounts");
        Map<User, Double> result = new HashMap<>();
        double totalSpecified = 0;

        for (User participant : participants) {
            Double userAmount = exactAmounts.get(participant.getUserId());
            if (userAmount == null) {
                throw new IllegalArgumentException("Exact amount not specified for user: " + participant.getUserId());
            }
            if (userAmount < 0) {
                throw new IllegalArgumentException("Amount cannot be negative for user: " + participant.getUserId());
            }
            result.put(participant, userAmount);
            totalSpecified += userAmount;
        }

        if (Math.abs(totalSpecified - amount) > 0.01) {
            throw new IllegalArgumentException(
                String.format("Sum of exact amounts (%.2f) doesn't match total amount (%.2f)", 
                    totalSpecified, amount)
            );
        }
        
        return result;
    }
}
