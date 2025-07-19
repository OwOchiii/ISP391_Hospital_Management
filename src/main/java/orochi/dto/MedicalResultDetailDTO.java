package orochi.dto;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class MedicalResultDetailDTO {
    // -----------------------
    // MedicalResult
    // -----------------------
    private Integer resultId;
    private LocalDateTime resultDate;
    private String description;
    private String status;
    private String fileUrl;

    // -----------------------
    // Appointment
    // -----------------------
    private Integer appointmentId;
    private LocalDateTime appointmentDateTime;
    private String appointmentStatus;
    private String appointmentDescription;
    private String appointmentEmail;
    private String appointmentPhone;
    private String roomName;

    // -----------------------
    // Patient
    // -----------------------
    private Integer patientId;
    private String patientName;
    private String patientEmail;
    private String patientGender;
    private LocalDate patientDob;

    // -----------------------
    // Doctor
    // -----------------------
    private Integer doctorId;
    private String doctorName;
    private String doctorContact;
    private String doctorBio;

    // -----------------------
    // Orders
    // -----------------------
    @Data
    public static class OrderInfo {
        private Integer orderId;
        private String orderType;
        private String orderStatus;
        private LocalDate orderDate;
        private String assignedDept;
    }
    private List<OrderInfo> orders;
}
