package splitwise.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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
}