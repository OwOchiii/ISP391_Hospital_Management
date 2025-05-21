package orochi.model;

import jakarta.persistence.*;
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

    @Column(name = "RoomNumber", nullable = false, unique = true)
    private String roomNumber;

    @Column(name = "RoomName", nullable = false)
    private String roomName;

    @Column(name = "Type", nullable = false)
    private String type;

    @Column(name = "Capacity", nullable = false, columnDefinition = "int DEFAULT 1")
    private Integer capacity;

    @Column(name = "Status", nullable = false, columnDefinition = "varchar(20) DEFAULT 'Available'")
    private String status;

    @Column(name = "DepartmentID", nullable = false)
    private Integer departmentId;

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
