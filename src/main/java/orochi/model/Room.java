package orochi.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Table(name = "Room")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "RoomID")
    private Integer roomId;

    @NotBlank(message = "Room number cannot be left blank")
    @Size(max = 20, message = "Room number can be maximum 20 characters")
    @Pattern(regexp = "^(?!.*<script>).*$", message = "Do not type script tag")
    @Column(name = "RoomNumber", nullable = false, unique = true)
    private String roomNumber;

    @NotBlank(message = "Room name cannot be left blank")
    @Size(max = 50, message = "Room name can be maximum 50 characters")
    @Pattern(regexp = "^(?!.*<script>).*$", message = "Do not type script tag")
    @Column(name = "RoomName", nullable = false)
    private String roomName;

    @Column(name = "Type", nullable = false)
    private String type;

    @Min(value = 1, message = "Capacity must be ≥ 1")
    @Max(value = 50, message = "Capacity must be ≤ 50")
    @Column(name = "Capacity", nullable = false, columnDefinition = "int DEFAULT 1")
    private Integer capacity;

    @NotBlank
    @Pattern(regexp = "^(Available|Occupied)$", message = "Status is not valid")
    @Column(name = "Status", nullable = false, columnDefinition = "varchar(20) DEFAULT 'Available'")
    private String status;

    @NotNull(message = "You must choose a department")
    @Column(name = "DepartmentID", nullable = false)
    private Integer departmentId;

    @Size(max = 500, message = "Notes can be maximum 500 characters")
    @Pattern(regexp = "^(?!.*<script>).*$", message = "Do not type script tag")
    @Column(name = "Notes", length = Integer.MAX_VALUE)
    private String notes;

    @ManyToOne
    @JoinColumn(name = "DepartmentID", insertable = false, updatable = false)
    private Department department;

    @OneToMany(mappedBy = "room")
    private List<Appointment> appointments;

    @OneToMany(mappedBy = "room")
    private List<Schedule> schedules;
}
