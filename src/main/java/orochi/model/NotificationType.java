package orochi.model;

/**
 * Các loại thông báo do Admin gửi.
 */
public enum NotificationType {
    /**
     * Thông báo chung (ví dụ: tin tức, bảo trì hệ thống…)
     */
    AdminNotice,

    /**
     * Cập nhật liên quan đến thanh toán, hóa đơn
     */
    BillingUpdate,

    /**
     * Nhắc nhở (hẹn lịch, công việc cần làm…)
     */
    Reminder,

    /**
     * Cảnh báo bảo mật (đổi mật khẩu, truy cập bất thường…)
     */
    SecurityAlert
}