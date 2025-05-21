package orochi.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "AuditLog")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "LogID")
    private Integer logId;

    @Column(name = "UserID", nullable = false)
    private Integer userId;

    @Column(name = "TableName", nullable = false, length = 100)
    private String tableName;

    @Column(name = "RecordID", nullable = false)
    private Integer recordId;

    @Column(name = "Action", nullable = false, length = 50)
    private String action;

    @Column(name = "OldValue", columnDefinition = "varchar(max)")
    private String oldValue;

    @Column(name = "NewValue", columnDefinition = "varchar(max)")
    private String newValue;

    @Column(name = "Timestamp", nullable = false)
    private LocalDateTime timestamp = LocalDateTime.now();

    @ManyToOne
    @JoinColumn(name = "UserID", insertable = false, updatable = false)
    private Users user;
}
