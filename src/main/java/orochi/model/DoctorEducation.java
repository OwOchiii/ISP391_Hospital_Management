package orochi.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "DoctorEducation")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DoctorEducation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "EducationID")
    private Integer educationId;

    @Column(name = "DoctorID", nullable = false)
    private Integer doctorId;

    @Column(name = "Degree", nullable = false)
    private String degree;

    @Column(name = "Institution", nullable = false)
    private String institution;

    @Column(name = "Graduation")
    private Integer graduation;

    @Column(name = "Description", columnDefinition = "varchar(max)")
    private String description;

    @ManyToOne
    @JoinColumn(name = "DoctorID", insertable = false, updatable = false)
    private Doctor doctor;
}