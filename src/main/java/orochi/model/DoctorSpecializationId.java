package orochi.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DoctorSpecializationId implements java.io.Serializable {

    @Column(name = "UserID")
    private Integer userId;

    @Column(name = "SpecID")
    private Integer specId;
}
