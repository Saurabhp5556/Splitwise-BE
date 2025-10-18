package splitwise.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import splitwise.model.User;
import splitwise.model.UserPair;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserPairRepository extends JpaRepository<UserPair, Long> {
    
    Optional<UserPair> findByUser1AndUser2(User user1, User user2);
    
    @Query("SELECT up FROM UserPair up WHERE up.user1 = :user OR up.user2 = :user")
    List<UserPair> findByUser(User user);
    
    /**
     * Optimized query to get net balances for all users with aggregation.
     * Returns: [User, owedAmount, owesAmount]
     */
    @Query("""
        SELECT u,
               SUM(CASE WHEN up.user2 = u THEN up.balance ELSE 0 END) as owedAmount,
               SUM(CASE WHEN up.user1 = u THEN up.balance ELSE 0 END) as owesAmount
        FROM User u
        LEFT JOIN UserPair up ON (up.user1 = u OR up.user2 = u)
        GROUP BY u
        HAVING SUM(CASE WHEN up.user2 = u THEN up.balance ELSE 0 END) != 0
            OR SUM(CASE WHEN up.user1 = u THEN up.balance ELSE 0 END) != 0
        """)
    List<Object[]> getNetBalances();
}