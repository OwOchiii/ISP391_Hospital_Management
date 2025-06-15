package orochi.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import orochi.config.CustomUserDetails;
import orochi.model.Appointment;
import orochi.model.Patient;
import orochi.model.Users;
import orochi.repository.UserRepository;
import orochi.service.impl.DoctorServiceImpl;
import orochi.service.impl.ReceptionistService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.Year;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@RestController
@RequestMapping("/api")
public class ReceptionistApiController {

    private final ReceptionistService receptionistService;
    private final DoctorServiceImpl doctorService;
    private final UserRepository userRepository;

    public ReceptionistApiController(ReceptionistService receptionistService,
                                     DoctorServiceImpl doctorService,
                                     UserRepository userRepository) {
        this.receptionistService = receptionistService;
        this.doctorService = doctorService;
        this.userRepository = userRepository;
    }

    @GetMapping("/doctors")
    public ResponseEntity<?> getAllDoctors() {
        try {
            List<Map<String, Object>> doctors = doctorService.getAllDoctors().stream()
                    .map(doctor -> {
                        Map<String, Object> doctorMap = new HashMap<>();
                        doctorMap.put("id", doctor.getDoctorId());
                        doctorMap.put("name", doctor.getUser().getFullName());
                        return doctorMap;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(doctors);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error fetching doctors: " + e.getMessage());
        }
    }

    @GetMapping("/doctors/{id}")
    public ResponseEntity<?> getDoctorById(@PathVariable Integer id) {
        try {
            return doctorService.getDoctorById(id)
                    .map(doctor -> {
                        Map<String, Object> doctorDetails = new HashMap<>();
                        doctorDetails.put("id", doctor.getDoctorId());
                        doctorDetails.put("name", doctor.getUser().getFullName());
                        doctorDetails.put("specialty", doctor.getSpecializations());
                        doctorDetails.put("phone", doctor.getUser().getPhoneNumber());
                        doctorDetails.put("email", doctor.getUser().getEmail());
                        doctorDetails.put("experience", doctor.getEducations());
                        doctorDetails.put("bio", doctor.getBioDescription());
                        return ResponseEntity.ok(doctorDetails);
                    })
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error fetching doctor details: " + e.getMessage());
        }
    }

    @GetMapping("/patients")
    public ResponseEntity<?> getPatients(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "") String search) {
        try {
            // Implementing pagination and search
            int itemsPerPage = 12;
            List<Patient> allPatients = receptionistService.getAllPatients();

            // Filter by search term if provided
            if (!search.isEmpty()) {
                String searchLower = search.toLowerCase();
                allPatients = allPatients.stream()
                        .filter(p -> p.getUser().getFullName().toLowerCase().contains(searchLower) ||
                                (p.getUser().getPhoneNumber() != null && p.getUser().getPhoneNumber().toLowerCase().contains(searchLower)))
                        .collect(Collectors.toList());
            }

            // Calculate pagination
            int total = allPatients.size();
            int start = (page - 1) * itemsPerPage;
            int end = Math.min(start + itemsPerPage, total);

            List<Map<String, Object>> paginatedPatients = allPatients.subList(start, end).stream()
                    .map(patient -> {
                        Map<String, Object> patientMap = new HashMap<>();
                        patientMap.put("id", patient.getPatientId());
                        patientMap.put("name", patient.getUser().getFullName());
                        patientMap.put("phone", patient.getUser().getPhoneNumber());
                        return patientMap;
                    })
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("patients", paginatedPatients);
            response.put("total", total);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error fetching patients: " + e.getMessage());
        }
    }

    @GetMapping("/appointments")
    public ResponseEntity<?> getAppointments(
            @RequestParam String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "false") boolean fetchAll) {
        try {
            List<Appointment> allAppointments = receptionistService.getAllAppointments();

            // Filter by status
            List<Appointment> filteredAppointments = allAppointments.stream()
                    .filter(a -> a.getStatus().equalsIgnoreCase(status))
                    .collect(Collectors.toList());

            // Filter by search term if provided
            if (!search.isEmpty()) {
                String searchLower = search.toLowerCase();
                filteredAppointments = filteredAppointments.stream()
                        .filter(a -> a.getPatient().getUser().getFullName().toLowerCase().contains(searchLower) ||
                                (a.getPatient().getUser().getPhoneNumber() != null &&
                                 a.getPatient().getUser().getPhoneNumber().toLowerCase().contains(searchLower)))
                        .collect(Collectors.toList());
            }

            int total = filteredAppointments.size();
            List<Map<String, Object>> appointmentData;

            // If fetchAll is true, return all appointments without pagination
            if (fetchAll) {
                appointmentData = mapAppointmentsToResponse(filteredAppointments);
            } else {
                // Apply pagination
                int itemsPerPage = 5;
                int start = (page - 1) * itemsPerPage;
                int end = Math.min(start + itemsPerPage, total);

                appointmentData = mapAppointmentsToResponse(
                        filteredAppointments.subList(start, end)
                );
            }

            Map<String, Object> response = new HashMap<>();
            response.put("appointments", appointmentData);
            response.put("total", total);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error fetching appointments: " + e.getMessage());
        }
    }

