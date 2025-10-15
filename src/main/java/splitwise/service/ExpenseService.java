package splitwise.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
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

    // Add expense between two users (or more)
    public Expense addExpense(String title, String description, double amount, 
                             String payerId, List<String> participantIds, 
                             SplitTypes splitType, Map<String, Object> splitDetails) {
        
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
        Expense expense = new Expense(expenseId, title, splitType, amount, payer, participants, shares, splitDetails, LocalDateTime.now());
        expense.setDescription(description);
        
        expenseManager.addExpense(expense);
        return expense;
    }
    
    // Add expense to a group
    public Expense addGroupExpense(String title, String description, double amount,
                                  String payerId, Long groupId,
                                  SplitTypes splitType, Map<String, Object> splitDetails) {
        
        User payer = userService.getUser(payerId);
        Group group = groupService.getGroup(groupId);
        List<User> groupMembers = group.getUserList();
        
        // Use all group members as participants by default
        List<User> participants = new ArrayList<>(groupMembers);
        
        // Ensure payer is part of the group
        if (!groupMembers.contains(payer)) {
            throw new IllegalArgumentException("Payer must be a member of the group");
        }
        
        // Convert user IDs to User objects in split details if needed
        Map<String, Object> processedSplitDetails = processSplitDetails(splitDetails, splitType);
        
        Split split = SplitFactory.createSplit(splitType);
        Map<User, Double> shares = split.calculateSplit(amount, participants, processedSplitDetails);
        
        String expenseId = UUID.randomUUID().toString();
        Expense expense = new Expense(expenseId, title, splitType, amount, payer, participants, shares, splitDetails, LocalDateTime.now());
        expense.setDescription(description);
        expense.setGroup(group);
        
        expenseManager.addExpense(expense);
        return expense;
    }
    
    // Add expense to a group with specific participants
    public Expense addGroupExpense(String title, String description, double amount,
                                  String payerId, Long groupId, List<String> participantIds,
                                  SplitTypes splitType, Map<String, Object> splitDetails) {
        
        User payer = userService.getUser(payerId);
        Group group = groupService.getGroup(groupId);
        List<User> groupMembers = group.getUserList();
        
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
        Expense expense = new Expense(expenseId, title, splitType,amount, payer, participants, shares, splitDetails,  LocalDateTime.now());
        expense.setDescription(description);
        expense.setGroup(group);
        
        expenseManager.addExpense(expense);
        return expense;
    }
    
    // Edit expense between users
    public Expense editExpense(String expenseId, String title, String description, 
                              Double amount, String payerId, List<String> participantIds, 
                              SplitTypes splitType, Map<String, Object> splitDetails) {
        
        Expense existingExpense = expenseManager.getExpenseById(expenseId);
        
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
        
        Map<User, Double> shares;
        if (splitType != null && amount != null) {
            // Convert user IDs to User objects in split details if needed
            Map<String, Object> processedSplitDetails = processSplitDetails(splitDetails, splitType);
            Split split = SplitFactory.createSplit(splitType);
            shares = split.calculateSplit(amount, participants, processedSplitDetails);
        } else if (amount != null) {
            // Recalculate with existing split type
            // This is a simplification - in a real app, you'd need to determine the original split type
            Split split = SplitFactory.createSplit(SplitTypes.EQUAL_SPLIT);
            shares = split.calculateSplit(amount, participants, new HashMap<>());
        } else {
            shares = existingExpense.getShares();
        }
        
        Expense updatedExpense = new Expense(
            expenseId,
            title != null ? title : existingExpense.getTitle(), splitType,
            amount != null ? amount : existingExpense.getAmount(),
            payer,
            participants,
            shares,
                splitDetails,
            LocalDateTime.now()
        );
        
        updatedExpense.setDescription(description != null ? description : existingExpense.getDescription());
        updatedExpense.setGroup(existingExpense.getGroup());
        
        expenseManager.updateExpense(updatedExpense);
        return updatedExpense;
    }
    
    // Delete expense
    public void deleteExpense(String expenseId) {
        Expense expenseToDelete = expenseManager.getExpenseById(expenseId);
        
        // Create a reverse expense to cancel out the original
        Expense reverseExpense = new Expense(
            UUID.randomUUID().toString(),
            "Reversal of: " + expenseToDelete.getTitle(),
                expenseToDelete.getSplitType(),
            -expenseToDelete.getAmount(),
            expenseToDelete.getPayer(),
            expenseToDelete.getParticipants(),
            createReverseShares(expenseToDelete.getShares()),
                expenseToDelete.getSplitDetails(),
            LocalDateTime.now()
        );
        
        reverseExpense.setGroup(expenseToDelete.getGroup());
        expenseManager.addExpense(reverseExpense);
        
        // Optionally, you can also delete the original expense from the database
        // expenseManager.deleteExpense(expenseId);
    }
    
    private Map<User, Double> createReverseShares(Map<User, Double> originalShares) {
        Map<User, Double> reverseShares = new HashMap<>();
        
        for (Map.Entry<User, Double> entry : originalShares.entrySet()) {
            reverseShares.put(entry.getKey(), -entry.getValue());
        }
        
        return reverseShares;
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
    public List<Expense> getExpensesByGroup(Long groupId) {
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
        
        return processedDetails;
    }
}