package splitwise.util.impl;

import splitwise.model.User;
import splitwise.util.Split;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EqualSplit implements Split {


    @Override
    public Map<User, Double> calculateSplit(double amount, List<User> participants, Map<String, Object> splitDetails) {
        Map<User, Double> splits = new HashMap<>();
        if(participants!=null && !participants.isEmpty()) {
            double amountPerUser = amount / participants.size();
            participants.forEach(p-> splits.put(p, amountPerUser));
        }
        return splits;
    }
}
