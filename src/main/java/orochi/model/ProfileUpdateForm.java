package orochi.model;

import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
public class ProfileUpdateForm {
    @NotBlank(message = "Full Name is required")
    @Size(max = 255, message = "Full Name must not exceed 255 characters")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;

    @NotBlank(message = "Phone Number is required")
    @Pattern(regexp = "^0\\d{9}$|^0\\d{11}$", message = "Phone number must start with 0 and be 10 or 12 digits long")
    private String phoneNumber;

    @NotNull(message = "Date of Birth is required")
    @Past(message = "Date of Birth must be in the past")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateOfBirth;

    @NotBlank(message = "Gender is required")
    @Pattern(regexp = "Male|Female|Other", message = "Gender must be Male, Female, or Other")
    private String gender;

    @Size(max = 250, message = "Description must not exceed 250 characters")
    private String description;

    @NotBlank(message = "Street Address is required")
    @Size(max = 255, message = "Street Address must not exceed 255 characters")
    private String streetAddress;
}