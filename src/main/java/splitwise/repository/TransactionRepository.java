package splitwise.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import splitwise.model.Transaction;
import splitwise.model.User;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    
    List<Transaction> findByFrom(User from);
    
    List<Transaction> findByTo(User to);
    
    List<Transaction> findByFromOrTo(User from, User to);
}