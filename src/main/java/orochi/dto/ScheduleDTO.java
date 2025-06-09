package orochi.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleDTO {
    private Integer scheduleId;
    private Integer doctorId;
    private Integer roomId;
    private Integer patientId;
    private Integer appointmentId;
    private LocalDate scheduleDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String eventType; // "appointment", "oncall", "break"
    private String description;

    // Additional display fields
    private String patientName;
    private String roomName;
    private String doctorName;
}
