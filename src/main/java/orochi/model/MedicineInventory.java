package orochi.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "MedicineInventory")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MedicineInventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "InventoryID")
    private Integer inventoryId;

    @Column(name = "MedicineName", nullable = false, unique = true)
    private String medicineName;

    @Column(name = "GenericName")
    private String genericName;

    @Column(name = "Category", length = 100)
    private String category;

    @Column(name = "UnitOfMeasure", nullable = false, length = 50)
    private String unitOfMeasure;

    @Column(name = "CurrentStock", nullable = false, columnDefinition = "int DEFAULT 0")
    private Integer currentStock;

    @Column(name = "ReorderLevel", nullable = false, columnDefinition = "int DEFAULT 10")
    private Integer reorderLevel;

    @Column(name = "ExpiryDate")
    private LocalDate expiryDate;

    @Column(name = "Cost", nullable = false, precision = 10, scale = 2)
    private BigDecimal cost;

    @Column(name = "Supplier")
    private String supplier;

    @OneToMany(mappedBy = "inventory")
    private List<Medicine> medicines;
}
