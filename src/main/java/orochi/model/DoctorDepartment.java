package orochi.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "DoctorDepartment")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DoctorDepartment {

    @EmbeddedId
    private DoctorDepartmentId id;

    @ManyToOne
    @MapsId("doctorId")
    @JoinColumn(name = "DoctorID")
    private Doctor doctor;

    @ManyToOne
    @MapsId("departmentId")
    @JoinColumn(name = "DepartmentID")
    private Department department;

    @Column(name = "IsPrimary")
    private boolean isPrimary;

    @Column(name = "JoinDate")
    private LocalDate joinDate;
}