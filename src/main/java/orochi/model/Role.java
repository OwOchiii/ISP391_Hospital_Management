package orochi.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Entity
@Table(name = "Role")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "RoleID")
    private Integer roleId;

    @Column(name = "RoleName", nullable = false)
    private String roleName;

    @Column(name = "Description", columnDefinition = "varchar(max)")
    private String description;

    @Column(name = "Permission", columnDefinition = "varchar(max)")
    private String permission;

    @OneToMany(mappedBy = "role")
    private List<Users> users;
}