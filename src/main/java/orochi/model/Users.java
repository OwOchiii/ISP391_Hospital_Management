package orochi.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDateTime;

@Entity
@Table(name = "Users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@DynamicUpdate
public class Users {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "UserID")
    private Integer userId;

    @Column(name = "FullName", nullable = false)
    private String fullName;

    @Column(name = "Email", unique = true)
    private String email;

    @Column(name = "PasswordHash")
    private String passwordHash;

    @Column(name = "PhoneNumber", unique = true)
    private String phoneNumber;

    @Column(name = "AvatarUrl")
    private String avatarUrl;

    @Column(name = "RoleID", nullable = false)
    private Integer roleId;

    @Column(name = "IsGuest", nullable = false, columnDefinition = "bit default 0")
    private boolean isGuest;

    @ManyToOne
    @JoinColumn(name = "RoleID", insertable = false, updatable = false)
    private Role role;

    @OneToOne(mappedBy = "user")
    private Patient patient;

    @OneToOne(mappedBy = "user")
    private Doctor doctor;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "Status", nullable = false, columnDefinition = "varchar(10) DEFAULT 'Active'")
    private String status = "Active";

    // Optional: Use JPA's @PrePersist to set the timestamp automatically
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = "Active";
        }
    }

    @Override
    public String toString() {
        return "Users{" +
                "userId=" + userId +
                ", fullName='" + fullName + '\'' +
                ", email='" + email + '\'' +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", isGuest=" + isGuest +
                ", roleId=" + (role != null ? role.getRoleId() : null) +
                ", createdAt=" + createdAt +
                '}';
    }
}
