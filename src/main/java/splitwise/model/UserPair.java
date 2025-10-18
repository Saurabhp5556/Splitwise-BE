package splitwise.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;

@Entity
@Table(name = "user_pairs",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user1_id", "user2_id"}),
       indexes = {
           @Index(name = "idx_user_pair", columnList = "user1_id,user2_id", unique = true),
           @Index(name = "idx_user1", columnList = "user1_id"),
           @Index(name = "idx_user2", columnList = "user2_id")
       })
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class UserPair {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user1_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private User user1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user2_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private User user2;
    
    @Column(nullable = false)
    private Double balance = 0.0;
    
    @Version
    private Long version;

    public UserPair(User u1, User u2) {
        this.user1 = u1;
        this.user2 = u2;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserPair)) return false;
        UserPair that = (UserPair) o;
        return Objects.equals(user1, that.user1) && Objects.equals(user2, that.user2);
    }

    @Override
    public int hashCode() {
        return Objects.hash(user1, user2);
    }
}
