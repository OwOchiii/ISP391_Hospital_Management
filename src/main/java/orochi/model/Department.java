package orochi.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Table(name = "Department")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "DepartmentID")
    private Integer departmentId;

    @Column(name = "DeptName", nullable = false)
    private String deptName;

    @Column(name = "Description", length = Integer.MAX_VALUE)
    private String description;

    @Column(name = "HeadDoctorID")
    private Integer headDoctorId;

    @ManyToOne
    @JoinColumn(name = "HeadDoctorID", insertable = false, updatable = false)
    private Doctor headDoctor;

    @OneToMany(mappedBy = "department")
    private List<Room> rooms;

    @OneToMany(mappedBy = "assignedToDepartment")
    private List<MedicalOrder> medicalOrders;
}
