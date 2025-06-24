package orochi.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import orochi.model.*;

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
    @NotNull(message = "Doctor ID is required")
    private Integer doctorId;

    @Column(name = "PatientID", nullable = false)
    @NotNull(message = "Patient ID is required")
    private Integer patientId;

    @Column(name = "RoomID")
    private Integer roomId;

    @Column(name = "Description")
    private String description;

    @Column(name = "DateTime", nullable = false)
    @NotNull(message = "Date and time is required")
    private LocalDateTime dateTime;

    @Column(name = "Status", nullable = false, columnDefinition = "varchar(20) DEFAULT 'Scheduled'")
    @NotNull(message = "Status is required")
    private String status;

    @Column(name = "Email")
    private String email;

    @Column(name = "PhoneNumber")
    private String phoneNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "DoctorID", insertable = false, updatable = false)
    private Doctor doctor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PatientID", insertable = false, updatable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "RoomID", insertable = false, updatable = false)
    private Room room;

    @OneToMany(mappedBy = "appointment", fetch = FetchType.LAZY)
    private List<Transaction> transactions;

    @OneToMany(mappedBy = "appointment", fetch = FetchType.LAZY)
    private List<MedicalResult> medicalResults;

    @OneToMany(mappedBy = "appointment", fetch = FetchType.LAZY)
    private List<MedicalOrder> medicalOrders;

    @OneToMany(mappedBy = "appointment", fetch = FetchType.LAZY)
    private List<Prescription> prescriptions;

    @OneToOne(mappedBy = "appointment", fetch = FetchType.LAZY)
    private Schedule schedule;
}
