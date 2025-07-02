package orochi.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "MedicalRecord")
@Data
@NoArgsConstructor
@AllArgsConstructor
@DynamicUpdate
public class MedicalRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "RecordID")
    private Integer recordId;

    @Column(name = "PatientID", nullable = false)
    private Integer patientId;

    @Column(name = "DoctorID", nullable = false)
    private Integer doctorId;

    @Column(name = "RecordDate", nullable = false)
    private LocalDateTime recordDate;

    @Column(name = "LastUpdated", nullable = false)
    private LocalDateTime lastUpdated;


    @Column(name = "MedicalConditions", columnDefinition = "varchar(max)")
    private String medicalConditions;

    @Column(name = "MedicalHistory", columnDefinition = "varchar(max)")
    private String medicalHistory;

    @Column(name = "Allergies", columnDefinition = "varchar(max)")
    private String allergies;


    @Column(name = "BloodType", length = 10)
    private String bloodType;

    @Column(name = "HealthStatus", length = 50)
    private String healthStatus;


    @ManyToOne
    @JoinColumn(name = "PatientID", referencedColumnName = "PatientID", insertable = false, updatable = false)
    private Patient patient;

    @ManyToOne
    @JoinColumn(name = "DoctorID", referencedColumnName = "DoctorID", insertable = false, updatable = false)
    private Doctor doctor;

    @OneToMany(mappedBy = "medicalRecord", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RecordMedication> medications;

    @PrePersist
    protected void onCreate() {
        recordDate = LocalDateTime.now();
        lastUpdated = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        lastUpdated = LocalDateTime.now();
    }
}
