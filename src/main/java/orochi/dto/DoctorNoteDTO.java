package orochi.dto;

import java.time.LocalDateTime;

/**
 * DTO class for DoctorNote to avoid circular references in JSON serialization
 */
public class DoctorNoteDTO {
    private Integer noteId;
    private Integer appointmentId;
    private Integer doctorId;
    private String noteContent;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String doctorName;

    // Constructors
    public DoctorNoteDTO() {
    }

    public DoctorNoteDTO(Integer noteId, Integer appointmentId, Integer doctorId,
                        String noteContent, LocalDateTime createdAt,
                        LocalDateTime updatedAt, String doctorName) {
        this.noteId = noteId;
        this.appointmentId = appointmentId;
        this.doctorId = doctorId;
        this.noteContent = noteContent;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.doctorName = doctorName;
    }

    // Getters and setters
    public Integer getNoteId() {
        return noteId;
    }

    public void setNoteId(Integer noteId) {
        this.noteId = noteId;
    }

    public Integer getAppointmentId() {
        return appointmentId;
    }

    public void setAppointmentId(Integer appointmentId) {
        this.appointmentId = appointmentId;
    }

    public Integer getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(Integer doctorId) {
        this.doctorId = doctorId;
    }

    public String getNoteContent() {
        return noteContent;
    }

    public void setNoteContent(String noteContent) {
        this.noteContent = noteContent;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
    }
}
