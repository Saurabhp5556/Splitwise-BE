package splitwise.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_user_id", nullable = false)
    private User from;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_user_id", nullable = false)
    private User to;

    @Column(nullable = false)
    private double amount;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;

    public Transaction(User debtor, User creditor, double transferAmount) {
        this.from = debtor;
        this.to = creditor;
        this.amount = transferAmount;
        this.createdAt = LocalDateTime.now();
    }
}
