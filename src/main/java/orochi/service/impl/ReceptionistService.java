package orochi.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import orochi.model.Appointment;
import orochi.model.Patient;
import orochi.model.PatientContact;
import orochi.model.Users;
import orochi.repository.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class ReceptionistService {

    private final UserRepository userRepository;
    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final ReceptionistRepository receptionistRepository;
    private final PatientContactRepository patientContactRepository;
    private final DoctorRepository DoctorRepository;

    public ReceptionistService(
            UserRepository userRepository,
            AppointmentRepository appointmentRepository,
            PatientRepository patientRepository,
            ReceptionistRepository receptionistRepository,
            PatientContactRepository patientContactRepository,
            DoctorRepository doctorRepository) {
        this.userRepository = userRepository;
        this.appointmentRepository = appointmentRepository;
        this.patientRepository = patientRepository;
        this.receptionistRepository = receptionistRepository;
        this.patientContactRepository = patientContactRepository;
        this.DoctorRepository = doctorRepository;
    }

    // Fetch all appointments for scheduling purposes
    public List<Appointment> getAllAppointments() {
        return appointmentRepository.findAll();
    }

    // Register a new patient
    public Users registerPatient(Users user) {
        Users patientRole = receptionistRepository.findByEmail("PATIENT")
                .orElseThrow(() -> new RuntimeException("Patient role not found"));
        user.setRoleId(patientRole.getRoleId());
        user.setGuest(false);
        user.setStatus("Active");
        return userRepository.save(user);
    }

    // Register a new patient with full information
    // Creates both Users and Patient records with PatientContact
    @Transactional
    public Users registerNewPatient(Map<String, Object> registrationData) {
        try {
            // Extract data from map
            String fullName = (String) registrationData.get("fullName");
            String email = (String) registrationData.get("email");
            String phoneNumber = (String) registrationData.get("phoneNumber");
            String passwordHash = (String) registrationData.get("passwordHash");
            String dateOfBirthStr = (String) registrationData.get("dateOfBirth");
            String gender = (String) registrationData.get("gender");
            String streetAddress = (String) registrationData.get("streetAddress");
            String city = (String) registrationData.get("city");
            String country = (String) registrationData.get("country");
            String addressType = (String) registrationData.get("addressType");
            String description = (String) registrationData.get("description");

            // Parse date of birth
            java.time.LocalDate dateOfBirth;
            try {
                dateOfBirth = java.time.LocalDate.parse(dateOfBirthStr);
            } catch (Exception e) {
                throw new RuntimeException("Invalid date format. Please use YYYY-MM-DD format.");
            }

            // Create Users entity
            Users newUser = new Users();
            newUser.setFullName(fullName);
            newUser.setEmail(email);
            newUser.setPhoneNumber(phoneNumber);
            newUser.setPasswordHash(passwordHash); // In production, this should be encrypted
            newUser.setRoleId(4); // Patient role ID = 4
            newUser.setGuest(false);
            newUser.setStatus("Active");
            newUser.setCreatedAt(java.time.LocalDateTime.now());

            // Save Users first to get UserID
            Users savedUser = userRepository.save(newUser);

            // Create Patient entity (without address field)
            Patient newPatient = new Patient();
            newPatient.setUserId(savedUser.getUserId());
            newPatient.setDateOfBirth(dateOfBirth);
            newPatient.setGender(gender);
            newPatient.setDescription(description);

            // Save Patient
            Patient savedPatient = patientRepository.save(newPatient);

            // Create PatientContact entity for address information
            PatientContact patientContact = new PatientContact();
            patientContact.setPatientId(savedPatient.getPatientId());
            patientContact.setAddressType(addressType != null ? addressType : "Home");
            patientContact.setStreetAddress(streetAddress);
            patientContact.setCity(city);
            patientContact.setCountry(country);
            // Set default values for missing fields if needed
            patientContact.setState(""); // Default empty state
            patientContact.setPostalCode(""); // Default empty postal code

            // Save PatientContact
            patientContactRepository.save(patientContact);

            // Set the patient relationship in user for return
            savedUser.setPatient(savedPatient);

            return savedUser;

        } catch (Exception e) {
            // Log the error
            System.err.println("Error registering new patient: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to register patient: " + e.getMessage());
        }
    }

    // Fetch all patients for registration management
    public List<Patient> getAllPatients() {
        return patientRepository.findAll();
    }

    public List<PatientContact> getAllPatientContacts() {
        return patientContactRepository.findAll();
    }

    public List<PatientContact> getPatientContactsByPatientId(Integer patientId) {
        return patientContactRepository.findByPatientId(patientId);
    }

    // Check if the user is a Receptionist
    public boolean isReceptionist(Users user) {
        return user.getRole().getRoleName().equals("RECEPTIONIST");
    }

    // Update appointment status
    public boolean updateAppointmentStatus(Integer appointmentId, String status) {
        Optional<Appointment> appointmentOptional = appointmentRepository.findById(appointmentId);
        if (appointmentOptional.isPresent()) {
            Appointment appointment = appointmentOptional.get();
            if (!isValidAppointmentStatus(status)) {
                throw new IllegalArgumentException("Invalid appointment status: " + status);
            }
            appointment.setStatus(status);
            appointmentRepository.save(appointment);
            return true;
        }
        return false;
    }

    private boolean isValidAppointmentStatus(String status) {
        return status != null && (status.equals("Scheduled") || status.equals("Completed") ||
                status.equals("Cancel") || status.equals("Pending"));
    }

    // Fetch all Receptionists with pagination and filtering
    public Page<Users> getAllReceptionists(Pageable pageable) {
        return receptionistRepository.findAllReceptionistsFiltered(null, null, pageable);
    }

    public Page<Users> getAllReceptionists(String search, String statusFilter, Pageable pageable) {
        return receptionistRepository.findAllReceptionistsFiltered(search, statusFilter, pageable);
    }

//    public int newPatients(){
//        return receptionistRepository.newPatients();
//    }

    public int ourDoctors() {
        // Get the number of doctors from the database, similar to activeStaff() and newPatients()
        return receptionistRepository.ourDoctors();
    }

    public int totalAppointment() {
        return receptionistRepository.totalAppointments();
    }

    public int activeStaff() {
        return receptionistRepository.activeStaff();
    }

    public int newPatients() {
        return receptionistRepository.newPatients();
    }

    public Map<String, Object> getPatientStatusChartData(String period) {

        // Only support "day" period - always return daily statistics for current month
        List<Object[]> results = receptionistRepository.getPatientStatsByDay();

        List<String> labels = new ArrayList<>();
        List<Integer> data = new ArrayList<>();

        for (Object[] row : results) {
            labels.add((String) row[0]);  // Day number (1-31)
            data.add(((Number) row[1]).intValue()); // Patient count
        }

        // Create nested structure that matches frontend expectations
        Map<String, Object> patientStats = new HashMap<>();
        patientStats.put("labels", labels);
        patientStats.put("data", data);

        Map<String, Object> response = new HashMap<>();
        response.put("patientStats", patientStats);

        // Also add the flat structure for backward compatibility
        response.put("labels", labels);
        response.put("data", data);

        return response;
    }

    public List<Map<String, Object>> getAppointmentTableData() {
        // G?i repository ?? l?y d? li?u
        return receptionistRepository.fetchAppointmentTableData();
    }

    public Appointment bookAppointment(Appointment appointment) {
        appointment.setStatus("Scheduled"); // Trạng thái mặc định
        return appointmentRepository.save(appointment);
    }

    public List<Map<String, Object>> getAllDoctorsWithDetails() {
        List<orochi.model.Doctor> doctors = DoctorRepository.findAll();

        return doctors.stream()
                .distinct() // Additional protection against duplicates
                .map(doctor -> {
                    Map<String, Object> doctorMap = new HashMap<>();
                    doctorMap.put("id", doctor.getDoctorId());
                    doctorMap.put("name", doctor.getUser() != null ? doctor.getUser().getFullName() : "Unknown");
                    doctorMap.put("email", doctor.getUser() != null ? doctor.getUser().getEmail() : "");
                    doctorMap.put("phone", doctor.getUser() != null ? doctor.getUser().getPhoneNumber() : "");

                    // Get specialty information - show primary specialty or all specialties
                    String specialty = "General Practice"; // Default value
                    if (doctor.getSpecializations() != null && !doctor.getSpecializations().isEmpty()) {
                        // For better readability, show only the first specialty in lists
                        // but keep all specialties for detailed views
                        specialty = doctor.getSpecializations().stream()
                            .map(spec -> spec.getSpecName())
                            .collect(Collectors.joining(", "));
                    }
                    doctorMap.put("specialty", specialty);

                    return doctorMap;
                }).collect(Collectors.toList());
    }

    public Map<String, Object> getDoctorDetails(Integer doctorId) {
        Optional<orochi.model.Doctor> doctorOptional = DoctorRepository.findById(doctorId);

        if (doctorOptional.isPresent()) {
            orochi.model.Doctor doctor = doctorOptional.get();
            Map<String, Object> doctorDetails = new HashMap<>();

            doctorDetails.put("id", doctor.getDoctorId());
            doctorDetails.put("name", doctor.getUser() != null ? doctor.getUser().getFullName() : "Unknown");
            doctorDetails.put("email", doctor.getUser() != null ? doctor.getUser().getEmail() : "");
            doctorDetails.put("phone", doctor.getUser() != null ? doctor.getUser().getPhoneNumber() : "");
            doctorDetails.put("bio", doctor.getBioDescription());
            //doctorDetails.put("avatar", doctor.getUser() != null ? doctor.getUser().getAvatar() : null);

            // Get education details
            String degree = "Not specified";
            String institution = "Not specified";
            String description = "Not specified";

            if (doctor.getEducations() != null && !doctor.getEducations().isEmpty()) {
                // Get the first education record (you can modify this logic as needed)
                var education = doctor.getEducations().get(0);
                degree = education.getDegree() != null ? education.getDegree() : "Not specified";
                institution = education.getInstitution() != null ? education.getInstitution() : "Not specified";
                description = education.getDescription() != null ? education.getDescription() : "Not specified";
            }

            doctorDetails.put("degree", degree);
            doctorDetails.put("institution", institution);
            doctorDetails.put("description", description);

            // Get specialty information
            String specialty = "General Practice";
            if (doctor.getSpecializations() != null && !doctor.getSpecializations().isEmpty()) {
                specialty = doctor.getSpecializations().stream()
                    .map(spec -> spec.getSpecName())
                    .collect(Collectors.joining(", "));
            }
            doctorDetails.put("specialty", specialty);

            return doctorDetails;
        }

        return null;
    }

    // Validate if doctor has the required specialty
    public boolean validateDoctorSpecialty(Integer doctorId, Integer specialtyId) {
        if (doctorId == null || specialtyId == null) {
            return false;
        }

        Optional<orochi.model.Doctor> doctorOptional = DoctorRepository.findById(doctorId);
        if (doctorOptional.isEmpty()) {
            return false;
        }

        orochi.model.Doctor doctor = doctorOptional.get();

        // Check if doctor has the required specialty
        if (doctor.getSpecializations() != null && !doctor.getSpecializations().isEmpty()) {
            return doctor.getSpecializations().stream()
                .anyMatch(spec -> spec.getSpecId().equals(specialtyId));
        }

        return false;
    }

    // Get doctors by specialty ID - now returns only one doctor per specialty
    public List<Map<String, Object>> getDoctorsBySpecialty(Integer specialtyId) {
        if (specialtyId == null) {
            return getAllDoctorsWithDetails();
        }

        // Use the repository method with DISTINCT to get doctors by specialty
        List<orochi.model.Doctor> doctors = DoctorRepository.findBySpecializationId(specialtyId);

        return doctors.stream()
            .distinct() // Additional protection against duplicates
            .limit(1) // Only take the first doctor for each specialty
            .map(doctor -> {
                Map<String, Object> doctorMap = new HashMap<>();
                doctorMap.put("id", doctor.getDoctorId());
                doctorMap.put("name", doctor.getUser() != null ? doctor.getUser().getFullName() : "Unknown");
                doctorMap.put("email", doctor.getUser() != null ? doctor.getUser().getEmail() : "");
                doctorMap.put("phone", doctor.getUser() != null ? doctor.getUser().getPhoneNumber() : "");

                // Get specialty information
                String specialty = "General Practice";
                if (doctor.getSpecializations() != null && !doctor.getSpecializations().isEmpty()) {
                    specialty = doctor.getSpecializations().stream()
                        .map(spec -> spec.getSpecName())
                        .collect(Collectors.joining(", "));
                }
                doctorMap.put("specialty", specialty);

                return doctorMap;
            })
            .collect(Collectors.toList());
    }

    // New method to get the single doctor assigned to a specialty
    public Map<String, Object> getDoctorBySpecialty(Integer specialtyId) {
        if (specialtyId == null) {
            return null;
        }

        List<orochi.model.Doctor> doctors = DoctorRepository.findBySpecializationId(specialtyId);

        if (doctors.isEmpty()) {
            return null;
        }

        // Get the first (and should be only) doctor for this specialty
        orochi.model.Doctor doctor = doctors.get(0);

        Map<String, Object> doctorMap = new HashMap<>();
        doctorMap.put("id", doctor.getDoctorId());
        doctorMap.put("name", doctor.getUser() != null ? doctor.getUser().getFullName() : "Unknown");
        doctorMap.put("email", doctor.getUser() != null ? doctor.getUser().getEmail() : "");
        doctorMap.put("phone", doctor.getUser() != null ? doctor.getUser().getPhoneNumber() : "");

        // Get specialty information
        String specialty = "General Practice";
        if (doctor.getSpecializations() != null && !doctor.getSpecializations().isEmpty()) {
            specialty = doctor.getSpecializations().stream()
                .map(spec -> spec.getSpecName())
                .collect(Collectors.joining(", "));
        }
        doctorMap.put("specialty", specialty);

        return doctorMap;
    }
}
