package orochi.model;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DoctorSpecializationId implements Serializable {

    private Integer doctorId;
    private Integer specId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DoctorSpecializationId that = (DoctorSpecializationId) o;

        if (doctorId != null ? !doctorId.equals(that.doctorId) : that.doctorId != null) return false;
        return specId != null ? specId.equals(that.specId) : that.specId == null;
    }

    @Override
    public int hashCode() {
        int result = doctorId != null ? doctorId.hashCode() : 0;
        result = 31 * result + (specId != null ? specId.hashCode() : 0);
        return result;
    }
}
