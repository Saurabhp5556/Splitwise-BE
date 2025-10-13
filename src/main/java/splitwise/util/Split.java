package splitwise.util;

import splitwise.model.User;

import java.util.List;
import java.util.Map;

public interface Split {


    /**
     * Calculates split for the given amount among participants based on split details
     * @param amount        Total amount to split
     * @param participants  List of users participating in the split
     * @param splitDetails  additional details required for split logic
     * @return  Map where key is user and value is amount they owe/get back
     */
    Map<User, Double> calculateSplit(double amount, List<User> participants, Map<String, Object> splitDetails);
}
