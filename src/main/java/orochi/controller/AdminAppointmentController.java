/*package orochi.controller;

import orochi.config.CustomUserDetails;
import orochi.model.Appointment;
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

    // Status constants that match the database constraints
    public static final String STATUS_SCHEDULED = "Scheduled";
    public static final String STATUS_COMPLETED = "Completed";
    public static final String STATUS_CANCELLED = "Cancel";
    public static final String STATUS_PENDING = "Pending";

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
                appointments = appointmentService.searchAppointmentsByDoctorId(doctorId, search, pageable);
            } else {
                appointments = appointmentService.getAppointmentsByDoctorId(doctorId, pageable);
            }
            model.addAttribute("currentStatus", "ALL");
        } else {
            if ("ALL".equals(status)) {
                appointments = appointmentService.getAllAppointments(pageable);
            } else {
                appointments = appointmentService.getAppointmentsByStatus(status, pageable);
            }
            model.addAttribute("currentStatus", status);
        }

        model.addAttribute("appointments", appointments);
        model.addAttribute("totalAppointments", appointmentMetricService.getTotalAppointments());
        model.addAttribute("inProgressCount", appointmentMetricService.getTotalAppointments(STATUS_PENDING));
        model.addAttribute("completedCount", appointmentMetricService.getTotalAppointments(STATUS_COMPLETED));
        model.addAttribute("rejectedCount", appointmentMetricService.getTotalAppointments(STATUS_CANCELLED));
        model.addAttribute("search", search);

        // Add Chart.js data
        model.addAttribute("chartData", getChartData());

        return "admin/appointments";
    }

    @PostMapping("/updateStatus")
    public String updateAppointmentStatus(@RequestParam Integer appointmentId,
                                          @RequestParam String status,
                                          @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails.getAuthorities().stream().noneMatch(a ->
                a.getAuthority().equals("ROLE_ADMIN") ||
                a.getAuthority().equals("ROLE_DOCTOR") ||
                a.getAuthority().equals("ROLE_RECEPTIONIST"))) {
            throw new SecurityException("Unauthorized action");
        }

        // Just pass the string status - no enum conversion
        appointmentService.updateAppointmentStatus(appointmentId, status);
        return "redirect:/admin/appointments";
    }

    private String getChartData() {
        return "{"
                + "\"labels\": [\"Pending\", \"Completed\", \"Cancelled\", \"Scheduled\"],"
                + "\"datasets\": [{"
                + "\"label\": \"Appointments\","
                + "\"data\": ["
                + appointmentMetricService.getAppointmentsByStatus(STATUS_PENDING) + ","
                + appointmentMetricService.getAppointmentsByStatus(STATUS_COMPLETED) + ","
                + appointmentMetricService.getAppointmentsByStatus(STATUS_CANCELLED) + ","
                + appointmentMetricService.getAppointmentsByStatus(STATUS_SCHEDULED)
                + "],"
                + "\"backgroundColor\": [\"#36A2EB\", \"#4CAF50\", \"#FF6384\", \"#FFCE56\"],"
                + "\"borderColor\": [\"#36A2EB\", \"#4CAF50\", \"#FF6384\", \"#FFCE56\"],"
                + "\"borderWidth\": 1"
                + "}]"
                + "}";
    }
}*/
