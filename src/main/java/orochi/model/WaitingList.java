package orochi.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "WaitingList")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WaitingList {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "WaitID")
    private Integer waitId;

    @Column(name = "UserID", nullable = false)
    private Integer userId;

    @Column(name = "Status", nullable = false, columnDefinition = "varchar(20) DEFAULT 'Waiting'")
    private String status;

    @Column(name = "Comment")
    private String comment;

    @Column(name = "CalledAt")
    private LocalDateTime calledAt;

    @Column(name = "CreateAt", nullable = false, columnDefinition = "datetime DEFAULT GETDATE()")
    private LocalDateTime createAt;

    @ManyToOne
    @JoinColumn(name = "UserID", insertable = false, updatable = false)
    private Patient patient;

    @OneToMany(mappedBy = "waitingList")
    private List<Appointment> appointments;

    @OneToMany(mappedBy = "waitingList")
    private List<Transaction> transactions;
}
