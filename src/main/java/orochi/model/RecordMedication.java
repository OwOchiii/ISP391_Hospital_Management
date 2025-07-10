package orochi.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDate;

@Entity
@Table(name = "RecordMedication")
@Data
@NoArgsConstructor
@AllArgsConstructor
@DynamicUpdate
public class RecordMedication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MedicationID")
    private Integer medicationId;

    @Column(name = "RecordID", nullable = false)
    private Integer recordId;

    @Column(name = "Name", nullable = false, length = 255)
    private String name;

    @Column(name = "Dosage", nullable = false, length = 100)
    private String dosage;

    @Column(name = "Frequency", nullable = false, length = 100)
    private String frequency;

    @Column(name = "StartDate", nullable = false)
    private LocalDate startDate;

    @Column(name = "EndDate")
    private LocalDate endDate;

    @ManyToOne
    @JoinColumn(name = "RecordID", referencedColumnName = "RecordID", insertable = false, updatable = false)
    private MedicalRecord medicalRecord;
}
