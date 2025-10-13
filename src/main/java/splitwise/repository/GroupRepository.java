package splitwise.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import splitwise.model.Group;
import splitwise.model.User;

import java.util.List;

@Repository
public interface GroupRepository extends JpaRepository<Group, Long> {
    
    @Query("SELECT g FROM Group g JOIN g.userList u WHERE u.userId = :userId")
    List<Group> findGroupsByUserId(String userId);
}