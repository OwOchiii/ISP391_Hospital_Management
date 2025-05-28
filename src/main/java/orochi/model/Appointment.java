package orochi.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "Appointment")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "AppointmentID")
    private Integer appointmentId;


    @Column(name = "DoctorID", nullable = false)
    private Integer doctorId;

    @Column(name = "PatientID", nullable = false)
    private Integer patientId;

    @Column(name = "RoomID")
    private Integer roomId;

    @Column(name = "Description")
    private String description;

    @Column(name = "DateTime", nullable = false)
    private LocalDateTime dateTime;

    @Column(name = "Status", nullable = false, columnDefinition = "varchar(20) DEFAULT 'Scheduled'")
    private String status;

    @ManyToOne
    @JoinColumn(name = "DoctorID", insertable = false, updatable = false)
    private Doctor doctor;

    @ManyToOne
    @JoinColumn(name = "PatientID", insertable = false, updatable = false)
    private Patient patient;

    @ManyToOne
    @JoinColumn(name = "RoomID", insertable = false, updatable = false)
    private Room room;


    @OneToMany(mappedBy = "appointment")
    private List<Transaction> transactions;

    @OneToMany(mappedBy = "appointment")
    private List<MedicalResult> medicalResults;

    @OneToMany(mappedBy = "appointment")
    private List<MedicalOrder> medicalOrders;

    @OneToOne(mappedBy = "appointment")
    private Prescription prescription;
}
