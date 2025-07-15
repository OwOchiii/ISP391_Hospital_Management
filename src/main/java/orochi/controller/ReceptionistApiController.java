package orochi.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import orochi.config.CustomUserDetails;
import orochi.model.Appointment;
import orochi.model.Patient;
import orochi.model.PatientContact;
import orochi.model.Users;
import orochi.repository.UserRepository;
import orochi.service.impl.DoctorServiceImpl;
import orochi.service.impl.EmailServiceImpl;
import orochi.service.impl.ReceptionistService;
import orochi.service.impl.RoomServiceImpl;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class ReceptionistApiController {

    private final ReceptionistService receptionistService;
    private final DoctorServiceImpl doctorService;
    private final UserRepository userRepository;
    private final RoomServiceImpl roomService;
    private final EmailServiceImpl emailService;

    public ReceptionistApiController(ReceptionistService receptionistService,
                                     DoctorServiceImpl doctorService,
                                     UserRepository userRepository,
                                     RoomServiceImpl roomService, EmailServiceImpl emailService) {
        this.receptionistService = receptionistService;
        this.doctorService = doctorService;
        this.userRepository = userRepository;
        this.roomService = roomService;
        this.emailService = emailService;
    }

    @PostMapping("/appointments/confirm")
    public ResponseEntity<?> confirmAppointment(
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
                return ResponseEntity.status(403).body("Forbidden: Only receptionists can confirm appointments");
            }

            // Get appointment ID from request
            Integer appointmentId = (Integer) request.get("stt");

            // Validate input
            if (appointmentId == null) {
                return ResponseEntity.badRequest().body("Invalid request: appointment ID is required");
            }

            // Update status to "Scheduled"
            boolean updated = receptionistService.updateAppointmentStatus(appointmentId, "Scheduled");

            if (updated) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Appointment confirmed successfully"
                ));
            } else {
                return ResponseEntity.status(404).body("Appointment not found with ID: " + appointmentId);
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
    @GetMapping("/doctors")
    public ResponseEntity<?> getAllDoctors() {
        try {
            List<Map<String, Object>> doctors = doctorService.getAllDoctors().stream()
                    .map(doctor -> {
                        Map<String, Object> doctorMap = new HashMap<>();
                        doctorMap.put("id", doctor.getDoctorId());
                        doctorMap.put("name", doctor.getUser().getFullName());
                        doctorMap.put("imageUrl", doctor.getImageUrl());
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
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(defaultValue = "") String search) {
        try {
            // Default page to 1 if null or invalid
            int pageNumber = (page == null || page < 1) ? 1 : page;
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
            int start = (pageNumber - 1) * itemsPerPage;
            int end = Math.min(start + itemsPerPage, total);

            List<Map<String, Object>> paginatedPatients = allPatients.subList(start, end).stream()
                    .map(patient -> {
                        List<PatientContact> contactsById = receptionistService.getPatientContactsByPatientId(patient.getPatientId());
                        Map<String, Object> patientMap = new HashMap<>();
                        patientMap.put("id", patient.getPatientId());
                        patientMap.put("name", patient.getUser().getFullName());
                        patientMap.put("phone", patient.getUser().getPhoneNumber());
                        patientMap.put("email", patient.getUser().getEmail());
                        patientMap.put("dateOfBirth", patient.getDateOfBirth());
                        patientMap.put("gender", patient.getGender());
                        // Find contact info for this patient
                        PatientContact contact = contactsById.stream()
                            .filter(c -> c.getPatientId().equals(patient.getPatientId()))
                            .findFirst().orElse(null);
                        if (contact != null) {
                            patientMap.put("country", contact.getCountry());
                            patientMap.put("province", contact.getCity());
                            patientMap.put("district", contact.getState());
                            patientMap.put("streetAddress", contact.getStreetAddress());
                            patientMap.put("postalCode", contact.getPostalCode());
                            patientMap.put("addressType", contact.getAddressType());
                        }
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

    @GetMapping("/patients/detailed")
    public ResponseEntity<?> getPatientsDetailed(
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(defaultValue = "") String search) {
        try {
            // Lấy tất cả patients có RoleID = 4 (Patient role) - KHÔNG PHÂN TRANG
            List<Patient> allPatients = receptionistService.getAllPatients().stream()
                    .filter(patient -> patient.getUser() != null && patient.getUser().getRoleId() == 4)
                    .collect(Collectors.toList());

            // Filter by search term if provided
            if (!search.isEmpty()) {
                String searchLower = search.toLowerCase();
                allPatients = allPatients.stream()
                        .filter(p -> p.getUser().getFullName().toLowerCase().contains(searchLower) ||
                                (p.getUser().getPhoneNumber() != null && p.getUser().getPhoneNumber().toLowerCase().contains(searchLower)) ||
                                (p.getUser().getEmail() != null && p.getUser().getEmail().toLowerCase().contains(searchLower)) ||
                                p.getPatientId().toString().contains(searchLower))
                        .collect(Collectors.toList());
            }

            // KHÔNG PHÂN TRANG - Trả về toàn bộ dữ liệu
            int total = allPatients.size();

            // Map toàn bộ patients (không slice)
            List<Map<String, Object>> allPatientsData = allPatients.stream()
                    .map(patient -> {
                        Map<String, Object> patientMap = new HashMap<>();

                        // Thông tin cơ bản từ bảng Patient
                        patientMap.put("PatientID", patient.getPatientId());
                        patientMap.put("UserID", patient.getUserId());
                        patientMap.put("DateOfBirth", patient.getDateOfBirth());
                        patientMap.put("Gender", patient.getGender());
                        patientMap.put("Description", patient.getDescription());

                        // Thông tin từ bảng Users (RoleID = 4)
                        if (patient.getUser() != null) {
                            patientMap.put("FullName", patient.getUser().getFullName());
                            patientMap.put("Email", patient.getUser().getEmail());
                            patientMap.put("PhoneNumber", patient.getUser().getPhoneNumber());
                            patientMap.put("RoleID", patient.getUser().getRoleId());
                        }

                        // Calculate age từ dateOfBirth
                        if (patient.getDateOfBirth() != null) {
                            LocalDate today = LocalDate.now();
                            LocalDate birthDate = patient.getDateOfBirth();
                            int age = today.getYear() - birthDate.getYear();
                            if (today.getDayOfYear() < birthDate.getDayOfYear()) {
                                age--;
                            }
                            patientMap.put("Age", age);
                        } else {
                            patientMap.put("Age", null);
                        }

                        // Lấy thông tin appointment mới nhất từ bảng Appointment
                        List<Appointment> patientAppointments = receptionistService.getAllAppointments().stream()
                                .filter(apt -> apt.getPatientId().equals(patient.getPatientId()))
                                .sorted((a1, a2) -> a2.getDateTime().compareTo(a1.getDateTime()))
                                .collect(Collectors.toList());

                        if (!patientAppointments.isEmpty()) {
                            Appointment latestAppointment = patientAppointments.get(0);
                            patientMap.put("AppointmentDateTime", latestAppointment.getDateTime().toString());
                            patientMap.put("AppointmentStatus", latestAppointment.getStatus());
                            patientMap.put("AppointmentID", latestAppointment.getAppointmentId());
                            patientMap.put("DoctorID", latestAppointment.getDoctorId());
                            patientMap.put("RoomID", latestAppointment.getRoomId());
                            patientMap.put("AppointmentDescription", latestAppointment.getDescription());
                            patientMap.put("AppointmentEmail", latestAppointment.getEmail());
                            patientMap.put("AppointmentPhoneNumber", latestAppointment.getPhoneNumber());
                        } else {
                            patientMap.put("AppointmentDateTime", "No Appointment");
                            patientMap.put("AppointmentStatus", "No Appointment");
                            patientMap.put("AppointmentID", null);
                            patientMap.put("DoctorID", null);
                            patientMap.put("RoomID", null);
                            patientMap.put("AppointmentDescription", null);
                            patientMap.put("AppointmentEmail", null);
                            patientMap.put("AppointmentPhoneNumber", null);
                        }

                        // Get contact info từ PatientContact
                        List<PatientContact> contactsById = receptionistService.getPatientContactsByPatientId(patient.getPatientId());
                        if (!contactsById.isEmpty()) {
                            PatientContact contact = contactsById.get(0);
                            patientMap.put("Country", contact.getCountry());
                            patientMap.put("City", contact.getCity());
                            patientMap.put("State", contact.getState());
                            patientMap.put("StreetAddress", contact.getStreetAddress());
                            patientMap.put("PostalCode", contact.getPostalCode());
                            patientMap.put("AddressType", contact.getAddressType());
                        }

                        return patientMap;
                    })
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("patients", allPatientsData);
            response.put("total", total);

            System.out.println("API Response: Found " + total + " patients in database");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Error in getPatientsDetailed: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Error fetching patients: " + e.getMessage());
        }
    }

    @GetMapping("/patients/{patientId}/details")
    public ResponseEntity<?> getPatientDetails(@PathVariable Integer patientId) {
        try {
            // Lấy patient theo PatientID và kiểm tra RoleID = 4
            Optional<Patient> patientOptional = receptionistService.getAllPatients().stream()
                    .filter(p -> p.getPatientId().equals(patientId) &&
                               p.getUser() != null &&
                               p.getUser().getRoleId() == 4)
                    .findFirst();

            if (patientOptional.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Patient patient = patientOptional.get();
            Map<String, Object> patientDetails = new HashMap<>();

            // Thông tin cơ bản từ bảng Patient
            patientDetails.put("PatientID", patient.getPatientId());
            patientDetails.put("UserID", patient.getUserId());
            patientDetails.put("DateOfBirth", patient.getDateOfBirth());
            patientDetails.put("Gender", patient.getGender());
            patientDetails.put("Description", patient.getDescription());

            // Thông tin từ bảng Users (RoleID = 4)
            if (patient.getUser() != null) {
                patientDetails.put("FullName", patient.getUser().getFullName());
                patientDetails.put("Email", patient.getUser().getEmail());
                patientDetails.put("PhoneNumber", patient.getUser().getPhoneNumber());
                patientDetails.put("RoleID", patient.getUser().getRoleId());
            }

            // Calculate age từ dateOfBirth
            if (patient.getDateOfBirth() != null) {
                LocalDate today = LocalDate.now();
                LocalDate birthDate = patient.getDateOfBirth();
                int age = today.getYear() - birthDate.getYear();
                if (today.getDayOfYear() < birthDate.getDayOfYear()) {
                    age--;
                }
                patientDetails.put("Age", age);
            }

            // Get contact information từ PatientContact
            List<PatientContact> contacts = receptionistService.getPatientContactsByPatientId(patientId);
            if (!contacts.isEmpty()) {
                PatientContact contact = contacts.get(0);
                patientDetails.put("Country", contact.getCountry());
                patientDetails.put("City", contact.getCity());
                patientDetails.put("State", contact.getState());
                patientDetails.put("StreetAddress", contact.getStreetAddress());
                patientDetails.put("PostalCode", contact.getPostalCode());
                patientDetails.put("AddressType", contact.getAddressType());
            }

            // Get all appointments cho patient này từ bảng Appointment
            List<Appointment> appointments = receptionistService.getAllAppointments().stream()
                    .filter(apt -> apt.getPatientId().equals(patientId))
                    .sorted((a1, a2) -> a2.getDateTime().compareTo(a1.getDateTime()))
                    .collect(Collectors.toList());

            List<Map<String, Object>> appointmentDetails = appointments.stream()
                    .map(apt -> {
                        Map<String, Object> aptMap = new HashMap<>();
                        // Thông tin từ bảng Appointment
                        aptMap.put("AppointmentID", apt.getAppointmentId());
                        aptMap.put("DoctorID", apt.getDoctorId());
                        aptMap.put("PatientID", apt.getPatientId());
                        aptMap.put("RoomID", apt.getRoomId());
                        aptMap.put("DateTime", apt.getDateTime().toString());
                        aptMap.put("Status", apt.getStatus());
                        aptMap.put("Description", apt.getDescription());
                        aptMap.put("Email", apt.getEmail());
                        aptMap.put("PhoneNumber", apt.getPhoneNumber());

                        // Thông tin doctor
                        if (apt.getDoctor() != null && apt.getDoctor().getUser() != null) {
                            aptMap.put("DoctorName", apt.getDoctor().getUser().getFullName());
                            aptMap.put("DoctorSpecialty", apt.getDoctor().getSpecializations().isEmpty() ?
                                    "General Practice" :
                                    apt.getDoctor().getSpecializations().get(0).getSpecName());
                        }
                        return aptMap;
                    })
                    .collect(Collectors.toList());

            patientDetails.put("Appointments", appointmentDetails);

            return ResponseEntity.ok(patientDetails);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error fetching patient details: " + e.getMessage());
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

            // Get current date for filtering
            LocalDate today = LocalDate.now();

            // Filter by status and today's date
            List<Appointment> filteredAppointments = allAppointments.stream()
                    .filter(a -> a.getStatus().equalsIgnoreCase(status))
                    .filter(a -> a.getDateTime().toLocalDate().equals(today)) // Only today's appointments
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
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @PutMapping("/patients/{patientId}/details")
    public ResponseEntity<?> updatePatientDetails(
            @PathVariable Integer patientId,
            @RequestBody Map<String, Object> updateData,
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
                return ResponseEntity.status(403).body("Forbidden: Only receptionists can update patient details");
            }

            // Find the patient
            Optional<Patient> patientOptional = receptionistService.getAllPatients().stream()
                    .filter(p -> p.getPatientId().equals(patientId) &&
                               p.getUser() != null &&
                               p.getUser().getRoleId() == 4)
                    .findFirst();

            if (patientOptional.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Patient patient = patientOptional.get();

            // Update patient information
            if (updateData.containsKey("DateOfBirth")) {
                String dateOfBirthStr = (String) updateData.get("DateOfBirth");
                try {
                    java.time.LocalDate dateOfBirth = java.time.LocalDate.parse(dateOfBirthStr);
                    patient.setDateOfBirth(dateOfBirth);
                } catch (Exception e) {
                    return ResponseEntity.badRequest().body("Invalid date format for DateOfBirth");
                }
            }

            if (updateData.containsKey("Gender")) {
                String gender = (String) updateData.get("Gender");
                if (gender != null && !gender.trim().isEmpty()) {
                    patient.setGender(gender.trim());
                }
            }

            if (updateData.containsKey("Description")) {
                String description = (String) updateData.get("Description");
                patient.setDescription(description != null ? description.trim() : "");
            }

            // Save patient changes
            Patient updatedPatient = receptionistService.updatePatient(patient);

            // Update contact information
            List<PatientContact> contacts = receptionistService.getPatientContactsByPatientId(patientId);
            PatientContact contact;

            if (contacts.isEmpty()) {
                // Create new contact if none exists
                contact = new PatientContact();
                contact.setPatientId(patientId);
            } else {
                // Update existing contact
                contact = contacts.get(0);
            }

            // Update contact fields
            if (updateData.containsKey("AddressType")) {
                String addressType = (String) updateData.get("AddressType");
                contact.setAddressType(addressType != null ? addressType.trim() : "");
            }

            if (updateData.containsKey("StreetAddress")) {
                String streetAddress = (String) updateData.get("StreetAddress");
                contact.setStreetAddress(streetAddress != null ? streetAddress.trim() : "");
            }

            if (updateData.containsKey("City")) {
                String city = (String) updateData.get("City");
                contact.setCity(city != null ? city.trim() : "");
            }

            if (updateData.containsKey("State")) {
                String state = (String) updateData.get("State");
                contact.setState(state != null ? state.trim() : "");
            }

            if (updateData.containsKey("PostalCode")) {
                String postalCode = (String) updateData.get("PostalCode");
                contact.setPostalCode(postalCode != null ? postalCode.trim() : "");
            }

            if (updateData.containsKey("Country")) {
                String country = (String) updateData.get("Country");
                contact.setCountry(country != null ? country.trim() : "");
            }

            // Save contact changes
            PatientContact updatedContact = receptionistService.savePatientContact(contact);

            // Send email notification about profile update
            try {
                if (updatedPatient.getUser() != null && updatedPatient.getUser().getEmail() != null) {
                    String patientEmail = updatedPatient.getUser().getEmail();
                    emailService.sendProfileUpdateEmail(patientEmail, updatedPatient);
                }
            } catch (Exception e) {
                // Log the error but don't fail the update operation
                System.err.println("Failed to send profile update email: " + e.getMessage());
            }

            // Prepare response data
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("PatientID", updatedPatient.getPatientId());
            responseData.put("DateOfBirth", updatedPatient.getDateOfBirth());
            responseData.put("Gender", updatedPatient.getGender());
            responseData.put("Description", updatedPatient.getDescription());
            responseData.put("AddressType", updatedContact.getAddressType());
            responseData.put("StreetAddress", updatedContact.getStreetAddress());
            responseData.put("City", updatedContact.getCity());
            responseData.put("State", updatedContact.getState());
            responseData.put("PostalCode", updatedContact.getPostalCode());
            responseData.put("Country", updatedContact.getCountry());

            return ResponseEntity.ok(responseData);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error updating patient details: " + e.getMessage());
        }
    }

    @GetMapping("/rooms/by-specialty-doctor")
    public ResponseEntity<?> getRoomsBySpecialtyAndDoctor(
            @RequestParam Integer specialtyId,
            @RequestParam Integer doctorId) {
        try {
            List<Map<String, Object>> rooms = receptionistService.getRoomsBySpecialtyAndDoctor(specialtyId, doctorId);
            return ResponseEntity.ok(rooms);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error fetching rooms: " + e.getMessage());
        }
    }

    /**
     * API endpoint to get rooms by doctor and specialty
     * Implements the SQL logic: SELECT DISTINCT s.SpecName AS SpecializationName, u.FullName AS DoctorName,
     * r.RoomID, r.RoomNumber, d.DeptName AS DepartmentName
     * FROM Specialization s INNER JOIN DoctorSpecialization ds ON s.SpecID = ds.SpecID
     * INNER JOIN Doctor doc ON ds.DoctorID = doc.DoctorID
     * INNER JOIN Users u ON doc.UserID = u.UserID
     * LEFT JOIN Schedule sch ON doc.DoctorID = sch.DoctorID
     * LEFT JOIN Room r ON sch.RoomID = r.RoomID
     * LEFT JOIN Department d ON r.DepartmentID = d.DepartmentID
     * ORDER BY u.FullName, s.SpecName, r.RoomNumber;
     */
    @GetMapping("/rooms-by-doctor-specialty")
    public ResponseEntity<?> getRoomsByDoctorAndSpecialty(
            @RequestParam Integer specialtyId,
            @RequestParam Integer doctorId) {
        try {
            // Validate input parameters
            if (doctorId == null || specialtyId == null) {
                return ResponseEntity.badRequest().body("Doctor ID and Specialty ID are required");
            }

            // Get rooms using the service method
            List<Map<String, Object>> rooms = roomService.getRoomsByDoctorAndSpecialty(doctorId, specialtyId);

            if (rooms.isEmpty()) {
                return ResponseEntity.ok(List.of()); // Return empty list instead of error
            }

            // Transform the data to match frontend expectations
            List<Map<String, Object>> transformedRooms = rooms.stream()
                    .map(room -> {
                        Map<String, Object> roomData = new HashMap<>();
                        roomData.put("roomId", room.get("RoomID"));
                        roomData.put("roomNumber", room.get("RoomNumber"));
                        roomData.put("departmentName", room.get("DepartmentName"));
                        roomData.put("specializationName", room.get("SpecializationName"));
                        roomData.put("doctorName", room.get("DoctorName"));
                        return roomData;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(transformedRooms);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid request parameters: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error fetching rooms: " + e.getMessage());
        }
    }

    // Add new endpoint to match frontend URL pattern
    @GetMapping("/rooms/doctor/{doctorId}/specialty/{specialtyId}")
    public ResponseEntity<?> getRoomsByDoctorAndSpecialtyPath(
            @PathVariable Integer doctorId,
            @PathVariable Integer specialtyId) {
        try {
            // Validate input parameters
            if (doctorId == null || specialtyId == null) {
                return ResponseEntity.badRequest().body("Doctor ID and Specialty ID are required");
            }

            // Get rooms using the service method
            List<Map<String, Object>> rooms = roomService.getRoomsByDoctorAndSpecialty(doctorId, specialtyId);

            if (rooms.isEmpty()) {
                return ResponseEntity.ok(List.of()); // Return empty list instead of error
            }

            // Transform the data to match frontend expectations
            List<Map<String, Object>> transformedRooms = rooms.stream()
                    .map(room -> {
                        Map<String, Object> roomData = new HashMap<>();
                        roomData.put("roomId", room.get("RoomID"));
                        roomData.put("roomNumber", room.get("RoomNumber"));
                        roomData.put("departmentName", room.get("DepartmentName"));
                        roomData.put("specializationName", room.get("SpecializationName"));
                        roomData.put("doctorName", room.get("DoctorName"));
                        return roomData;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(transformedRooms);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid request parameters: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error fetching rooms: " + e.getMessage());
        }
    }

    @GetMapping("/doctors-by-specialty")
    public ResponseEntity<?> getDoctorsBySpecialty(
            @RequestParam Integer specialtyId) {
        try {
            // Fetch doctors by specialty
            List<Map<String, Object>> doctors = doctorService.getDoctorsBySpecialtyId(specialtyId);

            if (doctors.isEmpty()) {
                return ResponseEntity.ok(doctors); // Return empty list, not 404
            }

            return ResponseEntity.ok(doctors);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error fetching doctors: " + e.getMessage());
        }
    }

    private List<Map<String, Object>> mapAppointmentsToResponse(List<Appointment> appointments) {
        return appointments.stream()
                .map(appointment -> {
                    Map<String, Object> appointmentMap = new HashMap<>();
                    appointmentMap.put("stt", appointment.getAppointmentId());
                    appointmentMap.put("name", appointment.getPatient().getUser().getFullName());
                    appointmentMap.put("dob", appointment.getPatient().getDateOfBirth());
                    appointmentMap.put("phone", appointment.getPatient().getUser().getPhoneNumber());
                    appointmentMap.put("time", appointment.getDateTime().toString());
                    appointmentMap.put("doctor", appointment.getDoctor().getUser().getFullName());
                    appointmentMap.put("status", appointment.getStatus());
                    return appointmentMap;
                })
                .collect(Collectors.toList());
    }


}
