package orochi.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDateTime;

@Entity
@Table(name = "Notification")
@DynamicUpdate
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "NotificationID")
    private Integer notificationId;

    @NotNull(message = "User ID is required")
    @Min(value = 1, message = "User ID must be positive")
    @Column(name = "UserID", nullable = false)
    private Integer userId;

    @NotBlank(message = "Message cannot be empty")
    @Column(name = "Message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @NotNull(message = "Type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "Type", nullable = false, length = 50)
    private NotificationType type;

    @Column(name = "IsRead", nullable = false)
    private boolean isRead = false;

    @Column(name = "CreatedAt", nullable = false, columnDefinition = "datetime DEFAULT GETDATE()")
    private LocalDateTime createdAt;

    @ManyToOne
    @JoinColumn(name = "UserID", insertable = false, updatable = false)
    private Users user;

}
