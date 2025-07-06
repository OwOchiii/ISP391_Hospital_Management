package orochi.model;

import com.fasterxml.jackson.annotation.*;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "Specialization")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "specId")
public class Specialization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SpecID")
    private Integer specId;

    @Column(name = "SpecName", nullable = false)
    @NotBlank(message = "Specialty name cannot be empty")
    private String specName;

    @Column(name = "Symptom")
    private String symptom;

    @Column(name = "Price", precision = 10, scale = 2)
    @NotNull(message = "Price is required")
    @Positive(message = "Price must be positive")
    private BigDecimal price;

    @ManyToMany(mappedBy = "specializations")
    @JsonIgnore
    private List<Doctor> doctors;

    @OneToMany(mappedBy = "specialization")
    @JsonIgnore
    private List<MedicalService> services;
}
