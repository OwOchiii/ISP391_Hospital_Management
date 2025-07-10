package orochi.model;

import jakarta.persistence.*;
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

    @Column(name = "UserID", nullable = false)
    private Integer userId;

    @Column(name = "Message", nullable = false, length = Integer.MAX_VALUE)
    private String message;

    @Column(name = "Type", nullable = false, length = 50)
    private String type;

    @Column(name = "IsRead", nullable = false)
    private boolean isRead;

    @Column(name = "CreatedAt", nullable = false, columnDefinition = "datetime DEFAULT GETDATE()")
    private LocalDateTime createdAt;

    @ManyToOne
    @JoinColumn(name = "UserID", insertable = false, updatable = false)
    private Users user;

    public boolean isRead() {
        return isRead;
    }
}
