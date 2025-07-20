package orochi.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
public class AppointmentFormDTO {
    private Integer appointmentId;

    @NotNull(message = "Patient ID is required")
    private Integer patientId;

    @NotNull(message = "Specialty is required")
    private Integer specialtyId;

    @NotNull(message = "Doctor selection is required")
    private Integer doctorId;


    private Integer roomId;

    @NotNull(message = "Appointment date is required")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate appointmentDate;

    @NotEmpty(message = "Appointment time is required")
    private String appointmentTime;

    @NotEmpty(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    private String email;

    @NotEmpty(message = "Phone number is required")
    @Pattern(regexp = "^0\\d{9}$|^0\\d{11}$", message = "Phone number must start with 0 and be either 10 or 12 digits")
    private String phoneNumber;

    private String description;

}
