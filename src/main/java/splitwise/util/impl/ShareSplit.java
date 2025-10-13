package splitwise.util.impl;

import splitwise.model.User;
import splitwise.util.Split;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShareSplit implements Split {
    @Override
    public Map<User, Double> calculateSplit(double amount, List<User> participants, Map<String, Object> splitDetails) {
        Map<User, Double> shares = (Map<User, Double>) splitDetails.get("shares");
        if (shares == null || shares.isEmpty()) {
            throw new IllegalArgumentException("Shares map cannot be null or empty");
        }
        double totalShares = shares.values().stream().mapToDouble(Double::doubleValue).sum();
        if (totalShares == 0) {
            throw new IllegalArgumentException("Total shares cannot be zero");
        }
        Map<User, Double> splits = new HashMap<>();

        double factor = amount/totalShares;
        for(User user: participants){
            splits.put(user, shares.getOrDefault(user, 0.0) * factor);
        }
        return splits;
    }
}
