package orochi.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "MedicalResult")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MedicalResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ResultID")
    private Integer resultId;

    @Column(name = "AppointmentID", nullable = false)
    private Integer appointmentId;

    @Column(name = "DoctorID", nullable = false)
    private Integer doctorId;

    @Column(name = "ResultDate", nullable = false)
    private LocalDateTime resultDate;

    @Column(name = "Description", length = Integer.MAX_VALUE)
    private String description;

    @Column(name = "FileURL")
    private String fileUrl;

    @Column(name = "Status", nullable = false)
    private String status;

    @ManyToOne
    @JoinColumn(name = "AppointmentID", insertable = false, updatable = false)
    private Appointment appointment;

    @ManyToOne
    @JoinColumn(name = "DoctorID", insertable = false, updatable = false)
    private Doctor doctor;

    @OneToMany(mappedBy = "medicalResult")
    private List<MedicalOrder> medicalOrders;
}
