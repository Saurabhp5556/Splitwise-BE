package splitwise.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import splitwise.model.Expense;
import splitwise.model.Group;
import splitwise.model.User;
import splitwise.repository.ExpenseRepository;
import splitwise.util.Split;
import splitwise.util.SplitFactory;
import splitwise.util.SplitTypes;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class ExpenseService {

    @Autowired
    private ExpenseManager expenseManager;

    @Autowired
    private UserService userService;

    @Autowired
    private GroupService groupService;
    
    @Autowired
    private ExpenseRepository expenseRepository;
    
    @Autowired
    private BalanceSheet balanceSheet;

    // Add expense between two users (or more)
    @Transactional
    public Expense addExpense(String title, String description, double amount,
                             String payerId, List<String> participantIds,
                             SplitTypes splitType, Map<String, Object> splitDetails, Boolean isSettleUp) {
        
        // Get the currently authenticated user
        String currentUserId = getCurrentUserId();
        
        // Validate that the current user is either the payer or one of the participants
        validateUserAuthorization(currentUserId, payerId, participantIds);
        
        // Validate that required split details are provided based on split type
        validateSplitDetails(splitType, splitDetails, participantIds);
        
        User payer = userService.getUser(payerId);
        List<User> participants = new ArrayList<>();
        
        for (String participantId : participantIds) {
            participants.add(userService.getUser(participantId));
        }
        
        // Convert user IDs to User objects in split details if needed
        Map<String, Object> processedSplitDetails = processSplitDetails(splitDetails, splitType);
        
        Split split = SplitFactory.createSplit(splitType);
        Map<User, Double> shares = split.calculateSplit(amount, participants, processedSplitDetails);
        
        String expenseId = UUID.randomUUID().toString();
        Expense expense = new Expense(expenseId, title, splitType, amount, payer, participants, shares, splitDetails, LocalDateTime.now(), isSettleUp);
        expense.setDescription(description);
        
        expenseManager.addExpense(expense);
        return expense;
    }
    
    // Add expense to a group with specific participants
    @Transactional
    public Expense addGroupExpense(String title, String description, double amount,
                                  String payerId, String groupId, List<String> participantIds,
                                  SplitTypes splitType, Map<String, Object> splitDetails, Boolean isSettleUp) {
        
        // Get the currently authenticated user
        String currentUserId = getCurrentUserId();
        
        User payer = userService.getUser(payerId);
        Group group = groupService.getGroup(groupId);
        List<User> groupMembers = group.getUserList();
        
        // Ensure current user is a member of the group
        User currentUser = userService.getUser(currentUserId);
        if (!groupMembers.contains(currentUser)) {
            throw new IllegalArgumentException("You must be a member of the group to add expenses");
        }
        
        // Validate that required split details are provided based on split type
        List<String> finalParticipantIds = (participantIds != null && !participantIds.isEmpty())
            ? participantIds
            : groupMembers.stream().map(User::getUserId).toList();
        validateSplitDetails(splitType, splitDetails, finalParticipantIds);
        
        // Ensure payer is part of the group
        if (!groupMembers.contains(payer)) {
            throw new IllegalArgumentException("Payer must be a member of the group");
        }
        
        // If specific participants are provided, use them; otherwise use all group members
        List<User> participants = new ArrayList<>();
        if (participantIds != null && !participantIds.isEmpty()) {
            for (String participantId : participantIds) {
                User participant = userService.getUser(participantId);
                // Ensure all participants are members of the group
                if (!groupMembers.contains(participant)) {
                    throw new IllegalArgumentException("User " + participantId + " is not a member of the group");
                }
                participants.add(participant);
            }
        } else {
            participants = new ArrayList<>(groupMembers);
        }
        
        // Convert user IDs to User objects in split details if needed
        Map<String, Object> processedSplitDetails = processSplitDetails(splitDetails, splitType);
        
        Split split = SplitFactory.createSplit(splitType);
        Map<User, Double> shares = split.calculateSplit(amount, participants, processedSplitDetails);
        
        String expenseId = UUID.randomUUID().toString();
        Expense expense = new Expense(expenseId, title, splitType,amount, payer, participants, shares, splitDetails,  LocalDateTime.now(), isSettleUp);
        expense.setDescription(description);
        expense.setGroup(group);
        
        expenseManager.addExpense(expense);
        return expense;
    }
    
    /**
     * Edit expense between users with proper balance reversal.
     * This method:
     * 1. Reverses the old expense's balance changes
     * 2. Updates the expense details
     * 3. Applies the new balance changes
     */
    @Transactional
    public Expense editExpense(String expenseId, String title, String description,
                              Double amount, String payerId, List<String> participantIds,
                              SplitTypes splitType, Map<String, Object> splitDetails, Boolean isSettleUp) {
        
        Expense existingExpense = expenseManager.getExpenseById(expenseId);
        
        // Step 1: Reverse existing balance changes
        balanceSheet.reverseBalances(existingExpense);
        
        // Step 2: Update expense details
        User payer = payerId != null ? userService.getUser(payerId) : existingExpense.getPayer();
        List<User> participants;
        
        if (participantIds != null) {
            participants = new ArrayList<>();
            for (String participantId : participantIds) {
                participants.add(userService.getUser(participantId));
            }
        } else {
            participants = existingExpense.getParticipants();
        }
        
        // Step 3: Recalculate splits
        Map<User, Double> shares;
        SplitTypes finalSplitType = splitType != null ? splitType : existingExpense.getSplitType();
        double finalAmount = amount != null ? amount : existingExpense.getAmount();
        
        if (splitType != null || amount != null) {
            Map<String, Object> processedSplitDetails = processSplitDetails(splitDetails, finalSplitType);
            Split split = SplitFactory.createSplit(finalSplitType);
            shares = split.calculateSplit(finalAmount, participants, processedSplitDetails);
        } else {
            shares = existingExpense.getShares();
        }
        
        Expense updatedExpense = new Expense(
            expenseId,
            title != null ? title : existingExpense.getTitle(),
            finalSplitType,
            finalAmount,
            payer,
            participants,
            shares,
            splitDetails != null ? splitDetails : existingExpense.getSplitDetails(),
            LocalDateTime.now(),
            isSettleUp
        );
        
        updatedExpense.setDescription(description != null ? description : existingExpense.getDescription());
        updatedExpense.setGroup(existingExpense.getGroup());
        updatedExpense.setVersion(existingExpense.getVersion());
        
        // Step 4: Apply new balance changes
        expenseManager.updateExpense(updatedExpense);
        
        return updatedExpense;
    }
    
    /**
     * Deletes an expense and reverses its impact on user balances.
     *
     * This method:
     * 1. Directly reverses the balance changes using BalanceSheet.reverseBalances()
     * 2. Deletes the expense from the database
     *
     * This is much simpler than creating temporary reverse expenses!
     *
     * @param expenseId The ID of the expense to delete
     */
    @Transactional
    public void deleteExpense(String expenseId) {
        Expense expenseToDelete = expenseManager.getExpenseById(expenseId);
        
        // Directly reverse the balance changes - much simpler!
        balanceSheet.reverseBalances(expenseToDelete);
        
        // Delete the expense from the database
        expenseManager.deleteExpense(expenseId);
    }
    
    // Get all expenses
    public List<Expense> getAllExpenses() {
        return expenseManager.getAllExpenses();
    }
    
    // Get expense by ID
    public Expense getExpenseById(String expenseId) {
        return expenseManager.getExpenseById(expenseId);
    }
    
    // Get expenses by user
    public List<Expense> getExpensesByUser(String userId) {
        User user = userService.getUser(userId);
        return expenseRepository.findByParticipantId(userId);
    }
    
    // Get expenses by group
    public List<Expense> getExpensesByGroup(String groupId) {
        Group group = groupService.getGroup(groupId);
        return expenseRepository.findByGroup(group);
    }
    
    /**
     * Process split details to convert user IDs to User objects for percentage and share splits
     */
    private Map<String, Object> processSplitDetails(Map<String, Object> splitDetails, SplitTypes splitType) {
        if (splitDetails == null) {
            return splitDetails;
        }
        
        Map<String, Object> processedDetails = new HashMap<>(splitDetails);
        
        // Handle percentage splits - convert user IDs to User objects
        if (splitType == SplitTypes.SPLIT_BY_PERCENTAGES && splitDetails.containsKey("percentages")) {
            @SuppressWarnings("unchecked")
            Map<String, Double> userIdPercentages = (Map<String, Double>) splitDetails.get("percentages");
            
            Map<User, Double> userPercentages = new HashMap<>();
            for (Map.Entry<String, Double> entry : userIdPercentages.entrySet()) {
                User user = userService.getUser(entry.getKey());
                try{
                    userPercentages.put(user, entry.getValue().doubleValue());

                } catch (Exception e){
                    Exception e1 = e;
                }
                entry.getValue();
            }
            
            processedDetails.put("percentages", userPercentages);
        }
        
        // Handle share splits - convert user IDs to User objects
        if (splitType == SplitTypes.SHARES_SPLIT && splitDetails.containsKey("shares")) {
            @SuppressWarnings("unchecked")
            Map<String, Double> userIdShares = (Map<String, Double>) splitDetails.get("shares");
            
            Map<User, Double> userShares = new HashMap<>();
            for (Map.Entry<String, Double> entry : userIdShares.entrySet()) {
                User user = userService.getUser(entry.getKey());
                userShares.put(user, entry.getValue());
            }
            
            processedDetails.put("shares", userShares);
        }
        
        // Handle exact amount splits - convert user IDs to User objects
        if (splitType == SplitTypes.EXACT_AMOUNT_SPLIT && splitDetails.containsKey("amounts")) {
            @SuppressWarnings("unchecked")
            Map<String, Double> userIdAmounts = (Map<String, Double>) splitDetails.get("amounts");
            
            Map<User, Double> userAmounts = new HashMap<>();
            for (Map.Entry<String, Double> entry : userIdAmounts.entrySet()) {
                User user = userService.getUser(entry.getKey());
                userAmounts.put(user, entry.getValue());
            }
            
            processedDetails.put("amounts", userAmounts);
        }
        
        // Handle adjustment splits - convert user IDs to User objects
        if (splitType == SplitTypes.ADJUSTMENT_SPLIT && splitDetails.containsKey("adjustments")) {
            @SuppressWarnings("unchecked")
            Map<String, Double> userIdAdjustments = (Map<String, Double>) splitDetails.get("adjustments");

            Map<User, Double> userAdjustments = new HashMap<>();
            for (Map.Entry<String, Double> entry : userIdAdjustments.entrySet()) {
                User user = userService.getUser(entry.getKey());
                userAdjustments.put(user, entry.getValue());
            }

            processedDetails.put("adjustments", userAdjustments);
        }
        
        return processedDetails;
    }
    
    /**
     * Get the currently authenticated user's ID from the security context
     */
    private String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("No authenticated user found");
        }
        return authentication.getName(); // This returns the userId (subject from JWT)
    }
    
    /**
     * Validate that the current user is authorized to create this expense.
     * The user must be either the payer or one of the participants.
     */
    private void validateUserAuthorization(String currentUserId, String payerId, List<String> participantIds) {
        boolean isAuthorized = currentUserId.equals(payerId) || participantIds.contains(currentUserId);
        
        if (!isAuthorized) {
            throw new IllegalArgumentException(
                "Unauthorized: You can only create expenses where you are either the payer or a participant"
            );
        }
    }
    
    /**
     * Validate that required split details are provided based on the split type
     */
    private void validateSplitDetails(SplitTypes splitType, Map<String, Object> splitDetails, List<String> participantIds) {
        if (splitType == null) {
            throw new IllegalArgumentException("Split type is required");
        }
        
        switch (splitType) {
            case EQUAL_SPLIT:
                // No additional details required
                break;
                
            case SPLIT_BY_PERCENTAGES:
                if (splitDetails == null || !splitDetails.containsKey("percentages")) {
                    throw new IllegalArgumentException("Split details with 'percentages' map is required for SPLIT_BY_PERCENTAGES");
                }
                @SuppressWarnings("unchecked")
                Map<String, Double> percentages = (Map<String, Double>) splitDetails.get("percentages");
                if (percentages.isEmpty()) {
                    throw new IllegalArgumentException("Percentages map cannot be empty");
                }
                break;
                
            case SHARES_SPLIT:
                if (splitDetails == null || !splitDetails.containsKey("shares")) {
                    throw new IllegalArgumentException("Split details with 'shares' map is required for SHARES_SPLIT");
                }
                @SuppressWarnings("unchecked")
                Map<String, Double> shares = (Map<String, Double>) splitDetails.get("shares");
                if (shares.isEmpty()) {
                    throw new IllegalArgumentException("Shares map cannot be empty");
                }
                break;
                
            case EXACT_AMOUNT_SPLIT:
                if (splitDetails == null || !splitDetails.containsKey("amounts")) {
                    throw new IllegalArgumentException("Split details with 'amounts' map is required for EXACT_AMOUNT_SPLIT");
                }
                @SuppressWarnings("unchecked")
                Map<String, Double> amounts = (Map<String, Double>) splitDetails.get("amounts");
                if (amounts.isEmpty()) {
                    throw new IllegalArgumentException("Amounts map cannot be empty");
                }
                break;
                
            case ADJUSTMENT_SPLIT:
                if (splitDetails == null || !splitDetails.containsKey("adjustments")) {
                    throw new IllegalArgumentException("Split details with 'adjustments' map is required for ADJUSTMENT_SPLIT");
                }
                @SuppressWarnings("unchecked")
                Map<String, Double> adjustments = (Map<String, Double>) splitDetails.get("adjustments");
                if (adjustments.isEmpty()) {
                    throw new IllegalArgumentException("Adjustments map cannot be empty");
                }
                break;
                
            default:
                throw new IllegalArgumentException("Unsupported split type: " + splitType);
        }
    }
}