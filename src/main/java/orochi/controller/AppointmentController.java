package orochi.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import orochi.dto.AppointmentDTO;
import orochi.model.Appointment;
import orochi.repository.AppointmentRepository;
import orochi.service.AppointmentService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
public class AppointmentController {

    private static final Logger logger = LoggerFactory.getLogger(AppointmentController.class);

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private AppointmentService appointmentService;

    @GetMapping("/api/booked-times")
    public List<String> getBookedTimes(@RequestParam Integer doctorId, @RequestParam String date) {
        logger.info("Fetching booked times for doctorId: {}, date: {}", doctorId, date);
        LocalDate appointmentDate = LocalDate.parse(date);
        LocalDateTime startOfDay = appointmentDate.atStartOfDay();
        LocalDateTime endOfDay = appointmentDate.atTime(23, 59, 59);
        List<Appointment> appointments = appointmentRepository.findByDoctorIdAndDateTimeBetween(doctorId, startOfDay, endOfDay);
        return appointments.stream()
                .map(appointment -> appointment.getDateTime().toLocalTime().toString())
                .collect(Collectors.toList());
    }

    @PostMapping("/api/appointments")
    public ResponseEntity<?> createAppointment(@RequestBody AppointmentDTO appointmentDTO, Authentication authentication) {
        try {
            logger.info("Received appointment request for doctorId: {}", appointmentDTO.getDoctorId());
            Appointment appointment = appointmentService.saveAppointment(appointmentDTO);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Error creating appointment: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    // Error response class for better error handling
    private static class ErrorResponse {
        private String message;

        public ErrorResponse(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }
}