package orochi.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

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

    @Column(name = "PrescriptionID", insertable = false, updatable = false)
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PrescriptionID", nullable = false)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @JsonBackReference("prescription-medicine")
    private Prescription prescription;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "InventoryID", insertable = false, updatable = false)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @JsonBackReference("inventory-medicine")
    private MedicineInventory inventory;

    // Methods to synchronize the relationship
    public void setPrescription(Prescription prescription) {
        this.prescription = prescription;
        this.prescriptionId = prescription != null ? prescription.getPrescriptionId() : null;
    }
}
