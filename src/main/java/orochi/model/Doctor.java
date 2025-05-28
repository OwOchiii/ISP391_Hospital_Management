package orochi.model;

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
public class Doctor {

    @Id
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
    private List<DoctorEducation> educations;

    @OneToMany(mappedBy = "doctor")
    private List<Appointment> appointments;

    @OneToMany(mappedBy = "doctor")
    private List<Schedule> schedules;

    @OneToMany(mappedBy = "doctor")
    private List<MedicalResult> medicalResults;

    @OneToMany(mappedBy = "orderBy")
    private List<MedicalOrder> medicalOrders;

    @OneToMany(mappedBy = "headDoctor")
    private List<Department> departmentsLed;

    @OneToMany(mappedBy = "doctor")
    private List<Prescription> prescriptions;

    @ManyToMany
    @JoinTable(
        name = "DoctorSpecialization",
        joinColumns = @JoinColumn(name = "DoctorID"),
        inverseJoinColumns = @JoinColumn(name = "SpecID")
    )
    private List<Specialization> specializations;
}

