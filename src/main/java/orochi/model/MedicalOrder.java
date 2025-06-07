package orochi.model;

import jakarta.persistence.*;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonBackReference;

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

    @Column(name = "DoctorID", nullable = false)
    private Integer doctorId;

    @ManyToOne
    @JoinColumn(name = "DoctorID", insertable = false, updatable = false)
    private Doctor orderBy;

    @Column(name = "OrderType", nullable = false)
    private String orderType;

    @Column(name = "Description", length = Integer.MAX_VALUE)
    private String description;

    @Column(name = "Status", nullable = false)
    private String status;

    @Column(name = "AssignedToDeptID")
    private Integer assignedToDeptId;

    @ManyToOne
    @JoinColumn(name = "AssignedToDeptID", insertable = false, updatable = false)
    private Department assignedToDepartment;

    @Column(name = "OrderDate", nullable = false)
    private java.sql.Date orderDate;

    @Column(name = "resultID")
    private Integer resultId;

    @ManyToOne
    @JoinColumn(name = "resultID", insertable = false, updatable = false)
    @JsonBackReference(value = "result-order")
    private MedicalResult medicalResult;
}
