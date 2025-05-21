package orochi.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "MedicalOrder")
@Data
public class MedicalOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "OrderID")
    private Integer orderId;

    @Column(name = "AppointmentID", nullable = false)
    private Integer appointmentId;

    @ManyToOne
    @JoinColumn(name = "AppointmentID", insertable = false, updatable = false)
    private Appointment appointment;

    @Column(name = "OrderByID", nullable = false)
    private Integer orderById;

    @ManyToOne
    @JoinColumn(name = "OrderByID", insertable = false, updatable = false)
    private Doctor orderBy;

    @Column(name = "OrderType", nullable = false)
    private String orderType;

    @Column(name = "Description", length = Integer.MAX_VALUE)
    private String description;

    @Column(name = "Status", nullable = false)
    private String status;

    @Column(name = "AssignedToDeptID")
    private Integer assignedToDeptId;

    // This is the field that needs to match the mappedBy in Department
    @ManyToOne
    @JoinColumn(name = "AssignedToDeptID", insertable = false, updatable = false)
    private Department assignedToDepartment;

    @Column(name = "OrderDate", nullable = false)
    private java.sql.Date orderDate;

    @Column(name = "resultID")
    private Integer resultId;

    @ManyToOne
    @JoinColumn(name = "resultID", insertable = false, updatable = false)
    private MedicalResult medicalResult;
}

