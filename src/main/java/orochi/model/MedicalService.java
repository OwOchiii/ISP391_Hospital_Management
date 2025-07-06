// src/main/java/orochi/model/MedicalService.java
package orochi.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "Service")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MedicalService {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ServiceID")
    private Integer serviceId;

    @NotNull(message = "Specialization must be selected")
    @Column(name = "SpecID", nullable = false)
    private Integer specId;

    @NotBlank(message = "Service name cannot be blank")
    @Size(max = 100, message = "Service name can be at most 100 characters")
    @Column(name = "ServiceName", nullable = false)
    private String serviceName;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be at least 0.01")
    @Digits(integer = 8, fraction = 2, message = "Price must be a valid amount with up to 2 decimals")
    @Column(name = "Price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @ManyToOne
    @JoinColumn(name = "SpecID", insertable = false, updatable = false)
    private Specialization specialization;
}
