package orochi.model;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Entity class representing doctor's notes for patient appointments.
 */
@Getter
@Entity
@Table(name = "DoctorNotes")
public class DoctorNote {

    // Getters and Setters
    @Setter
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "NoteID")
    private Integer noteId;

    @Setter
    @Column(name = "AppointmentID", nullable = false)
    private Integer appointmentId;

    @Setter
    @Column(name = "DoctorID", nullable = false)
    private Integer doctorId;

    @Setter
    @Column(name = "NoteContent", nullable = false, columnDefinition = "VARCHAR(MAX)")
    private String noteContent;

    @Setter
    @Column(name = "CreatedAt", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Setter
    @Column(name = "UpdatedAt")
    private LocalDateTime updatedAt;

    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AppointmentID", insertable = false, updatable = false)
    private Appointment appointment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "DoctorID", insertable = false, updatable = false)
    private Doctor doctor;

    // Constructors
    public DoctorNote() {
        // Default constructor required by JPA
    }

    public DoctorNote(Integer appointmentId, Integer doctorId, String noteContent) {
        this.appointmentId = appointmentId;
        this.doctorId = doctorId;
        this.noteContent = noteContent;
        this.createdAt = LocalDateTime.now();
    }

    // Pre-persist and pre-update callbacks
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "DoctorNote{" +
                "noteId=" + noteId +
                ", appointmentId=" + appointmentId +
                ", doctorId=" + doctorId +
                ", noteContent='" + (noteContent != null ? noteContent.substring(0, Math.min(noteContent.length(), 50)) + "..." : null) + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
