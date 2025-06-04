package orochi.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Form‐backing bean cho việc Add/Edit Doctor.
 * Gom các thuộc tính từ bảng Users và Doctor.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DoctorForm {
    /** Khóa chính của Doctor (null khi tạo mới) */
    private Integer doctorId;

    /** Khóa chính của Users (sẽ lấy từ doctor.userId) */
    private Integer userId;

    /** Các trường của Users */
    private String fullName;
    private String email;
    private String phoneNumber;
    private String status;          // ACTIVE / LOCKED

    /** Trường riêng của Doctor */
    private String bioDescription;
}
