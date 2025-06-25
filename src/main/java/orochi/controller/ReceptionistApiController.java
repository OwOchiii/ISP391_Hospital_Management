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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
