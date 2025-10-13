package splitwise.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import splitwise.model.Expense;
import splitwise.model.Group;
import splitwise.model.User;

import java.util.List;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, String> {
    
    List<Expense> findByPayer(User payer);
    
    @Query("SELECT e FROM Expense e JOIN e.participants p WHERE p.userId = :userId")
    List<Expense> findByParticipantId(String userId);
    
    List<Expense> findByGroup(Group group);
}