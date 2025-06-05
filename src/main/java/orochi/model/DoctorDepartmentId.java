package orochi.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DoctorDepartmentId implements Serializable {

    @Column(name = "DoctorID")
    private Integer doctorId;

    @Column(name = "DepartmentID")
    private Integer departmentId;
}