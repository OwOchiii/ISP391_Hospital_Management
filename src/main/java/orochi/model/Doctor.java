package orochi.model;

import com.fasterxml.jackson.annotation.*;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Entity
@Table(name = "Doctor")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "doctorId")
public class Doctor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "DoctorID")
    private Integer doctorId;

    @Column(name = "UserID", unique = true, nullable = false)
    private Integer userId;

    @Column(name = "BioDescription", columnDefinition = "varchar(max)")
    private String bioDescription;

    @OneToOne
    @JoinColumn(name = "UserID", insertable = false, updatable = false)
    private Users user;

    @OneToMany(mappedBy = "doctor")
    @JsonIgnore
    private List<DoctorEducation> educations;

    @OneToMany(mappedBy = "doctor")
    @JsonIgnore
    private List<Appointment> appointments;

    @OneToMany(mappedBy = "doctor")
    @JsonIgnore
    private List<Schedule> schedules;

    @OneToMany(mappedBy = "doctor")
    @JsonIgnore
    private List<MedicalResult> medicalResults;

    @OneToMany(mappedBy = "orderBy")
    @JsonIgnore
    private List<MedicalOrder> medicalOrders;

    @OneToMany(mappedBy = "headDoctor")
    @JsonIgnore
    private List<Department> departmentsLed;

    @OneToMany(mappedBy = "doctor")
    @JsonIgnore
    private List<Prescription> prescriptions;

    @ManyToMany
    @JoinTable(
        name = "DoctorSpecialization",
        joinColumns = @JoinColumn(name = "DoctorID"),
        inverseJoinColumns = @JoinColumn(name = "SpecID")
    )
    @JsonIgnore
    private List<Specialization> specializations;
}