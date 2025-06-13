package orochi.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "Patient")
@Data
@NoArgsConstructor
@AllArgsConstructor
@DynamicUpdate
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

    @Override
    public String toString() {
        return "Patient{" +
                "patientId=" + patientId +
                ", userId=" + (user != null ? user.getUserId() : null) +
                ", dateOfBirth=" + dateOfBirth +
                ", gender='" + gender + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}
