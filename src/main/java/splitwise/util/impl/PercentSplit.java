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

        for(User user: participants){
            double userAmount = amount * percentages.getOrDefault(user, 0.0) /100;
            splits.put(user, userAmount);
        }

        return splits;
    }
}
