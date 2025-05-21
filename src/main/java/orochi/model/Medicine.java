package orochi.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "Medicine")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Medicine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MedicationID")
    private Integer medicationId;

    @Column(name = "PrescriptionID", nullable = false)
    private Integer prescriptionId;

    @Column(name = "InventoryID", nullable = false)
    private Integer inventoryId;

    @Column(name = "Dosage", nullable = false, length = 100)
    private String dosage;

    @Column(name = "Frequency", nullable = false, length = 100)
    private String frequency;

    @Column(name = "Duration", nullable = false, length = 100)
    private String duration;

    @Column(name = "Instructions", length = Integer.MAX_VALUE)
    private String instructions;

    @ManyToOne
    @JoinColumn(name = "PrescriptionID", insertable = false, updatable = false)
    private Prescription prescription;

    @ManyToOne
    @JoinColumn(name = "InventoryID", insertable = false, updatable = false)
    private MedicineInventory inventory;
}
