package orochi.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class MedicalResultSummaryDTO {
    // -----------------------
    // MedicalResult
    // -----------------------
    private Integer resultId;
    private LocalDateTime resultDate;
    private String status;
    private String fileUrl;

    // -----------------------
    // Appointment
    // -----------------------
    private LocalDateTime appointmentDateTime;
    private String appointmentRoom;    // từ Room.getRoomName()
    private String appointmentEmail;
    private String appointmentPhone;

    // -----------------------
    // Patient & Doctor
    // -----------------------
    private String patientName;
    private String doctorName;
}
