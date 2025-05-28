package orochi.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "DoctorSpecialization")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DoctorSpecialization {

    @EmbeddedId
    private DoctorSpecializationId id;

    @ManyToOne
    @MapsId("doctorId")
    @JoinColumn(name = "DoctorID")
    private Doctor doctor;

    @ManyToOne
    @MapsId("specId")
    @JoinColumn(name = "SpecID")
    private Specialization specialization;
}

