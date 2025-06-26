package orochi.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;


/**
 * Form-backing bean cho việc Add/Edit Doctor.
 * Gom các thuộc tính từ bảng Users và Doctor.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DoctorForm {
    /** Khóa chính của Doctor (null khi tạo mới) */
    private Integer doctorId;

    /** Khóa chính của Users */
    private Integer userId;

    @NotBlank(message = "Full Name không được để trống")
    private String fullName;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    private String email;

    @NotBlank(message = "Phone Number không được để trống")
    @Pattern(regexp = "^\\d{10,15}$", message = "Phone number phải là 10–15 chữ số")
    private String phoneNumber;

    @NotNull(message = "Status bắt buộc chọn")
    private String status;  // ACTIVE / LOCKED

    private String bioDescription;
}
