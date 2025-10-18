package splitwise.util.impl;

import splitwise.model.User;
import splitwise.util.Split;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdjustmentSplit implements Split {

    @Override
    public Map<User, Double> calculateSplit(double amount, List<User> participants, Map<String, Object> splitDetails) {
        // Handle both User objects and String user IDs as keys
        Map<User, Double> adjustments = new HashMap<>();
        
        if (splitDetails != null && splitDetails.containsKey("adjustments")) {
            Object adjustmentsObj = splitDetails.get("adjustments");
            
            if (adjustmentsObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<Object, Double> rawAdjustments = (Map<Object, Double>) adjustmentsObj;
                
                for (Map.Entry<Object, Double> entry : rawAdjustments.entrySet()) {
                    if (entry.getKey() instanceof User) {
                        adjustments.put((User) entry.getKey(), entry.getValue());
                    } else if (entry.getKey() instanceof String) {
                        // Find the user by ID from participants
                        String userId = (String) entry.getKey();
                        User user = participants.stream()
                            .filter(p -> p.getUserId().equals(userId))
                            .findFirst()
                            .orElseThrow(() -> new IllegalArgumentException("User " + userId + " not found in participants"));
                        adjustments.put(user, entry.getValue());
                    }
                }
            }
        }

        Map<User, Double> result = new HashMap<>();
        double totalAdjustments = 0;

        // Calculate total adjustments
        for (User participant : participants) {
            Double adjustment = adjustments.get(participant);
            if (adjustment != null) {
                totalAdjustments += adjustment;
            }
        }

        // Calculate base amount (remaining after adjustments)
        double remainingAmount = amount - totalAdjustments;
        if (remainingAmount < 0) {
            throw new IllegalArgumentException(
                String.format("Adjustments (%.2f) exceed total amount (%.2f)",
                    totalAdjustments, amount)
            );
        }

        // Split remaining amount equally
        double equalShare = remainingAmount / participants.size();

        // Apply equal share + adjustments
        for (User participant : participants) {
            Double adjustment = adjustments.get(participant);
            double userShare = equalShare + (adjustment != null ? adjustment : 0);
            
            if (userShare < 0) {
                throw new IllegalArgumentException(
                    String.format("Calculated share for user %s is negative: %.2f",
                        participant.getUserId(), userShare)
                );
            }
            
            result.put(participant, userShare);
        }

        return result;
    }
}
