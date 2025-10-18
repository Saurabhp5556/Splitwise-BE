package splitwise.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import splitwise.model.Group;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupRepository extends JpaRepository<Group, String> {
    
    @Query("SELECT g FROM Group g JOIN g.userList u WHERE u.userId = :userId")
    List<Group> findGroupsByUserId(String userId);
    
    @EntityGraph(attributePaths = {"userList"})
    @Query("SELECT g FROM Group g WHERE g.groupId = :groupId")
    Optional<Group> findByIdWithMembers(@Param("groupId") String groupId);
    
    @EntityGraph(attributePaths = {"userList"})
    @Query("SELECT DISTINCT g FROM Group g JOIN g.userList u WHERE u.userId = :userId")
    List<Group> findGroupsWithMembersByUserId(@Param("userId") String userId);
}