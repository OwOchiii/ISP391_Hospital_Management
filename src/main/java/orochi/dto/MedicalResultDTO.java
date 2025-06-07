package orochi.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * DTO for returning MedicalResult data through REST API endpoints
 * to avoid circular reference issues with Jackson serialization
 */
@Setter
@Getter
public class MedicalResultDTO {
    // Getters and setters
    private Integer resultId;
    private Integer appointmentId;
    private Integer doctorId;
    private String doctorName;
    private LocalDateTime resultDate;
    private String description;
    private String fileUrl;
    private String status;

    public MedicalResultDTO() {
    }

    public MedicalResultDTO(orochi.model.MedicalResult result) {
        this.resultId = result.getResultId();
        this.appointmentId = result.getAppointmentId();
        this.doctorId = result.getDoctorId();
        // Only set doctor name if doctor is available
        if (result.getDoctor() != null && result.getDoctor().getUser() != null) {
            this.doctorName = result.getDoctor().getUser().getFullName();
        }
        this.resultDate = result.getResultDate();
        this.description = result.getDescription();
        this.fileUrl = result.getFileUrl();
        this.status = result.getStatus();
    }

}
