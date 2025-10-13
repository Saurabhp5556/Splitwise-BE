package splitwise.util;

import splitwise.util.impl.*;

public class SplitFactory {

    public static Split createSplit(SplitTypes splitType){

        switch (splitType){

            case EQUAL_SPLIT:
                return new EqualSplit();
            case SPLIT_BY_PERCENTAGES:
                return new PercentSplit();
            case EXACT_AMOUNT_SPLIT:
                return new ExactAmountSplit();
            case SHARES_SPLIT:
                return new ShareSplit();
            case ADJUSTMENT_SPLIT:
                return new AdjustmentSplit();
            default:
                throw new IllegalArgumentException("Unknown Split Type : "+splitType);
        }
    }

}
