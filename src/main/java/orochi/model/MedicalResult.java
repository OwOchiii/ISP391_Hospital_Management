package orochi.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonBackReference;

import java.time.LocalDateTime;

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

    @ManyToOne
    @JoinColumn(name = "AppointmentID", insertable = false, updatable = false)
    @JsonBackReference(value = "appointment-result")
    private Appointment appointment;

    @Column(name = "DoctorID", nullable = false)
    private Integer doctorId;

    @ManyToOne
    @JoinColumn(name = "DoctorID", insertable = false, updatable = false)
    @JsonBackReference(value = "doctor-result")
    private Doctor doctor;

    @Column(name = "ResultDate", nullable = false)
    private LocalDateTime resultDate;

    @Column(name = "Description", length = Integer.MAX_VALUE)
    private String description;

    @Column(name = "FileURL")
    private String fileUrl;

    @Column(name = "Status", nullable = false)
    private String status;

    @OneToMany(mappedBy = "medicalResult")
    @JsonManagedReference(value = "result-order")
    private java.util.List<MedicalOrder> orders;

    @PrePersist
    protected void onCreate() {
        if (resultDate == null) {
            resultDate = LocalDateTime.now();
        }
    }
}
