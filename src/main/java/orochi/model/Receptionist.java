package orochi.model;

import com.fasterxml.jackson.annotation.*;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "Receptionist")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "receptionistId")
public class Receptionist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ReceptionistID")
    private Integer receptionistId;

    @Column(name = "UserID", unique = true, nullable = false)
    private Integer userId;

    @Column(name = "ImageURL")
    private String imageUrl;

    @OneToOne
    @JoinColumn(name = "UserID", insertable = false, updatable = false)
    private Users user;
}
