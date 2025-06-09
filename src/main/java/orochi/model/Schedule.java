package orochi.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "Schedule")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Schedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ScheduleID")
    private Integer scheduleId;

    @Column(name = "DoctorID", nullable = false)
    private Integer doctorId;

    @Column(name = "RoomID", nullable = false)
    private Integer roomId;

    @Column(name = "PatientID")
    private Integer patientId;

    @Column(name = "AppointmentID")
    private Integer appointmentId;

    @Column(name = "ScheduleDate", nullable = false)
    private LocalDate scheduleDate;

    @Column(name = "startTime", nullable = false)
    private LocalTime startTime;

    @Column(name = "endTime", nullable = false)
    private LocalTime endTime;

    @Column(name = "EventType", nullable = false, length = 20)
    private String eventType; // "appointment", "oncall", "break"

    @Column(name = "Description", length = 255)
    private String description;

    @Column(name = "IsCompleted", nullable = false)
    private Boolean isCompleted = false;

    @ManyToOne
    @JoinColumn(name = "DoctorID", insertable = false, updatable = false)
    private Doctor doctor;

    @ManyToOne
    @JoinColumn(name = "RoomID", insertable = false, updatable = false)
    private Room room;

    @ManyToOne
    @JoinColumn(name = "PatientID", insertable = false, updatable = false)
    private Patient patient;

    @OneToOne
    @JoinColumn(name = "AppointmentID", insertable = false, updatable = false)
    private Appointment appointment;
}
