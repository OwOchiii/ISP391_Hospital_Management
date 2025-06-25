package orochi.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "[Transaction]")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TransactionID")
    private Integer transactionId;

    @Column(name = "AppointmentID")
    private Integer appointmentId;


    @Column(name = "UserID", nullable = false)
    private Integer userId;

    @Column(name = "Method", nullable = false)
    private String method;

    @Column(name = "TimeOfPayment", nullable = false)
    private LocalDateTime timeOfPayment;

    @Column(name = "Status", nullable = false, columnDefinition = "varchar(20) DEFAULT 'Pending'")
    private String status;

    @Column(name = "IsPaid", nullable = false, columnDefinition = "bit DEFAULT 0")
    private Boolean isPaid = false;

    @Column(name = "RefundReason", length = Integer.MAX_VALUE)
    private String refundReason;

    @Column(name = "ProcessedByUserID")
    private Integer processedByUserId;

    @ManyToOne
    @JoinColumn(name = "AppointmentID", insertable = false, updatable = false)
    private Appointment appointment;


    @ManyToOne
    @JoinColumn(name = "UserID", insertable = false, updatable = false)
    private Users user;

    @ManyToOne
    @JoinColumn(name = "ProcessedByUserID", insertable = false, updatable = false)
    private Users processedByUser;

    @OneToOne(mappedBy = "transaction")
    private Receipt receipt;
}
