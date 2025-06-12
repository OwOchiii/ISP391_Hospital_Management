package orochi.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DoctorSpecializationId implements java.io.Serializable {

    @Column(name = "DoctorID")
    private Integer doctorId;

    @Column(name = "SpecID")
    private Integer specId;

    // Getters and Setters
    public Integer getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(Integer doctorId) {
        this.doctorId = doctorId;
    }

    public Integer getSpecId() {
        return specId;
    }

    public void setSpecId(Integer specId) {
        this.specId = specId;
    }

    // equals and hashCode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DoctorSpecializationId that = (DoctorSpecializationId) o;
        return Objects.equals(doctorId, that.doctorId) && Objects.equals(specId, that.specId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(doctorId, specId);
    }
}