    @PostMapping("/appointments/update")
    public ResponseEntity<?> updateAppointmentStatus(
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        try {
            // Verify user is a receptionist
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401).body("Unauthorized");
            }

            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Users user = userRepository.findById(userDetails.getUserId())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (!receptionistService.isReceptionist(user)) {
                return ResponseEntity.status(403).body("Forbidden: Only receptionists can update appointment status");
            }

            // Get parameters from request
            Integer appointmentId = (Integer) request.get("stt");
            String status = (String) request.get("status");

            // Validate inputs
            if (appointmentId == null || status == null || status.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Invalid request: appointment ID and status are required");
            }

            // Call the service to update appointment status
            boolean updated = receptionistService.updateAppointmentStatus(appointmentId, status);

            if (updated) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Appointment status updated successfully"
                ));
            } else {
                return ResponseEntity.status(404).body("Appointment not found with ID: " + appointmentId);
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Error updating appointment status: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error updating appointment status: " + e.getMessage());
        }
    }

    /**
     * Endpoint to serve dashboard data including patient and appointment statistics
     * @param dataType Type of data requested (patientStats, appointmentStatus, summaryData)
     * @param period Time period for statistics (day, month, 5years)
     * @return Dashboard statistics data
     */
    @GetMapping("/dashboard-data")
    public ResponseEntity<?> getDashboardData(
            @RequestParam String dataType,
            @RequestParam(defaultValue = "month") String period) {

        try {
            Map<String, Object> response = new HashMap<>();

            switch (dataType) {
                case "patientStats":
                    response.put("patientStats", getPatientStatistics(period));
                    break;

                case "appointmentStatus":
                    response.put("appointmentStatus", getAppointmentStatistics(period));
                    break;

                case "summaryData":
                    response.put("summary", getDashboardSummary());
                    break;

                default:
                    return ResponseEntity.badRequest().body("Invalid dataType parameter");
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error fetching dashboard data: " + e.getMessage());
        }
    }

    /**
     * Get patient statistics based on the specified time period
     */
    private Map<String, Object> getPatientStatistics(String period) {
        List<Patient> allPatients = receptionistService.getAllPatients();
        Map<String, Object> result = new HashMap<>();
        LocalDate now = LocalDate.now();

        // Generate data points based on period
        switch (period) {
            case "day":
                // Last 30 days patient count
                List<Integer> dailyStats = IntStream.rangeClosed(1, 30)
                    .mapToObj(day -> {
                        LocalDate targetDate = now.minusDays(30 - day);
                        return (int) allPatients.stream()
                                .filter(p -> p.getUser().getCreatedAt() != null &&
                                        p.getUser().getCreatedAt().toLocalDate().equals(targetDate))
                                .count();
                    })
                    .collect(Collectors.toList());
                result.put("data", dailyStats);
                break;

            case "month":
                // Monthly patient count for current year
                List<Integer> monthlyStats = IntStream.rangeClosed(1, 12)
                    .mapToObj(month -> {
                        return (int) allPatients.stream()
                                .filter(p -> p.getUser().getCreatedAt() != null &&
                                        p.getUser().getCreatedAt().toLocalDate().getYear() == now.getYear() &&
                                        p.getUser().getCreatedAt().toLocalDate().getMonthValue() == month)
                                .count();
                    })
                    .collect(Collectors.toList());
                result.put("data", monthlyStats);
                break;

            case "5years":
                // Yearly patient count for last 5 years
                int currentYear = now.getYear();
                List<Integer> yearlyStats = IntStream.rangeClosed(currentYear - 4, currentYear)
                    .mapToObj(year -> {
                        return (int) allPatients.stream()
                                .filter(p -> p.getUser().getCreatedAt() != null &&
                                        p.getUser().getCreatedAt().toLocalDate().getYear() == year)
                                .count();
                    })
                    .collect(Collectors.toList());
                result.put("data", yearlyStats);
                break;
        }

        return result;
    }

    /**
     * Get appointment statistics based on the specified time period
     */
    private Map<String, Object> getAppointmentStatistics(String period) {
        List<Appointment> allAppointments = receptionistService.getAllAppointments();
        Map<String, Object> result = new HashMap<>();
        LocalDateTime now = LocalDateTime.now();

        // Generate data points based on period
        switch (period) {
            case "day":
                // Last 30 days appointment count by status
                List<Integer> scheduledDaily = generateDailyStats(allAppointments, "SCHEDULED", 30);
                List<Integer> completedDaily = generateDailyStats(allAppointments, "COMPLETED", 30);
                List<Integer> cancelledDaily = generateDailyStats(allAppointments, "CANCELLED", 30);

                result.put("scheduled", scheduledDaily);
                result.put("completed", completedDaily);
                result.put("cancelled", cancelledDaily);
                break;

            case "month":
                // Monthly appointment count by status for current year
                List<Integer> scheduledMonthly = generateMonthlyStats(allAppointments, "SCHEDULED");
                List<Integer> completedMonthly = generateMonthlyStats(allAppointments, "COMPLETED");
                List<Integer> cancelledMonthly = generateMonthlyStats(allAppointments, "CANCELLED");

                result.put("scheduled", scheduledMonthly);
                result.put("completed", completedMonthly);
                result.put("cancelled", cancelledMonthly);
                break;

            case "5years":
                // Yearly appointment count by status for last 5 years
                int currentYear = now.getYear();
                List<Integer> scheduledYearly = generateYearlyStats(allAppointments, "SCHEDULED", currentYear);
                List<Integer> completedYearly = generateYearlyStats(allAppointments, "COMPLETED", currentYear);
                List<Integer> cancelledYearly = generateYearlyStats(allAppointments, "CANCELLED", currentYear);

                result.put("scheduled", scheduledYearly);
                result.put("completed", completedYearly);
                result.put("cancelled", cancelledYearly);
                break;
        }

        return result;
    }

    /**
     * Get summary data for dashboard cards
     */
    private Map<String, Object> getDashboardSummary() {
        Map<String, Object> summary = new HashMap<>();

        // Count new patients in the last 30 days
        LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);
        long newPatients = receptionistService.getAllPatients().stream()
                .filter(p -> p.getUser().getCreatedAt() != null &&
                        p.getUser().getCreatedAt().toLocalDate().isAfter(thirtyDaysAgo))
                .count();

        // Count all doctors
        long doctorsCount = doctorService.getAllDoctors().size();

        // Count total appointments
        long totalAppointments = receptionistService.getAllAppointments().size();

        // Count active staff (doctors + receptionists)
        long activeStaff = doctorsCount +
                userRepository.findAll().stream()
                .filter(u -> "RECEPTIONIST".equals(u.getRole()) && "ACTIVE".equals(u.getStatus()))
                .count();

        summary.put("newPatients", newPatients);
        summary.put("doctors", doctorsCount);
        summary.put("totalAppointments", totalAppointments);
        summary.put("activeStaff", activeStaff);

        return summary;
    }

    /**
     * Helper method to generate daily statistics for appointments by status
     */
    private List<Integer> generateDailyStats(List<Appointment> appointments, String status, int days) {
        LocalDate now = LocalDate.now();
        return IntStream.rangeClosed(1, days)
                .mapToObj(day -> {
                    LocalDate targetDate = now.minusDays(days - day);
                    return (int) appointments.stream()
                            .filter(a -> status.equalsIgnoreCase(a.getStatus()) &&
                                    a.getDateTime() != null &&
                                    a.getDateTime().toLocalDate().equals(targetDate))
                            .count();
                })
                .collect(Collectors.toList());
    }

    /**
     * Helper method to generate monthly statistics for appointments by status
     */
    private List<Integer> generateMonthlyStats(List<Appointment> appointments, String status) {
        int currentYear = LocalDate.now().getYear();
        return IntStream.rangeClosed(1, 12)
                .mapToObj(month -> {
                    return (int) appointments.stream()
                            .filter(a -> status.equalsIgnoreCase(a.getStatus()) &&
                                    a.getDateTime() != null &&
                                    a.getDateTime().getYear() == currentYear &&
                                    a.getDateTime().getMonthValue() == month)
                            .count();
                })
                .collect(Collectors.toList());
    }

    /**
     * Helper method to generate yearly statistics for appointments by status
     */
    private List<Integer> generateYearlyStats(List<Appointment> appointments, String status, int currentYear) {
        return IntStream.rangeClosed(currentYear - 4, currentYear)
                .mapToObj(year -> {
                    return (int) appointments.stream()
                            .filter(a -> status.equalsIgnoreCase(a.getStatus()) &&
                                    a.getDateTime() != null &&
                                    a.getDateTime().getYear() == year)
                            .count();
                })
                .collect(Collectors.toList());
    }

    // Helper method to map appointments to response format
    private List<Map<String, Object>> mapAppointmentsToResponse(List<Appointment> appointments) {
        return appointments.stream()
                .map(appointment -> {
                    Map<String, Object> appointmentMap = new HashMap<>();
                    appointmentMap.put("stt", appointment.getAppointmentId());
                    appointmentMap.put("name", appointment.getPatient().getUser().getFullName());
                    appointmentMap.put("phone", appointment.getPatient().getUser().getPhoneNumber());
                    appointmentMap.put("time", appointment.getDateTime().toString());
                    appointmentMap.put("doctor", appointment.getDoctor().getUser().getFullName());
                    appointmentMap.put("status", appointment.getStatus());
                    return appointmentMap;
                })
                .collect(Collectors.toList());
    }
}
