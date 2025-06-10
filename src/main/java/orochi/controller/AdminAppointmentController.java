/*package orochi.controller;

import orochi.config.CustomUserDetails;
import orochi.model.Appointment;
import orochi.config.CustomUserDetails;
import orochi.service.AppointmentMetricService;
import orochi.service.AppointmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/appointments")
public class AdminAppointmentController {

    private final AppointmentService appointmentService;
    private final AppointmentMetricService appointmentMetricService;

    @Autowired
    public AdminAppointmentController(AppointmentService appointmentService,
                                      AppointmentMetricService appointmentMetricService) {
        this.appointmentService = appointmentService;
        this.appointmentMetricService = appointmentMetricService;
    }

    @GetMapping
    public String showAppointments(@RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "10") int size,
                                   @RequestParam(defaultValue = "ALL") String status,
                                   @RequestParam(required = false) String search,
                                   @AuthenticationPrincipal CustomUserDetails userDetails,
                                   Model model) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Appointment> appointments;

        if (userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_DOCTOR"))) {
            Integer doctorId = userDetails.getDoctorId();
            if (search != null && !search.isBlank()) {
                model.addAttribute("appointments");
                model.addAttribute("currentStatus", "ALL");
            } else {
                appointments = appointmentService.getAppointmentsByDoctorId(doctorId, pageable);
                model.addAttribute("appointments", appointments);
                model.addAttribute("currentStatus", "ALL");
            }
        } else {
            if ("ALL".equals(status)) {
                appointments = appointmentService.getAllAppointments(pageable);
            } else {
                Appointment.AppointmentStatus appointmentStatus = Appointment.AppointmentStatus.valueOf(status);
                appointments = appointmentService.getTotalAppointments(appointmentStatus, pageable);
            }
            model.addAttribute("appointments", appointments);
            model.addAttribute("currentStatus", status);
        }

        model.addAttribute("totalAppointments", appointmentMetricService.getTotalAppointments());
        model.addAttribute("inProgressCount", appointmentMetricService.getTotalAppointments(Appointment.AppointmentStatus.IN_PROGRESS));
        model.addAttribute("completedCount", appointmentMetricService.getTotalAppointments(Appointment.AppointmentStatus.COMPLETED));
        model.addAttribute("rejectedCount", appointmentMetricService.getTotalAppointments(Appointment.AppointmentStatus.REJECTED));
        model.addAttribute("search", search);

        // Add Chart.js data
        model.addAttribute("chartData", getChartData());

        return "appointments";
    }

    @PostMapping("/updateStatus")
    public String updateAppointmentStatus(@RequestParam Integer appointmentId,
                                          @RequestParam String status,
                                          @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_DOCTOR") || a.getAuthority().equals("ROLE_RECEPTIONIST"))) {
            throw new SecurityException("Unauthorized action");
        }
        Appointment.AppointmentStatus appointmentStatus = Appointment.AppointmentStatus.valueOf(status);
        appointmentService.updateAppointmentStatus(appointmentId, appointmentStatus);
        return "redirect:/admin/appointments";
    }

    private String getChartData() {
        return "{"
                + "\"labels\": [\"In Progress\", \"Completed\", \"Rejected\", \"Scheduled\", \"Cancelled\", \"Pending\"],"
                + "\"datasets\": [{"
                + "\"label\": \"Appointments\","
                + "\"data\": ["
                + appointmentMetricService.getAppointmentsByStatus(Appointment.AppointmentStatus.IN_PROGRESS) + ","
                + appointmentMetricService.getAppointmentsByStatus(Appointment.AppointmentStatus.COMPLETED) + ","
                + appointmentMetricService.getAppointmentsByStatus(Appointment.AppointmentStatus.REJECTED) + ","
                + appointmentMetricService.getAppointmentsByStatus(Appointment.AppointmentStatus.SCHEDULED) + ","
                + appointmentMetricService.getAppointmentsByStatus(Appointment.AppointmentStatus.CANCELLED) + ","
                + appointmentMetricService.getAppointmentsByStatus(Appointment.AppointmentStatus.PENDING)
                + "],"
                + "\"backgroundColor\": [\"#36A2EB\", \"#4CAF50\", \"#FF6384\", \"#FFCE56\", \"#E7E9ED\", \"#9966FF\"],"
                + "\"borderColor\": [\"#36A2EB\", \"#4CAF50\", \"#FF6384\", \"#FFCE56\", \"#E7E9ED\", \"#9966FF\"],"
                + "\"borderWidth\": 1"
                + "}]"
                + "}";
    }
}*/
