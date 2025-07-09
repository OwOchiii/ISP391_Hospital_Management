package orochi.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicUpdate;

@Entity
@Table(name = "PatientContact")
@Data
@NoArgsConstructor
@AllArgsConstructor
@DynamicUpdate
public class PatientContact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ContactID")
    private Integer contactId;

    @Column(name = "PatientID", nullable = false)
    private Integer patientId;

    @Column(name = "AddressType", nullable = false, length = 50)
    private String addressType;

    @Column(name = "StreetAddress", nullable = false)
    private String streetAddress;

    @Column(name = "City", nullable = false, length = 100)
    private String city;

    @Column(name = "State", length = 100)
    private String state;

    @Column(name = "PostalCode", length = 20)
    private String postalCode;

    @Column(name = "Country", nullable = false, length = 100)
    private String country;

    @ManyToOne
    @JoinColumn(name = "PatientID", insertable = false, updatable = false)
    private Patient patient;

    @Override
    public String toString() {
        return "PatientContact{" +
                "contactId=" + contactId +
                ", patientId=" + patientId +
                ", addressType='" + addressType + '\'' +
                ", streetAddress='" + streetAddress + '\'' +
                ", city='" + city + '\'' +
                ", country='" + country + '\'' +
                '}';
    }
}
