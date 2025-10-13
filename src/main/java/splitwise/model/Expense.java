package splitwise.model;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "expenses")
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Expense {

    @Id
    private String id;
    
    @Column(nullable = false)
    private String title;
    
    private String description;
    
    @Column(nullable = false)
    private double amount;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payer_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private User payer;
    
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "expense_participants",
        joinColumns = @JoinColumn(name = "expense_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private List<User> participants;
    
    @ElementCollection
    @CollectionTable(
        name = "expense_shares",
        joinColumns = @JoinColumn(name = "expense_id")
    )
    @MapKeyJoinColumn(name = "user_id")
    @Column(name = "share_amount")
    @JsonIgnore
    private Map<User, Double> shares;
    
    @Column(nullable = false)
    private LocalDateTime timestamp;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Group group;

    public Expense(String id, String title, double amount, User payer, List<User> participants, Map<User, Double> shares, LocalDateTime timestamp) {
        this.id = id;
        this.title = title;
        this.amount = amount;
        this.payer = payer;
        this.participants = participants;
        this.shares = shares;
        this.timestamp = timestamp;
    }
    
    @JsonGetter("shares")
    public Map<String, Double> getSharesForJson() {
        if (shares == null) {
            return new HashMap<>();
        }
        
        Map<String, Double> result = new HashMap<>();
        for (Map.Entry<User, Double> entry : shares.entrySet()) {
            result.put(entry.getKey().getUserId(), entry.getValue());
        }
        return result;
    }
}
