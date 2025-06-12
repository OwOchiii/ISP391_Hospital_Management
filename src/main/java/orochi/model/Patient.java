package orochi.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "Patient")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PatientID")
    private Integer patientId;

    @Column(name = "UserID", nullable = false, unique = true)
    private Integer userId;

    @Column(name = "dateOfBirth")
    private LocalDate dateOfBirth;

    @Column(name = "gender")
    private String gender;

    @Column(name = "description", length = Integer.MAX_VALUE)
    private String description;

    @OneToOne
    @JoinColumn(name = "UserID", insertable = false, updatable = false)
    private Users user;

    @OneToMany(mappedBy = "patient")
    private List<PatientContact> contacts;

    @OneToMany(mappedBy = "patient")
    private List<Appointment> appointments;

    @OneToMany(mappedBy = "patient")
    private List<Prescription> prescriptions;

    @Transient
    private Integer age;

    @Transient
    private String status;

    @Transient
    private Integer totalAppointments;

    @Transient
    private Integer upcomingAppointments;

    @Transient
    private String lastVisit;

    public String getFullName() {
        return user != null ? user.getFullName() : null;
    }

    /**
     * Gets the formatted primary address of the patient.
     * If no primary address exists, returns the latest contact address.
     * @return Formatted address string or "No address available" if none exists
     */
    public String getPrimaryAddress() {
        if (contacts == null || contacts.isEmpty()) {
            return "No address available";
        }

        // First try to find a contact with addressType "PRIMARY" or similar
        PatientContact primaryContact = contacts.stream()
                .filter(c -> c.getAddressType() != null &&
                       (c.getAddressType().equalsIgnoreCase("PRIMARY") ||
                        c.getAddressType().equalsIgnoreCase("MAIN")))
                .findFirst()
                .orElse(null);

        // If no primary address is found, just use the first one in the list
        // (assuming the list might be ordered with the latest first)
        if (primaryContact == null && !contacts.isEmpty()) {
            primaryContact = contacts.get(0);
        }

        if (primaryContact != null) {
            StringBuilder address = new StringBuilder();
            address.append(primaryContact.getStreetAddress());

            if (primaryContact.getCity() != null && !primaryContact.getCity().isEmpty()) {
                address.append(", ").append(primaryContact.getCity());
            }

            if (primaryContact.getState() != null && !primaryContact.getState().isEmpty()) {
                address.append(", ").append(primaryContact.getState());
            }

            if (primaryContact.getPostalCode() != null && !primaryContact.getPostalCode().isEmpty()) {
                address.append(" ").append(primaryContact.getPostalCode());
            }

            if (primaryContact.getCountry() != null && !primaryContact.getCountry().isEmpty()) {
                address.append(", ").append(primaryContact.getCountry());
            }

            return address.toString();
        }

        return "No address available";
    }
}
