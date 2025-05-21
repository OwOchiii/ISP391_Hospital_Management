package orochi.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Entity
@Table(name = "MedicalOrder")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MedicalOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "OrderID")
    private Integer orderId;

    @Column(name = "AppointmentID", nullable = false)
    private Integer appointmentId;

    @Column(name = "OrderByID", nullable = false)
    private Integer orderById;

    @Column(name = "OrderType", nullable = false)
    private String orderType;

    @Column(name = "Description", columnDefinition = "varchar(max)")
    private String description;

    @Column(name = "Status", nullable = false)
    private String status = "Pending";

    @Column(name = "AssignedToDeptID")
    private Integer assignedToDeptId;

    @Column(name = "OrderDate", nullable = false)
    private LocalDate orderDate = LocalDate.now();

    @Column(name = "resultID")
    private Integer resultId;

    @ManyToOne
    @JoinColumn(name = "AppointmentID", insertable = false, updatable = false)
    private Appointment appointment;

    @ManyToOne
    @JoinColumn(name = "OrderByID", insertable = false, updatable = false)
    private Doctor orderBy;

    @ManyToOne
    @JoinColumn(name = "resultID", insertable = false, updatable = false)
    private MedicalResult medicalResult;

    @ManyToOne
    @JoinColumn(name = "AssignedToDeptID", insertable = false, updatable = false)
    private Department assignedToDept;
}