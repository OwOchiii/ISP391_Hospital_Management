package orochi.model;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

@Data
public class PatientForm {
    private Integer patientId;
    private Integer userId;

    @NotBlank(message="Họ tên không được để trống")
    private String fullName;

    @NotBlank(message="Email không được để trống")
    private String email;

    @NotBlank(message="Số điện thoại không được để trống")
    private String phoneNumber;

    @NotBlank(message="Trạng thái không được để trống")
    private String status;

    // Trường Patient-specific
    private LocalDate dateOfBirth;
    private String gender;
    private String address;
    private String description;
}
