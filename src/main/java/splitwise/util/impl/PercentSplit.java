package splitwise.util.impl;

import splitwise.model.User;
import splitwise.util.Split;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PercentSplit implements Split {

    @Override
    public Map<User, Double> calculateSplit(double amount, List<User> participants, Map<String, Object> splitDetails) {
        Map<User, Double> percentages = (Map<User, Double>) splitDetails.get("percentages");
        Map<User, Double> splits = new HashMap<>();

        // Validate that percentages sum to 100
        double totalPercentage = 0.0;
        for (User user : participants) {
            double userPercentage = percentages.getOrDefault(user, 0.0);
            totalPercentage += userPercentage;
        }
        
        if (Math.abs(totalPercentage - 100.0) > 0.01) {
            throw new IllegalArgumentException("Percentages must sum to 100%. Current sum: " + totalPercentage);
        }

        for(User user: participants){
            double userPercentage = percentages.getOrDefault(user, 0.0);
            double userAmount = amount * userPercentage / 100.0;
            splits.put(user, userAmount);
        }

        return splits;
    }
}
