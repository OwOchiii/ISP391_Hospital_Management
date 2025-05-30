package orochi.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "MedicalReport")
@Data
public class MedicalReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ReportID")
    private Integer reportId;

    @Column(name = "AppointmentID", nullable = false)
    private Integer appointmentId;

    @ManyToOne
    @JoinColumn(name = "AppointmentID", insertable = false, updatable = false)
    private Appointment appointment;

    @Column(name = "DoctorID", nullable = false)
    private Integer doctorId;

    @ManyToOne
    @JoinColumn(name = "DoctorID", insertable = false, updatable = false)
    private Doctor doctor;

    @Column(name = "ReportDate", nullable = false)
    private LocalDateTime reportDate;

    @Column(name = "Summary", length = Integer.MAX_VALUE)
    private String summary;

    @Column(name = "Status", nullable = false)
    private String status;

    @Column(name = "FileURL")
    private String fileUrl;
}
