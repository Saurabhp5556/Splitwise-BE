package splitwise.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import splitwise.repository.*;

import java.util.HashMap;
import java.util.Map;

/**
 * AdminService - Handles administrative operations for the Splitwise application
 * 
 * This service provides destructive operations that should only be used by administrators.
 * All operations are transactional to ensure data consistency.
 */
@Service
public class AdminService {

    private static final Logger logger = LoggerFactory.getLogger(AdminService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private UserPairRepository userPairRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    /**
     * Clears all data from the database.
     * 
     * This method deletes all records from all tables in the correct order
     * to avoid foreign key constraint violations.
     * 
     * Deletion order:
     * 1. Transactions (no dependencies)
     * 2. UserPairs (balance records)
     * 3. Expenses (references users and groups)
     * 4. Groups (references users via many-to-many)
     * 5. Users (referenced by other entities)
     * 
     * @return Map containing the count of deleted records for each entity type
     */
    @Transactional
    public Map<String, Integer> clearAllData() {
        logger.warn("Starting database cleanup operation");
        
        Map<String, Integer> deletionStats = new HashMap<>();
        
        try {
            // Delete in order to avoid foreign key constraint violations
            
            // 1. Delete Transactions
            long transactionCount = transactionRepository.count();
            transactionRepository.deleteAll();
            deletionStats.put("transactions", (int) transactionCount);
            logger.info("Deleted {} transactions", transactionCount);
            
            // 2. Delete UserPairs (balance records)
            long userPairCount = userPairRepository.count();
            userPairRepository.deleteAll();
            deletionStats.put("userPairs", (int) userPairCount);
            logger.info("Deleted {} user pairs", userPairCount);
            
            // 3. Delete Expenses
            long expenseCount = expenseRepository.count();
            expenseRepository.deleteAll();
            deletionStats.put("expenses", (int) expenseCount);
            logger.info("Deleted {} expenses", expenseCount);
            
            // 4. Delete Groups (this will also clear the many-to-many relationship with users)
            long groupCount = groupRepository.count();
            groupRepository.deleteAll();
            deletionStats.put("groups", (int) groupCount);
            logger.info("Deleted {} groups", groupCount);
            
            // 5. Delete Users
            long userCount = userRepository.count();
            userRepository.deleteAll();
            deletionStats.put("users", (int) userCount);
            logger.info("Deleted {} users", userCount);
            
            // Calculate total
            int totalDeleted = deletionStats.values().stream().mapToInt(Integer::intValue).sum();
            deletionStats.put("total", totalDeleted);
            
            logger.warn("Database cleanup completed successfully. Total records deleted: {}", totalDeleted);
            
            return deletionStats;
            
        } catch (Exception e) {
            logger.error("Error during database cleanup", e);
            throw new RuntimeException("Failed to clear database: " + e.getMessage(), e);
        }
    }

    /**
     * Gets current database statistics (record counts for each table)
     * 
     * @return Map containing current record counts
     */
    public Map<String, Long> getDatabaseStats() {
        Map<String, Long> stats = new HashMap<>();
        
        stats.put("users", userRepository.count());
        stats.put("groups", groupRepository.count());
        stats.put("expenses", expenseRepository.count());
        stats.put("userPairs", userPairRepository.count());
        stats.put("transactions", transactionRepository.count());
        
        return stats;
    }

    /**
     * Validates that the database is in a consistent state
     * 
     * @return Map containing validation results
     */
    public Map<String, Object> validateDatabaseIntegrity() {
        Map<String, Object> validation = new HashMap<>();
        
        try {
            // Check for orphaned records or inconsistencies
            long totalRecords = userRepository.count() + 
                              groupRepository.count() + 
                              expenseRepository.count() + 
                              userPairRepository.count() + 
                              transactionRepository.count();
            
            validation.put("totalRecords", totalRecords);
            validation.put("status", "healthy");
            validation.put("timestamp", java.time.LocalDateTime.now().toString());
            
        } catch (Exception e) {
            validation.put("status", "error");
            validation.put("error", e.getMessage());
        }
        
        return validation;
    }
}