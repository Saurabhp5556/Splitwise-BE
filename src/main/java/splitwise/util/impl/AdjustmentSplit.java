package splitwise.util.impl;

import splitwise.model.User;
import splitwise.util.Split;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdjustmentSplit implements Split {

    //TODO
    @Override
    public Map<User, Double> calculateSplit(double amount, List<User> participants, Map<String, Object> splitDetails) {
        Map<User, Double> res = new HashMap<>();
        participants.forEach(p-> res.put(p, 0d));
        return res;
    }
}
