package orochi.service.impl;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
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
import orochi.model.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class ReceptionistService {

    private static final Logger logger = LoggerFactory.getLogger(ReceptionistService.class);

    private final UserRepository userRepository;
    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final ReceptionistRepository receptionistRepository;
    private final ReceptionistEntityRepository receptionistEntityRepository;
    private final PatientContactRepository patientContactRepository;
    private final DoctorRepository DoctorRepository;
    private final ReceiptRepository receiptRepository;
    private final RoomRepository roomRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    public ReceptionistService(
            UserRepository userRepository,
            AppointmentRepository appointmentRepository,
            PatientRepository patientRepository,
            ReceptionistRepository receptionistRepository,
            ReceptionistEntityRepository receptionistEntityRepository,
            PatientContactRepository patientContactRepository,
            DoctorRepository doctorRepository,
            ReceiptRepository receiptRepository,
            RoomRepository roomRepository) {
        this.userRepository = userRepository;
        this.appointmentRepository = appointmentRepository;
        this.patientRepository = patientRepository;
        this.receptionistRepository = receptionistRepository;
        this.receptionistEntityRepository = receptionistEntityRepository;
        this.patientContactRepository = patientContactRepository;
        this.DoctorRepository = doctorRepository;
        this.receiptRepository = receiptRepository;
        this.roomRepository = roomRepository;
    }

    // Fetch all appointments for scheduling purposes
    public List<Appointment> getAllAppointments() {
        return appointmentRepository.findAll();
    }

    // Register a new patient
    @SuppressWarnings("unused") // This method might be used elsewhere
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

            // Create Patient entity
            Patient newPatient = new Patient();
            newPatient.setUserId(savedUser.getUserId());
            newPatient.setDateOfBirth(dateOfBirth);
            newPatient.setGender(gender);
            newPatient.setDescription(description);

            // Save Patient
            Patient savedPatient = patientRepository.save(newPatient);

            // QUAN TRỌNG: Thiết lập quan hệ bidirectional để email template có thể truy cập dữ liệu
            savedPatient.setUser(savedUser); // Set user relationship in patient
            savedUser.setPatient(savedPatient); // Set patient relationship in user

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

            // Log để debug
            logger.info("=== Patient Registration Debug ===");
            logger.info("Created User - ID: {}, FullName: {}, Email: {}, Phone: {}",
                       savedUser.getUserId(), savedUser.getFullName(), savedUser.getEmail(), savedUser.getPhoneNumber());
            logger.info("Created Patient - ID: {}, UserID: {}, Gender: {}, DOB: {}",
                       savedPatient.getPatientId(), savedPatient.getUserId(), savedPatient.getGender(), savedPatient.getDateOfBirth());
            logger.info("Patient.User relationship: {}", savedPatient.getUser() != null ? "SET" : "NULL");

            return savedUser;

        } catch (Exception e) {
            // Log the error
            logger.error("Error registering new patient: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to register patient: " + e.getMessage());
        }
    }

    // Fetch all patients for registration management
    public List<Patient> getAllPatients() {
        return patientRepository.findAll();
    }

    @SuppressWarnings("unused") // This method might be used elsewhere
    public List<PatientContact> getAllPatientContacts() {
        return patientContactRepository.findAll();
    }

    public List<PatientContact> getPatientContactsByPatientId(Integer patientId) {
        return patientContactRepository.findByPatientId(patientId);
    }

    /**
     * Update patient information
     */
    @Transactional
    public Patient updatePatient(Patient patient) {
        try {
            return patientRepository.save(patient);
        } catch (Exception e) {
            logger.error("Error updating patient: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to update patient: " + e.getMessage());
        }
    }

    /**
     * Save patient contact information
     */
    @Transactional
    public PatientContact savePatientContact(PatientContact contact) {
        try {
            return patientContactRepository.save(contact);
        } catch (Exception e) {
            logger.error("Error saving patient contact: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save patient contact: " + e.getMessage());
        }
    }

    // Check if the user is a Receptionist
    @SuppressWarnings("unused") // This method might be used elsewhere
    public boolean isReceptionist(Users user) {
        return user.getRole() != null && user.getRole().getRoleName().equals("RECEPTIONIST");
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
    @SuppressWarnings("unused") // This method might be used elsewhere
    public Page<Users> getAllReceptionists(Pageable pageable) {
        return receptionistRepository.findAllReceptionistsFiltered(null, null, pageable);
    }

    @SuppressWarnings("unused") // This method might be used elsewhere
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

    @SuppressWarnings("unused") // Parameter period is intended for future use
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

    // New method to get pending appointments only
    public List<Map<String, Object>> getPendingAppointmentTableData() {
        // Get all appointments and filter for Pending status
        List<Appointment> allAppointments = getAllAppointments();

        return allAppointments.stream()
                .filter(appointment -> "Pending".equalsIgnoreCase(appointment.getStatus()))
                .map(appointment -> {
                    Map<String, Object> appointmentData = new HashMap<>();
                    appointmentData.put("id", appointment.getAppointmentId());
                    appointmentData.put("name", appointment.getPatient().getUser().getFullName());
                    appointmentData.put("phone", appointment.getPatient().getUser().getPhoneNumber());
                    appointmentData.put("gender", appointment.getPatient().getGender());
                    appointmentData.put("date", appointment.getDateTime().toLocalDate().toString());
                    appointmentData.put("time", appointment.getDateTime().toLocalTime().toString());
                    appointmentData.put("status", appointment.getStatus());
                    appointmentData.put("email", appointment.getEmail());
                    appointmentData.put("description", appointment.getDescription());
                    appointmentData.put("appointmentDateTime", appointment.getDateTime().toString());
                    appointmentData.put("dateTime", appointment.getDateTime().toString());
                    if (appointment.getDoctor() != null && appointment.getDoctor().getUser() != null) {
                        appointmentData.put("doctorName", appointment.getDoctor().getUser().getFullName());
                    } else {
                        appointmentData.put("doctorName", "Not Assigned");
                    }
                    return appointmentData;
                })
                .collect(Collectors.toList());
    }

    // New method to get today's pending appointments
    public List<Map<String, Object>> getTodaysPendingAppointmentTableData(String dateStr) {
        try {
            // Parse the date string
            java.time.LocalDate targetDate = java.time.LocalDate.parse(dateStr);

            // Get all appointments and filter for today's date and Pending status
            List<Appointment> allAppointments = getAllAppointments();

            System.out.println("Total appointments in database: " + allAppointments.size());

            List<Map<String, Object>> todaysAppointments = allAppointments.stream()
                    .filter(appointment -> {
                        // Check if appointment date matches target date
                        java.time.LocalDate appointmentDate = appointment.getDateTime().toLocalDate();
                        boolean isToday = appointmentDate.equals(targetDate);

                        // Check if status is Pending
                        boolean isPending = "Pending".equalsIgnoreCase(appointment.getStatus());

                        if (isToday && isPending) {
                            System.out.println("Found matching appointment: " + appointment.getAppointmentId() +
                                    " on " + appointmentDate + " with status " + appointment.getStatus());
                        }

                        return isToday && isPending;
                    })
                    .map(appointment -> {
                        Map<String, Object> appointmentData = new HashMap<>();
                        appointmentData.put("id", appointment.getAppointmentId());
                        appointmentData.put("name", appointment.getPatient().getUser().getFullName());
                        appointmentData.put("phone", appointment.getPatient().getUser().getPhoneNumber());
                        appointmentData.put("gender", appointment.getPatient().getGender());
                        appointmentData.put("date", appointment.getDateTime().toLocalDate().toString());
                        appointmentData.put("time", appointment.getDateTime().toLocalTime().toString());
                        appointmentData.put("status", appointment.getStatus());
                        appointmentData.put("email", appointment.getEmail());
                        appointmentData.put("description", appointment.getDescription());
                        appointmentData.put("appointmentDateTime", appointment.getDateTime().toString());
                        appointmentData.put("dateTime", appointment.getDateTime().toString());
                        appointmentData.put("patientId", appointment.getPatient().getPatientId());

                        if (appointment.getDoctor() != null && appointment.getDoctor().getUser() != null) {
                            appointmentData.put("doctorName", appointment.getDoctor().getUser().getFullName());
                        } else {
                            appointmentData.put("doctorName", "Not Assigned");
                        }

                        System.out.println("Processed appointment data: " + appointmentData);
                        return appointmentData;
                    })
                    .collect(Collectors.toList());

            System.out.println("Returning " + todaysAppointments.size() + " appointments for date " + dateStr);
            return todaysAppointments;

        } catch (Exception e) {
            logger.error("Error filtering today's appointments: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    @SuppressWarnings("unused") // This method might be used elsewhere
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
                    doctorMap.put("imageUrl", doctor.getImageUrl()); // Add the imageUrl field

                    // Get specialty information - show primary specialty or all specialties
                    String specialty = "General Practice"; // Default value
                    if (doctor.getSpecializations() != null && !doctor.getSpecializations().isEmpty()) {
                        // For better readability, show only the first specialty in lists
                        // but keep all specialties for detailed views
                        specialty = doctor.getSpecializations().stream()
                                .map(Specialization::getSpecName)
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
                        .map(Specialization::getSpecName)
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
                                .map(Specialization::getSpecName)
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
                    .map(Specialization::getSpecName)
                    .collect(Collectors.joining(", "));
        }
        doctorMap.put("specialty", specialty);

        return doctorMap;
    }

    // New payment methods

    /**
     * Get today's payment data for the payments page
     * Amount từ Receipt.TotalAmount, Status từ Transaction.Status
     */
    public List<Map<String, Object>> getTodaysPaymentData() {
        try {
            // Sử dụng getAllReceiptsWithTransactionData() để lấy dữ liệu th���c
            List<Map<String, Object>> rawData = receiptRepository.getAllReceiptsWithTransactionData();

            // Log để debug
            System.out.println("Raw payment data count: " + rawData.size());
            if (!rawData.isEmpty()) {
                System.out.println("First payment data: " + rawData.get(0));
            }

            // Process the data to match frontend expectations
            return rawData.stream().map(payment -> {
                Map<String, Object> processedPayment = new HashMap<>();
                processedPayment.put("patientId", payment.get("patientId"));
                processedPayment.put("patientName", payment.get("fullName")); // Full Name from Users table
                processedPayment.put("visitDate", payment.get("phoneNumber")); // PhoneNumber from Users table (as per requirement)

                // Amount từ Receipt.TotalAmount - hiển thị số nguyên
                Object totalAmount = payment.get("totalAmount");
                processedPayment.put("amount", Objects.requireNonNullElse(totalAmount, 0));

                // Status từ Transaction.Status - hiển thị trực tiếp
                String transactionStatus = (String) payment.get("status");
                processedPayment.put("status", mapTransactionStatusToPaymentStatus(transactionStatus));
                processedPayment.put("receiptId", payment.get("receiptId"));
                processedPayment.put("transactionId", payment.get("transactionId"));

                // Log để debug mapping
                System.out.println("Processing payment - PatientID: " + payment.get("patientId") +
                        ", Amount: " + totalAmount +
                        ", Transaction Status: " + transactionStatus +
                        " -> Payment Status: " + processedPayment.get("status"));

                return processedPayment;
            }).collect(Collectors.toList());

        } catch (Exception e) {
            logger.error("Error fetching today's payment data: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Get test payment data for debugging - sử dụng dữ liệu thực từ Receipt và Transaction
     */
    public List<Map<String, Object>> getTestPaymentData() {
        try {
            // Thử lấy dữ liệu thực từ Receipt và Transaction trước
            List<Map<String, Object>> rawData = receiptRepository.getAllReceiptsWithTransactionData();

            System.out.println("Real receipt data count: " + rawData.size());
            if (!rawData.isEmpty()) {
                System.out.println("First real receipt data: " + rawData.get(0));
            }

            if (!rawData.isEmpty()) {
                // Nếu có dữ liệu thực, sử dụng nó
                return rawData.stream().map(payment -> {
                    Map<String, Object> processedPayment = new HashMap<>();
                    processedPayment.put("patientId", payment.get("patientId"));
                    processedPayment.put("patientName", payment.get("fullName"));
                    processedPayment.put("visitDate", payment.get("phoneNumber"));

                    // Amount từ Receipt.TotalAmount
                    Object totalAmount = payment.get("totalAmount");
                    if (totalAmount != null) {
                        try {
                            double amountValue = Double.parseDouble(totalAmount.toString());
                            processedPayment.put("amount", amountValue);
                        } catch (NumberFormatException e) {
                            logger.error("Error parsing receipt amount: {}", totalAmount);
                            processedPayment.put("amount", 0);
                        }
                    } else {
                        processedPayment.put("amount", 0);
                    }

                    // Status từ Transaction.Status
                    String transactionStatus = (String) payment.get("status");
                    processedPayment.put("status", mapTransactionStatusToPaymentStatus(transactionStatus));
                    processedPayment.put("receiptId", payment.get("receiptId"));
                    processedPayment.put("transactionId", payment.get("transactionId"));

                    System.out.println("Real data - Amount: " + totalAmount + ", Status: " + transactionStatus);
                    return processedPayment;
                }).collect(Collectors.toList());
            } else {
                // Nếu không có dữ liệu Receipt/Transaction, lấy danh sách bệnh nhân cơ bản
                List<Map<String, Object>> patientData = receiptRepository.getAllPatientsWithPaymentInfo();
                System.out.println("Patient data count: " + patientData.size());

                return patientData.stream().map(payment -> {
                    Map<String, Object> processedPayment = new HashMap<>();
                    processedPayment.put("patientId", payment.get("patientId"));
                    processedPayment.put("patientName", payment.get("fullName"));
                    processedPayment.put("visitDate", payment.get("phoneNumber"));
                    processedPayment.put("amount", 0); // Mặc định là 0 nếu không có Receipt
                    processedPayment.put("status", "unpaid"); // Mặc định là unpaid
                    processedPayment.put("receiptId", 0);
                    processedPayment.put("transactionId", 0);
                    return processedPayment;
                }).collect(Collectors.toList());
            }

        } catch (Exception e) {
            logger.error("Error fetching test payment data: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Get all receipt data with transaction info - method mới để debug
     */
    public List<Map<String, Object>> getAllReceiptsWithTransactionData() {
        try {
            List<Map<String, Object>> rawData = receiptRepository.getAllReceiptsWithTransactionData();

            System.out.println("All receipts count: " + rawData.size());
            if (!rawData.isEmpty()) {
                System.out.println("Sample receipt data: " + rawData.get(0));
            }

            return rawData.stream().map(payment -> {
                Map<String, Object> processedPayment = new HashMap<>();
                processedPayment.put("patientId", payment.get("patientId"));
                processedPayment.put("patientName", payment.get("fullName"));
                processedPayment.put("visitDate", payment.get("phoneNumber"));

                // Amount từ Receipt.TotalAmount
                Object totalAmount = payment.get("totalAmount");
                if (totalAmount != null) {
                    try {
                        double amountValue = Double.parseDouble(totalAmount.toString());
                        processedPayment.put("amount", amountValue);
                    } catch (NumberFormatException e) {
                        logger.error("Error parsing amount from receipt: {}", totalAmount);
                        processedPayment.put("amount", 0);
                    }
                } else {
                    processedPayment.put("amount", 0);
                }

                // Status từ Transaction.Status
                String transactionStatus = (String) payment.get("status");
                processedPayment.put("status", mapTransactionStatusToPaymentStatus(transactionStatus));
                processedPayment.put("receiptId", payment.get("receiptId"));
                processedPayment.put("transactionId", payment.get("transactionId"));
                processedPayment.put("issuedDate", payment.get("issuedDate"));

                System.out.println("Receipt data - Amount: " + totalAmount + ", Transaction Status: " + transactionStatus);
                return processedPayment;
            }).collect(Collectors.toList());

        } catch (Exception e) {
            logger.error("Error fetching all receipts data: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Get today's total revenue (only from completed transactions)
     */
    public Double getTodaysTotalRevenue() {
        try {
            Double total = receiptRepository.getTodaysTotalRevenue();
            return total != null ? total : 0.0;
        } catch (Exception e) {
            logger.error("Error fetching today's total revenue: {}", e.getMessage(), e);
            return 0.0;
        }
    }

    /**
     * Get filtered payment data by status for today
     */
    public List<Map<String, Object>> getTodaysPaymentDataByStatus(String status) {
        try {
            // Map frontend status to database status
            String dbStatus = mapPaymentStatusToTransactionStatus(status);
            if (dbStatus.isEmpty()) {
                // If no valid status mapping, return all today's data
                return getTodaysPaymentData();
            }

            List<Map<String, Object>> rawData = receiptRepository.getTodaysPaymentDataByStatus(dbStatus);

            // Process the data to match frontend expectations
            return rawData.stream().map(payment -> {
                Map<String, Object> processedPayment = new HashMap<>();
                processedPayment.put("patientId", payment.get("patientId"));
                processedPayment.put("patientName", payment.get("fullName")); // Full Name from Users table
                processedPayment.put("visitDate", payment.get("phoneNumber")); // PhoneNumber from Users table (as per requirement)

                // Format amount without VND
                Object totalAmount = payment.get("totalAmount");
                if (totalAmount != null) {
                    processedPayment.put("amount", totalAmount);
                } else {
                    processedPayment.put("amount", 0);
                }

                processedPayment.put("status", mapTransactionStatusToPaymentStatus((String) payment.get("status")));
                processedPayment.put("receiptId", payment.get("receiptId"));
                processedPayment.put("transactionId", payment.get("transactionId"));
                return processedPayment;
            }).collect(Collectors.toList());

        } catch (Exception e) {
            logger.error("Error fetching filtered payment data: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Map transaction status from database to payment status for frontend
     */
    private String mapTransactionStatusToPaymentStatus(String transactionStatus) {
        if (transactionStatus == null) {
            return "pending";
        }

        return switch (transactionStatus.toLowerCase()) {
            case "paid" -> "paid";
            case "pending" -> "pending";
            case "refunded" -> "refunded";
            default -> "pending"; // Default to pending for any unknown status
        };
    }

    /**
     * Map payment status from frontend to transaction status for database query
     */
    private String mapPaymentStatusToTransactionStatus(String paymentStatus) {
        if (paymentStatus == null || paymentStatus.isEmpty()) {
            return "";
        }

        return switch (paymentStatus.toLowerCase()) {
            case "paid" -> "Paid";
            case "pending" -> "Pending";
            case "refunded" -> "Refunded";
            default -> "";
        };
    }

    /**
     * Get patients with appointments today and their transaction status
     * Logic: Check today's appointments by DateTime -> map to PatientID -> get Transaction status
     */
    public List<Map<String, Object>> getPatientsWithAppointmentsToday() {
        try {
            // Get today's date
            java.time.LocalDate today = java.time.LocalDate.now();
            java.time.LocalDateTime startOfDay = today.atStartOfDay();
            java.time.LocalDateTime endOfDay = today.atTime(23, 59, 59);

            System.out.println("Fetching appointments for today: " + today);

            // Get all appointments for today using DateTime from Appointment table
            List<Appointment> todaysAppointments = appointmentRepository.findByDateTimeBetween(startOfDay, endOfDay);
            System.out.println("Found " + todaysAppointments.size() + " appointments for today");

            List<Map<String, Object>> result = new ArrayList<>();

            for (Appointment appointment : todaysAppointments) {
                try {
                    Map<String, Object> patientPaymentData = new HashMap<>();

                    // Get patient information from appointment
                    Patient patient = appointment.getPatient();
                    if (patient == null || patient.getUser() == null) {
                        System.out.println("Skipping appointment " + appointment.getAppointmentId() + " - missing patient data");
                        continue;
                    }

                    // Basic patient info
                    patientPaymentData.put("appointmentId", appointment.getAppointmentId());
                    patientPaymentData.put("patientId", patient.getPatientId());
                    patientPaymentData.put("patientName", patient.getUser().getFullName());
                    patientPaymentData.put("visitDate", patient.getUser().getPhoneNumber()); // Phone as per requirement
                    patientPaymentData.put("appointmentDate", appointment.getDateTime().toLocalDate().toString());
                    patientPaymentData.put("appointmentTime", appointment.getDateTime().toLocalTime().toString());
                    patientPaymentData.put("appointmentStatus", appointment.getStatus());

                    // Get transaction status by AppointmentID mapping
                    String transactionStatus = "unpaid"; // Default status
                    Double amount = 0.0; // Default amount
                    Integer receiptId = null;
                    Integer transactionId = null;

                    try {
                        // Find transactions linked to this appointment
                        List<Transaction> transactions = transactionRepository.findByAppointmentId(appointment.getAppointmentId());

                        if (!transactions.isEmpty()) {
                            // Get the latest transaction
                            Transaction latestTransaction = transactions.get(0);
                            transactionStatus = mapTransactionStatusToPaymentStatus(latestTransaction.getStatus());
                            transactionId = latestTransaction.getTransactionId();

                            // Get amount from Receipt, not Transaction
                            if (latestTransaction.getReceipt() != null && latestTransaction.getReceipt().getTotalAmount() != null) {
                                amount = latestTransaction.getReceipt().getTotalAmount().doubleValue();
                            }

                            // Try to find corresponding receipt
                            try {
                                List<Map<String, Object>> receipts = receiptRepository.getReceiptByTransactionId(latestTransaction.getTransactionId());
                                if (!receipts.isEmpty()) {
                                    Map<String, Object> receipt = receipts.get(0);
                                    receiptId = (Integer) receipt.get("receiptId");
                                    Object totalAmount = receipt.get("totalAmount");
                                    if (totalAmount != null) {
                                        amount = Double.parseDouble(totalAmount.toString());
                                    }
                                }
                            } catch (Exception receiptError) {
                                logger.error("Error fetching receipt for transaction {}: {}", latestTransaction.getTransactionId(), receiptError.getMessage());
                            }
                        }
                    } catch (Exception transactionError) {
                        logger.error("Error fetching transactions for appointment {}: {}", appointment.getAppointmentId(), transactionError.getMessage());
                    }

                    // Set payment information
                    patientPaymentData.put("amount", amount.longValue()); // Convert to long to remove decimals
                    patientPaymentData.put("status", transactionStatus);
                    patientPaymentData.put("receiptId", receiptId);
                    patientPaymentData.put("transactionId", transactionId);

                    result.add(patientPaymentData);

                    System.out.println("Processed appointment " + appointment.getAppointmentId() +
                            " for patient " + patient.getPatientId() +
                            " with status: " + transactionStatus +
                            " and amount: " + amount);

                } catch (Exception e) {
                    logger.error("Error processing appointment {}: {}", appointment.getAppointmentId(), e.getMessage(), e);
                }
            }

            System.out.println("Returning " + result.size() + " patients with appointments today");
            return result;

        } catch (Exception e) {
            logger.error("Error fetching patients with appointments today: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Get today's appointment payment data based on DateTime in Appointment table
     * Map by AppointmentID to get PatientID and Transaction Status
     * Logic: Check today's appointments by DateTime -> map to PatientID -> get Transaction status
     */
    public List<Map<String, Object>> getTodaysAppointmentPaymentData() {
        try {
            // Get today's date
            java.time.LocalDate today = java.time.LocalDate.now();
            java.time.LocalDateTime startOfDay = today.atStartOfDay();
            java.time.LocalDateTime endOfDay = today.atTime(23, 59, 59);

            System.out.println("Fetching appointments for today: " + today);

            // Get all appointments for today using DateTime from Appointment table
            List<Appointment> todaysAppointments = appointmentRepository.findByDateTimeBetween(startOfDay, endOfDay);
            System.out.println("Found " + todaysAppointments.size() + " appointments for today");

            List<Map<String, Object>> result = new ArrayList<>();

            for (Appointment appointment : todaysAppointments) {
                try {
                    Map<String, Object> patientPaymentData = new HashMap<>();

                    // Get patient information from appointment
                    Patient patient = appointment.getPatient();
                    if (patient == null || patient.getUser() == null) {
                        System.out.println("Skipping appointment " + appointment.getAppointmentId() + " - missing patient data");
                        continue;
                    }

                    // Basic patient info
                    patientPaymentData.put("appointmentId", appointment.getAppointmentId());
                    patientPaymentData.put("patientId", patient.getPatientId());
                    patientPaymentData.put("patientName", patient.getUser().getFullName());
                    patientPaymentData.put("phoneNumber", patient.getUser().getPhoneNumber());
                    patientPaymentData.put("appointmentDate", appointment.getDateTime().toLocalDate().toString());
                    patientPaymentData.put("appointmentTime", appointment.getDateTime().toLocalTime().toString());
                    patientPaymentData.put("appointmentStatus", appointment.getStatus());

                    // Get doctor information
                    if (appointment.getDoctor() != null && appointment.getDoctor().getUser() != null) {
                        patientPaymentData.put("doctorName", appointment.getDoctor().getUser().getFullName());
                    } else {
                        patientPaymentData.put("doctorName", "Not Assigned");
                    }

                    // Get transaction status by AppointmentID mapping
                    String transactionStatus = "unpaid"; // Default status
                    Double amount = 0.0; // Default amount
                    Integer receiptId = null;
                    Integer transactionId = null;

                    try {
                        // Find transactions linked to this appointment
                        List<Transaction> transactions = transactionRepository.findByAppointmentId(appointment.getAppointmentId());

                        if (!transactions.isEmpty()) {
                            // Get the latest transaction
                            Transaction latestTransaction = transactions.get(0);
                            transactionStatus = mapTransactionStatusToPaymentStatus(latestTransaction.getStatus());
                            transactionId = latestTransaction.getTransactionId();

                            // Get amount from Receipt, not Transaction
                            if (latestTransaction.getReceipt() != null && latestTransaction.getReceipt().getTotalAmount() != null) {
                                amount = latestTransaction.getReceipt().getTotalAmount().doubleValue();
                            }

                            // Try to find corresponding receipt
                            try {
                                List<Map<String, Object>> receipts = receiptRepository.getReceiptByTransactionId(latestTransaction.getTransactionId());
                                if (!receipts.isEmpty()) {
                                    Map<String, Object> receipt = receipts.get(0);
                                    receiptId = (Integer) receipt.get("receiptId");
                                    Object totalAmount = receipt.get("totalAmount");
                                    if (totalAmount != null) {
                                        amount = Double.parseDouble(totalAmount.toString());
                                    }
                                }
                            } catch (Exception receiptError) {
                                logger.error("Error fetching receipt for transaction {}: {}", latestTransaction.getTransactionId(), receiptError.getMessage());
                            }
                        }
                    } catch (Exception transactionError) {
                        logger.error("Error fetching transactions for appointment {}: {}", appointment.getAppointmentId(), transactionError.getMessage());
                    }

                    // Set payment information
                    patientPaymentData.put("amount", amount.longValue()); // Convert to long to remove decimals
                    patientPaymentData.put("status", transactionStatus);
                    patientPaymentData.put("receiptId", receiptId);
                    patientPaymentData.put("transactionId", transactionId);

                    result.add(patientPaymentData);

                    System.out.println("Processed appointment " + appointment.getAppointmentId() +
                            " for patient " + patient.getPatientId() +
                            " with status: " + transactionStatus +
                            " and amount: " + amount);

                } catch (Exception e) {
                    logger.error("Error processing appointment {}: {}", appointment.getAppointmentId(), e.getMessage(), e);
                }
            }

            System.out.println("Returning " + result.size() + " patients with appointments today");
            return result;

        } catch (Exception e) {
            logger.error("Error fetching today's appointment payment data: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Get today's total revenue from appointments only
     */
    public Double getTodaysAppointmentPaymentRevenue() {
        try {
            // Get today's date
            java.time.LocalDate today = java.time.LocalDate.now();
            java.time.LocalDateTime startOfDay = today.atStartOfDay();
            java.time.LocalDateTime endOfDay = today.atTime(23, 59, 59);

            // Get all appointments for today
            List<Appointment> todaysAppointments = appointmentRepository.findByDateTimeBetween(startOfDay, endOfDay);

            double totalRevenue = 0.0;

            for (Appointment appointment : todaysAppointments) {
                try {
                    // Find transactions linked to this appointment
                    List<Transaction> transactions = transactionRepository.findByAppointmentId(appointment.getAppointmentId());

                    for (Transaction transaction : transactions) {
                        // Only count completed transactions
                        if ("Completed".equalsIgnoreCase(transaction.getStatus()) || "Success".equalsIgnoreCase(transaction.getStatus())) {
                            // Get amount from Receipt, not Transaction
                            if (transaction.getReceipt() != null && transaction.getReceipt().getTotalAmount() != null) {
                                totalRevenue += transaction.getReceipt().getTotalAmount().doubleValue();
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.error("Error calculating revenue for appointment {}: {}", appointment.getAppointmentId(), e.getMessage());
                }
            }

            return totalRevenue;

        } catch (Exception e) {
            logger.error("Error calculating today's appointment payment revenue: {}", e.getMessage(), e);
            return 0.0;
        }
    }

    /**
     * Get all available transaction statuses from database
     */
    public List<String> getAllAvailableStatuses() {
        try {
            // Get all unique statuses from Receipt/Transaction data
            List<Map<String, Object>> rawData = receiptRepository.getAllReceiptsWithTransactionData();

            return rawData.stream()
                    .map(payment -> (String) payment.get("status"))
                    .filter(status -> status != null && !status.trim().isEmpty())
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error fetching available statuses: {}", e.getMessage(), e);
            // Return default statuses if query fails
            return List.of("Pending", "Completed", "Failed", "Cancelled", "Refunded");
        }
    }

    /**
     * Get today's payment data without status mapping - show raw database status
     */
    public List<Map<String, Object>> getTodaysPaymentDataRaw() {
        try {
            List<Map<String, Object>> rawData = receiptRepository.getAllReceiptsWithTransactionData();

            return rawData.stream().map(payment -> {
                Map<String, Object> processedPayment = new HashMap<>();
                processedPayment.put("patientId", payment.get("patientId"));
                processedPayment.put("patientName", payment.get("fullName"));
                processedPayment.put("visitDate", payment.get("phoneNumber"));

                // Show raw amount and status without processing
                processedPayment.put("amount", Objects.requireNonNullElse(payment.get("totalAmount"), 0));
                processedPayment.put("status", payment.get("status")); // Raw status from database
                processedPayment.put("receiptId", payment.get("receiptId"));
                processedPayment.put("transactionId", payment.get("transactionId"));

                return processedPayment;
            }).collect(Collectors.toList());

        } catch (Exception e) {
            logger.error("Error fetching today's payment data (raw): {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Get rooms by specialty and doctor based on the SQL logic:
     * SELECT DISTINCT s.SpecName AS SpecializationName, u.FullName AS DoctorName,
     * r.RoomID, r.RoomNumber, d.DeptName AS DepartmentName
     * FROM Specialization s
     * INNER JOIN DoctorSpecialization ds ON s.SpecID = ds.SpecID
     * INNER JOIN Doctor doc ON ds.DoctorID = doc.DoctorID
     * INNER JOIN Users u ON doc.UserID = u.UserID
     * LEFT JOIN Schedule sch ON doc.DoctorID = sch.DoctorID
     * LEFT JOIN Room r ON sch.RoomID = r.RoomID
     * LEFT JOIN Department d ON r.DepartmentID = d.DepartmentID
     * ORDER BY u.FullName, s.SpecName, r.RoomNumber;
     */
    public List<Map<String, Object>> getRoomsBySpecialtyAndDoctor(Integer specialtyId, Integer doctorId) {
        try {
            // First validate that the doctor has the required specialty
            if (!validateDoctorSpecialty(doctorId, specialtyId)) {
                return new ArrayList<>();
            }

            // Get doctor by ID to access schedule information
            Optional<orochi.model.Doctor> doctorOptional = DoctorRepository.findById(doctorId);
            if (doctorOptional.isEmpty()) {
                return new ArrayList<>();
            }

            orochi.model.Doctor doctor = doctorOptional.get();

            // Get specialty name for validation
            String specializationName = doctor.getSpecializations().stream()
                    .filter(spec -> spec.getSpecId().equals(specialtyId))
                    .map(Specialization::getSpecName)
                    .findFirst()
                    .orElse("Unknown");

            // Get rooms from doctor's schedule that match the department
            List<Map<String, Object>> rooms = new ArrayList<>();

            if (doctor.getSchedules() != null && !doctor.getSchedules().isEmpty()) {
                // Get rooms from doctor's schedules
                Set<Room> doctorRooms = doctor.getSchedules().stream()
                        .map(Schedule::getRoom)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());

                for (Room room : doctorRooms) {
                    // Validate that the room's department matches the specialty
                    if (room.getDepartment() != null) {
                        String deptName = room.getDepartment().getDeptName();

                        // Check if department name matches or contains the specialization
                        // This is a simple matching logic - you can refine this based on your data
                        if (deptName.toLowerCase().contains(specializationName.toLowerCase()) ||
                                specializationName.toLowerCase().contains(deptName.toLowerCase())) {

                            Map<String, Object> roomData = new HashMap<>();
                            roomData.put("roomId", room.getRoomId());
                            roomData.put("roomNumber", room.getRoomNumber());
                            roomData.put("roomName", room.getRoomName());
                            roomData.put("departmentName", deptName);
                            roomData.put("specializationName", specializationName);
                            roomData.put("doctorName", doctor.getUser().getFullName());
                            roomData.put("capacity", room.getCapacity());
                            roomData.put("status", room.getStatus());

                            rooms.add(roomData);
                        }
                    }
                }
            }

            // If no rooms found from schedule, get available rooms from the same department
            if (rooms.isEmpty()) {
                // Find specialty and get its associated department
                // For now, we'll get all available rooms and let the frontend handle selection
                List<Room> availableRooms = roomRepository.findAllAvailableRooms();

                for (Room room : availableRooms) {
                    if (room.getDepartment() != null) {
                        Map<String, Object> roomData = new HashMap<>();
                        roomData.put("roomId", room.getRoomId());
                        roomData.put("roomNumber", room.getRoomNumber());
                        roomData.put("roomName", room.getRoomName());
                        roomData.put("departmentName", room.getDepartment().getDeptName());
                        roomData.put("specializationName", specializationName);
                        roomData.put("doctorName", doctor.getUser().getFullName());
                        roomData.put("capacity", room.getCapacity());
                        roomData.put("status", room.getStatus());

                        rooms.add(roomData);
                    }
                }
            }

            // Sort by room number
            rooms.sort((r1, r2) -> {
                String room1 = (String) r1.get("roomNumber");
                String room2 = (String) r2.get("roomNumber");
                return room1.compareTo(room2);
            });

            return rooms;

        } catch (Exception e) {
            logger.error("Error fetching rooms by specialty and doctor: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Get payment history data according to user requirements
     * Using the new query with proper joins and field mapping
     */
    public List<Map<String, Object>> getPaymentHistoryData() {
        try {
            List<Map<String, Object>> rawData = receiptRepository.getAllPaymentHistoryData();

            logger.info("Payment history data count: {}", rawData.size());
            if (!rawData.isEmpty()) {
                logger.info("First payment history record: {}", rawData.get(0));
            }

            return rawData.stream().map(payment -> {
                Map<String, Object> processedPayment = new HashMap<>();

                // Patient ID từ Patient table
                processedPayment.put("patientId", payment.get("patientId"));

                // Patient Name từ Users table với RoleID = 4
                processedPayment.put("patientName", payment.get("patientName"));

                // Phone từ Users.PhoneNumber
                processedPayment.put("phone", payment.get("phone"));

                // Appointment ID từ Appointment table
                processedPayment.put("appointmentId", payment.get("appointmentId"));

                // DateTime từ Appointment table - format để hiển thị
                Object dateTime = payment.get("dateTime");
                if (dateTime != null) {
                    processedPayment.put("dateTime", dateTime.toString());
                } else {
                    processedPayment.put("dateTime", "");
                }

                // Status từ Transaction table (chỉ Status = 'Paid')
                processedPayment.put("status", payment.get("status"));

                // Method từ Transaction table
                Object method = payment.get("method");
                processedPayment.put("method", method != null ? method : "Unknown");

                // Amount từ Receipt.TotalAmount - format với $ và số
                Object amount = payment.get("amount");
                if (amount != null) {
                    try {
                        double amountValue = Double.parseDouble(amount.toString());
                        processedPayment.put("amount", String.format("$%.2f", amountValue));
                    } catch (NumberFormatException e) {
                        processedPayment.put("amount", "$0.00");
                    }
                } else {
                    processedPayment.put("amount", "$0.00");
                }

                // Additional fields cho debug và reference
                processedPayment.put("transactionId", payment.get("transactionId"));
                processedPayment.put("receiptId", payment.get("receiptId"));
                processedPayment.put("timeOfPayment", payment.get("timeOfPayment"));

                return processedPayment;
            }).collect(Collectors.toList());

        } catch (Exception e) {
            logger.error("Error fetching payment history data: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Get payment history data with date range filter
     */
    public List<Map<String, Object>> getPaymentHistoryDataByDateRange(LocalDate fromDate, LocalDate toDate) {
        try {
            List<Map<String, Object>> rawData = receiptRepository.getPaymentHistoryDataByDateRange(fromDate, toDate);

            logger.info("Payment history data count for date range {}-{}: {}", fromDate, toDate, rawData.size());

            return rawData.stream().map(payment -> {
                Map<String, Object> processedPayment = new HashMap<>();

                processedPayment.put("patientId", payment.get("patientId"));
                processedPayment.put("patientName", payment.get("patientName"));
                processedPayment.put("phone", payment.get("phone"));
                processedPayment.put("appointmentId", payment.get("appointmentId"));

                // DateTime formatting
                Object dateTime = payment.get("dateTime");
                if (dateTime != null) {
                    processedPayment.put("dateTime", dateTime.toString());
                } else {
                    processedPayment.put("dateTime", "");
                }

                processedPayment.put("status", payment.get("status"));

                Object method = payment.get("method");
                processedPayment.put("method", method != null ? method : "Unknown");

                // Amount formatting
                Object amount = payment.get("amount");
                if (amount != null) {
                    try {
                        double amountValue = Double.parseDouble(amount.toString());
                        processedPayment.put("amount", String.format("$%.2f", amountValue));
                    } catch (NumberFormatException e) {
                        processedPayment.put("amount", "$0.00");
                    }
                } else {
                    processedPayment.put("amount", "$0.00");
                }

                processedPayment.put("transactionId", payment.get("transactionId"));
                processedPayment.put("receiptId", payment.get("receiptId"));
                processedPayment.put("timeOfPayment", payment.get("timeOfPayment"));

                return processedPayment;
            }).collect(Collectors.toList());

        } catch (Exception e) {
            logger.error("Error fetching payment history data by date range: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Get all payment history data for testing (without Status filter)
     */
    public List<Map<String, Object>> getAllPaymentHistoryDataForTesting() {
        try {
            List<Map<String, Object>> rawData = receiptRepository.getAllPaymentHistoryData();

            logger.info("All payment history data count: {}", rawData.size());
            if (!rawData.isEmpty()) {
                logger.info("First payment history record (all): {}", rawData.get(0));
            }

            return rawData.stream().map(payment -> {
                Map<String, Object> processedPayment = new HashMap<>();

                processedPayment.put("patientId", payment.get("patientId"));
                processedPayment.put("patientName", payment.get("patientName"));
                processedPayment.put("phone", payment.get("phone"));
                processedPayment.put("appointmentId", payment.get("appointmentId"));

                Object dateTime = payment.get("dateTime");
                if (dateTime != null) {
                    processedPayment.put("dateTime", dateTime.toString());
                } else {
                    processedPayment.put("dateTime", "");
                }

                processedPayment.put("status", payment.get("status"));

                Object method = payment.get("method");
                processedPayment.put("method", method != null ? method : "Unknown");

                Object amount = payment.get("amount");
                if (amount != null) {
                    try {
                        double amountValue = Double.parseDouble(amount.toString());
                        processedPayment.put("amount", String.format("$%.2f", amountValue));
                    } catch (NumberFormatException e) {
                        processedPayment.put("amount", "$0.00");
                    }
                } else {
                    processedPayment.put("amount", "$0.00");
                }

                processedPayment.put("transactionId", payment.get("transactionId"));
                processedPayment.put("receiptId", payment.get("receiptId"));
                processedPayment.put("timeOfPayment", payment.get("timeOfPayment"));

                return processedPayment;
            }).collect(Collectors.toList());

        } catch (Exception e) {
            logger.error("Error fetching all payment history data: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Get invoice data by patient ID including patient info and appointment details
     * Patient ID: lấy theo Patient ID trong bảng Patient
     * Full Name: lấy theo FullName trong bảng User
     * Date of Birth: lấy theo dateOfBirth trong bảng Patient
     * Gender: lấy theo Gender trong bảng Patient
     * Appointment ID: lấy theo Patient ID trong b��ng Patient
     * Payer Name: lấy theo AppointmentID trong bảng Appointment
     * Contact: lấy theo PhoneNumber trong bảng User
     */
    public Map<String, Object> getInvoiceDataByPatientId(Integer patientId) {
        try {
            // Tìm bệnh nhân theo PatientID
            Optional<Patient> patientOptional = patientRepository.findById(patientId);
            if (patientOptional.isEmpty()) {
                logger.error("Patient not found with ID: {}", patientId);
                return null;
            }

            Patient patient = patientOptional.get();
            Users user = patient.getUser();

            if (user == null) {
                logger.error("User not found for patient ID: {}", patientId);
                return null;
            }

            logger.info("=== Patient Info Debug ===");
            logger.info("Patient ID: {}", patient.getPatientId());
            logger.info("User ID: {}", user.getUserId());
            logger.info("Full Name: {}", user.getFullName());
            logger.info("Email: {}", user.getEmail());
            logger.info("Phone: {}", user.getPhoneNumber());

            // Tìm appointment gần nhất của bệnh nhân này
            List<Appointment> appointments = appointmentRepository.findByPatientPatientIdOrderByDateTimeDesc(patientId);
            Appointment latestAppointment = null;
            if (!appointments.isEmpty()) {
                latestAppointment = appointments.get(0); // Lấy appointment gần nhất
                logger.info("Latest appointment ID: {}", latestAppointment.getAppointmentId());
            }

            // T��o dữ liệu invoice
            Map<String, Object> invoiceData = new HashMap<>();

            // Patient Information - Đảm bảo lấy đúng FullName từ Users table
            invoiceData.put("patientId", patient.getPatientId());
            invoiceData.put("fullName", user.getFullName() != null ? user.getFullName() : "Unknown Patient");
            invoiceData.put("dateOfBirth", patient.getDateOfBirth() != null ?
                    patient.getDateOfBirth().toString() : "N/A");
            invoiceData.put("gender", patient.getGender() != null ? patient.getGender() : "N/A");
            invoiceData.put("contact", user.getPhoneNumber() != null ? user.getPhoneNumber() : "N/A");
            invoiceData.put("email", user.getEmail() != null ? user.getEmail() : "N/A");

            // Appointment Information
            if (latestAppointment != null) {
                invoiceData.put("appointmentId", latestAppointment.getAppointmentId());
                // Payer name should be the same as patient name (FullName from Users table)
                invoiceData.put("payerName", user.getFullName() != null ? user.getFullName() : "Unknown Patient");
                invoiceData.put("appointmentDate", latestAppointment.getDateTime().toLocalDate().toString());
                invoiceData.put("appointmentTime", latestAppointment.getDateTime().toLocalTime().toString());
                invoiceData.put("description", latestAppointment.getDescription() != null ?
                        latestAppointment.getDescription() : "General Consultation");

                // Doctor information
                if (latestAppointment.getDoctor() != null && latestAppointment.getDoctor().getUser() != null) {
                    invoiceData.put("doctorName", latestAppointment.getDoctor().getUser().getFullName());
                } else {
                    invoiceData.put("doctorName", "Not Assigned");
                }

                // Room information
                if (latestAppointment.getRoom() != null) {
                    invoiceData.put("roomNumber", latestAppointment.getRoom().getRoomNumber());
                } else {
                    invoiceData.put("roomNumber", "Not Assigned");
                }
            } else {
                invoiceData.put("appointmentId", "N/A");
                // Even without appointment, payer name should be patient name
                invoiceData.put("payerName", user.getFullName() != null ? user.getFullName() : "Unknown Patient");
                invoiceData.put("appointmentDate", "N/A");
                invoiceData.put("appointmentTime", "N/A");
                invoiceData.put("description", "No appointment found");
                invoiceData.put("doctorName", "N/A");
                invoiceData.put("roomNumber", "N/A");
            }

            // Service Information - Lấy dữ liệu từ bảng Service
            List<Map<String, Object>> servicesUsed = getServicesUsedByPatient(patientId, latestAppointment);
            invoiceData.put("servicesUsed", servicesUsed);

            // Payment/Transaction Information
            try {
                if (latestAppointment != null) {
                    List<Transaction> transactions = transactionRepository.findByAppointmentId(latestAppointment.getAppointmentId());
                    if (!transactions.isEmpty()) {
                        Transaction latestTransaction = transactions.get(0);
                        invoiceData.put("transactionId", latestTransaction.getTransactionId());
                        invoiceData.put("transactionStatus", latestTransaction.getStatus());
                        invoiceData.put("paymentMethod", latestTransaction.getMethod());

                        // Generate receipt number based on transaction ID
                        invoiceData.put("receiptNumber", "REC" + latestTransaction.getTransactionId());

                        // Add financial calculation properties that template expects
                        double calculatedTotal = calculateTotalFromServices(servicesUsed);
                        invoiceData.put("totalAmount", calculatedTotal);
                        invoiceData.put("taxAmount", calculatedTotal * 0.1); // 10% tax
                        invoiceData.put("discountAmount", 0.0);
                        invoiceData.put("issuedDate", LocalDate.now().toString());

                        // Add receipt information
                        if (latestTransaction.getReceipt() != null) {
                            invoiceData.put("receiptId", latestTransaction.getReceipt().getReceiptId());
                            // Use receipt total amount if available, otherwise use calculated
                            if (latestTransaction.getReceipt().getTotalAmount() != null) {
                                double receiptTotal = latestTransaction.getReceipt().getTotalAmount().doubleValue();
                                invoiceData.put("totalAmount", receiptTotal);
                                invoiceData.put("taxAmount", receiptTotal * 0.1);
                            }
                        } else {
                            invoiceData.put("receiptId", "N/A");
                        }

                        // Thêm thông tin payment details từ transaction
                        invoiceData.put("timeOfPayment", latestTransaction.getTimeOfPayment() != null ?
                               latestTransaction.getTimeOfPayment().toString() : "");

                        // Parse payment details from refundReason field - SỬA ĐỂ PARSE CHÍNH XÁC
                        String refundReason = latestTransaction.getRefundReason();
                        logger.info("=== Parsing refundReason ===");
                        logger.info("Raw refundReason: {}", refundReason);

                        if (refundReason != null && !refundReason.trim().isEmpty()) {
                            // Parse Amount Received
                            if (refundReason.contains("Amount Received:")) {
                                try {
                                    String amountPart = refundReason.substring(refundReason.indexOf("Amount Received: $") + 18);
                                    String amountStr = amountPart;

                                    // Tìm end của amount (trước | hoặc Notes:)
                                    if (amountStr.contains(" |")) {
                                        amountStr = amountStr.substring(0, amountStr.indexOf(" |")).trim();
                                    } else if (amountStr.contains(" Notes:")) {
                                        amountStr = amountStr.substring(0, amountStr.indexOf(" Notes:")).trim();
                                    }

                                    double amountReceived = Double.parseDouble(amountStr);
                                    invoiceData.put("amountReceived", amountReceived);
                                    logger.info("Successfully parsed amountReceived: {}", amountReceived);

                                    // Calculate change từ total amount nếu có
                                    Object totalAmountFromInvoice = invoiceData.get("totalAmount");
                                    double totalForChange = totalAmountFromInvoice != null ?
                                        ((Number) totalAmountFromInvoice).doubleValue() :
                                        calculateTotalFromServices(servicesUsed);
                                    double change = amountReceived - totalForChange;
                                    invoiceData.put("changeReturned", change > 0 ? change : 0.0);
                                    logger.info("Calculated change: {} (amountReceived: {} - total: {})", change, amountReceived, totalForChange);

                                } catch (Exception e) {
                                    logger.warn("Could not parse amount received from refund reason: {}", refundReason);
                                    invoiceData.put("amountReceived", 0.0);
                                    invoiceData.put("changeReturned", 0.0);
                                }
                            } else {
                                invoiceData.put("amountReceived", 0.0);
                                invoiceData.put("changeReturned", 0.0);
                            }

                            // Parse Notes
                            if (refundReason.contains("Notes:")) {
                                try {
                                    String notesPart = refundReason.substring(refundReason.indexOf("Notes:") + 6).trim();
                                    invoiceData.put("notes", notesPart);
                                    logger.info("Successfully parsed notes: {}", notesPart);
                                } catch (Exception e) {
                                    logger.warn("Could not parse notes from refund reason: {}", refundReason);
                                    invoiceData.put("notes", "Payment completed successfully");
                                }
                            } else {
                                // Nếu không có "Notes:" prefix, coi toàn bộ refundReason là notes
                                if (!refundReason.contains("Amount Received:")) {
                                    invoiceData.put("notes", refundReason);
                                    logger.info("Using entire refundReason as notes: {}", refundReason);
                                } else {
                                    invoiceData.put("notes", "Payment completed successfully");
                                }
                            }
                        } else {
                            invoiceData.put("amountReceived", 0.0);
                            invoiceData.put("changeReturned", 0.0);
                            invoiceData.put("notes", "Payment completed successfully");
                        }
                    } else {
                        // Calculate total from services
                        double calculatedTotal = calculateTotalFromServices(servicesUsed);
                        invoiceData.put("transactionId", "N/A");
                        invoiceData.put("transactionStatus", "Pending");
                        invoiceData.put("paymentMethod", "N/A");
                        invoiceData.put("receiptId", "N/A");
                        invoiceData.put("receiptNumber", "PENDING");
                        invoiceData.put("totalAmount", calculatedTotal);
                        invoiceData.put("taxAmount", calculatedTotal * 0.1); // 10% tax
                        invoiceData.put("discountAmount", 0.0);
                        invoiceData.put("issuedDate", LocalDate.now().toString());
                        // Add default payment fields
                        invoiceData.put("amountReceived", 0.0);
                        invoiceData.put("changeReturned", 0.0);
                        invoiceData.put("notes", "Payment pending");
                        invoiceData.put("timeOfPayment", "");
                    }
                } else {
                    // Calculate total from services
                    double calculatedTotal = calculateTotalFromServices(servicesUsed);
                    invoiceData.put("transactionId", "N/A");
                    invoiceData.put("transactionStatus", "Pending");
                    invoiceData.put("paymentMethod", "N/A");
                    invoiceData.put("receiptId", "N/A");
                    invoiceData.put("receiptNumber", "PENDING");
                    invoiceData.put("totalAmount", calculatedTotal);
                    invoiceData.put("taxAmount", calculatedTotal * 0.1); // 10% tax
                    invoiceData.put("discountAmount", 0.0);
                    invoiceData.put("issuedDate", LocalDate.now().toString());
                    // Add default payment fields
                    invoiceData.put("amountReceived", 0.0);
                    invoiceData.put("changeReturned", 0.0);
                    invoiceData.put("notes", "No appointment found");
                    invoiceData.put("timeOfPayment", "");
                }
            } catch (Exception e) {
                logger.error("Error fetching transaction data for patient {}: {}", patientId, e.getMessage());
                // Calculate total from services for fallback
                double calculatedTotal = calculateTotalFromServices(servicesUsed);
                invoiceData.put("transactionId", "N/A");
                invoiceData.put("transactionStatus", "Pending");
                invoiceData.put("paymentMethod", "N/A");
                invoiceData.put("receiptId", "N/A");
                invoiceData.put("receiptNumber", "PENDING");
                invoiceData.put("totalAmount", calculatedTotal);
                invoiceData.put("taxAmount", calculatedTotal * 0.1); // 10% tax
                invoiceData.put("discountAmount", 0.0);
                invoiceData.put("issuedDate", LocalDate.now().toString());
                // Add default payment fields
                invoiceData.put("amountReceived", 0.0);
                invoiceData.put("changeReturned", 0.0);
                invoiceData.put("notes", "Error retrieving payment details");
                invoiceData.put("timeOfPayment", "");
            }

            logger.info("=== Final Invoice Data ===");
            logger.info("Full Name in invoice: {}", invoiceData.get("fullName"));
            logger.info("Payer Name in invoice: {}", invoiceData.get("payerName"));
            logger.info("Amount Received: {}", invoiceData.get("amountReceived"));
            logger.info("Change Returned: {}", invoiceData.get("changeReturned"));
            logger.info("Notes: {}", invoiceData.get("notes"));
            logger.info("Time of Payment: {}", invoiceData.get("timeOfPayment"));
            logger.info("Invoice data prepared for patient ID {}: {}", patientId, invoiceData);
            return invoiceData;

        } catch (Exception e) {
            logger.error("Error getting invoice data for patient ID {}: {}", patientId, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Get services used by patient based on appointment and patient history
     * Service ID: lấy theo ServiceID trong bảng Service
     * Specialization -> Symptom: lấy theo ServiceName trong bảng Service
     * Service Name: lấy theo ServiceName trong bảng Service
     * Quantity: count (tổng số lần sử dụng 1 Service)
     * Price: lấy theo Price trong bảng Service
     * Tax (%): 10%
     * Total (VND): Quantity * Price + Tax (10%)
     * Date: l���y theo ngày hiện tại
     */
    private List<Map<String, Object>> getServicesUsedByPatient(Integer patientId, Appointment appointment) {
        List<Map<String, Object>> servicesUsed = new ArrayList<>();

        try {
            // Lấy dữ liệu từ bảng Service thông qua specialization của appointment
            if (appointment != null && appointment.getDoctor() != null &&
                    appointment.getDoctor().getSpecializations() != null &&
                    !appointment.getDoctor().getSpecializations().isEmpty()) {

                // Lấy specialization đầu tiên của doctor
                Specialization specialization = appointment.getDoctor().getSpecializations().iterator().next();

                // Kiểm tra null để tránh NullPointerException
                if (specialization != null) {
                    Map<String, Object> service = new HashMap<>();
                    service.put("serviceId", "SRV" + String.format("%03d", specialization.getSpecId() != null ? specialization.getSpecId() : 1));
                    service.put("symptom", specialization.getSymptom() != null ? specialization.getSymptom() :
                               (specialization.getSpecName() != null ? specialization.getSpecName() : "General Medicine"));
                    service.put("serviceName", "General Consultation - " + (specialization.getSpecName() != null ? specialization.getSpecName() : "General"));
                    service.put("quantity", 1); // Mặc định 1 lần sử dụng

                    // Lấy price từ specialization price hoặc giá mặc định với null check
                    double price = 300000.0; // Giá mặc định
                    if (specialization.getPrice() != null) {
                        price = specialization.getPrice().doubleValue();
                    }
                    service.put("price", price);
                    service.put("taxPercent", 10); // 10% tax

                    // Tính total: Quantity * Price + Tax
                    double baseAmount = 1 * price; // quantity * price
                    double taxAmount = baseAmount * 0.1; // 10% tax
                    double total = baseAmount + taxAmount;
                    service.put("total", total);
                    service.put("date", LocalDate.now().toString());

                    servicesUsed.add(service);

                    // Thêm các dịch vụ bổ sung nếu cần với null check
                    String specName = specialization.getSpecName();
                    if (specName != null && (specName.toLowerCase().contains("internal") ||
                            specName.toLowerCase().contains("general"))) {
                        addAdditionalServices(servicesUsed, patientId);
                    }
                } else {
                    // Nếu specialization null, tạo dịch vụ mặc định
                    addDefaultServices(servicesUsed, patientId);
                }
            } else {
                // Nếu không có appointment hoặc specialization, tạo dịch vụ mặc định
                addDefaultServices(servicesUsed, patientId);
            }

        } catch (Exception e) {
            logger.error("Error getting services for patient {}: {}", patientId, e.getMessage(), e);
            // Fallback: tạo dịch vụ mặc định
            addDefaultServices(servicesUsed, patientId);
        }

        return servicesUsed;
    }

    /**
     * Add additional services based on patient history or common medical procedures
     */
    private void addAdditionalServices(List<Map<String, Object>> servicesUsed, Integer patientId) {
        // Blood Test
        Map<String, Object> bloodTest = new HashMap<>();
        bloodTest.put("serviceId", "SRV002");
        bloodTest.put("symptom", "Health Screening");
        bloodTest.put("serviceName", "Blood Test");
        bloodTest.put("quantity", 1);
        bloodTest.put("price", 500000.0);
        bloodTest.put("taxPercent", 10);
        double bloodTestTotal = 500000.0 * 1.1; // price + 10% tax
        bloodTest.put("total", bloodTestTotal);
        bloodTest.put("date", LocalDate.now().toString());
        servicesUsed.add(bloodTest);

        // Medication
        Map<String, Object> medication = new HashMap<>();
        medication.put("serviceId", "SRV003");
        medication.put("symptom", "Treatment");
        medication.put("serviceName", "Medication");
        medication.put("quantity", 2);
        medication.put("price", 100000.0);
        medication.put("taxPercent", 10);
        double medicationTotal = 100000.0 * 2 * 1.1; // quantity * price + 10% tax
        medication.put("total", medicationTotal);
        medication.put("date", LocalDate.now().toString());
        servicesUsed.add(medication);
    }

    /**
     * Add default services when no specific data is available
     */
    private void addDefaultServices(List<Map<String, Object>> servicesUsed, Integer patientId) {
        // General Consultation
        Map<String, Object> consultation = new HashMap<>();
        consultation.put("serviceId", "SRV001");
        consultation.put("symptom", "General Medicine");
        consultation.put("serviceName", "General Consultation");
        consultation.put("quantity", 1);
        consultation.put("price", 300000.0);
        consultation.put("taxPercent", 10);
        double consultationTotal = 300000.0 * 1.1; // price + 10% tax
        consultation.put("total", consultationTotal);
        consultation.put("date", LocalDate.now().toString());
        servicesUsed.add(consultation);
    }

    /**
     * Calculate total amount from services with proper null and type checking
     */
    private double calculateTotalFromServices(List<Map<String, Object>> servicesUsed) {
        if (servicesUsed == null || servicesUsed.isEmpty()) {
            return 0.0;
        }

        return servicesUsed.stream()
                .mapToDouble(service -> {
                    Object total = service.get("total");
                    if (total == null) {
                        return 0.0;
                    }
                    if (total instanceof Number) {
                        return ((Number) total).doubleValue();
                    }
                    try {
                        return Double.parseDouble(total.toString());
                    } catch (NumberFormatException e) {
                        logger.warn("Invalid total value in service: {}", total);
                        return 0.0;
                    }
                })
                .sum();
    }

    /**
     * Get staff member with roleID = 3 (Receptionist) for "Processed By" field
     */
    public Map<String, Object> getReceptionistStaffInfo() {
        try {
            logger.info("=== Starting getReceptionistStaffInfo ===");
            // Find a receptionist user with roleID = 3
            List<Users> receptionists = userRepository.findByRoleId(3);
            logger.info("Found {} users with roleID = 3", receptionists.size());

            if (!receptionists.isEmpty()) {
                Users receptionist = receptionists.get(0); // Get first receptionist
                logger.info("Selected receptionist - UserID: {}, FullName: {}, Email: {}",
                    receptionist.getUserId(), receptionist.getFullName(), receptionist.getEmail());

                Map<String, Object> staffInfo = new HashMap<>();
                staffInfo.put("userId", receptionist.getUserId());
                staffInfo.put("fullName", receptionist.getFullName());
                staffInfo.put("email", receptionist.getEmail());

                logger.info("Returning staffInfo: {}", staffInfo);
                return staffInfo;
            }

            // Fallback if no receptionist found
            logger.warn("No receptionist found with roleID = 3, using default staff info");
            Map<String, Object> defaultStaff = new HashMap<>();
            defaultStaff.put("userId", "STAFF001");
            defaultStaff.put("fullName", "Staff Member");
            defaultStaff.put("email", "staff@medicareplus.com");
            return defaultStaff;

        } catch (Exception e) {
            logger.error("Error getting receptionist staff info: {}", e.getMessage(), e);
            // Return default staff info
            Map<String, Object> defaultStaff = new HashMap<>();
            defaultStaff.put("userId", "STAFF001");
            defaultStaff.put("fullName", "Staff Member");
            defaultStaff.put("email", "staff@medicareplus.com");
            return defaultStaff;
        }
    }

    /**
     * Get a receptionist user (role = 3) to process the transaction
     */
    private Integer getReceptionistUserId() {
        try {
            List<Users> receptionists = userRepository.findByRoleId(3);
            if (receptionists.isEmpty()) {
                logger.warn("No receptionist found with role = 3");
                return null;
            }

            // Get the first active receptionist
            Users receptionist = receptionists.get(0);
            logger.info("Selected receptionist {} for processing transaction", receptionist.getUserId());
            return receptionist.getUserId();

        } catch (Exception e) {
            logger.error("Error getting receptionist user: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Process payment and update EXISTING transaction status to "Paid"
     * ONLY UPDATES existing Transaction - does NOT create new records
     * Updated logic to strictly find by AppointmentID and UserID
     */
    @Transactional
    public Map<String, Object> processPayment(
            Integer patientId,
            Integer appointmentId,
            String transactionIdStr,
            String receiptIdStr,
            String method,
            Double totalAmount,
            Double amountReceived,
            String notes) {

        try {
            logger.info("=== Processing Payment - UPDATE EXISTING TRANSACTION ONLY ===");
            logger.info("PatientId: {}, AppointmentId: {}, Method: {}, TotalAmount: {}",
                       patientId, appointmentId, method, totalAmount);

            // STEP 1: Validate AppointmentID and UserID exist in database
            if (!validateAppointmentAndPatient(appointmentId, patientId)) {
                throw new RuntimeException("Invalid AppointmentID or PatientID - not found in database");
            }

            // STEP 2: FIND EXISTING TRANSACTION BY BOTH AppointmentID AND UserID - STRICT MATCHING
            Transaction transaction = null;

            // Primary search: Find by BOTH AppointmentID AND UserID for security
            List<Transaction> candidateTransactions = transactionRepository.findByAppointmentId(appointmentId);
            if (!candidateTransactions.isEmpty()) {
                // Filter by UserID to ensure exact match
                Optional<Transaction> matchingTransaction = candidateTransactions.stream()
                    .filter(t -> t.getUserId() != null && t.getUserId().equals(patientId))
                    .findFirst();

                if (matchingTransaction.isPresent()) {
                    transaction = matchingTransaction.get();
                    logger.info("Found existing transaction by AppointmentID={} AND UserID={}: TransactionID={}",
                               appointmentId, patientId, transaction.getTransactionId());
                } else {
                    logger.error("No transaction found matching AppointmentID={} AND UserID={}", appointmentId, patientId);
                    throw new RuntimeException("No transaction found for AppointmentID: " + appointmentId + " and UserID: " + patientId);
                }
            } else {
                logger.error("No transactions found for AppointmentID: {}", appointmentId);
                throw new RuntimeException("No existing transaction found for AppointmentID: " + appointmentId);
            }

            // STEP 3: Validate transaction is not already paid to prevent duplicate processing
            if ("Paid".equalsIgnoreCase(transaction.getStatus())) {
                logger.warn("Transaction {} is already paid", transaction.getTransactionId());
                return buildPaymentResult(transaction, "Already Paid");
            }

            logger.info("UPDATING existing transaction {} from {} to Paid",
                       transaction.getTransactionId(), transaction.getStatus());

            // STEP 4: UPDATE ONLY the required fields - NEVER change AppointmentId or UserId
            String oldStatus = transaction.getStatus();
            transaction.setStatus("Paid");
            transaction.setMethod(method);
            transaction.setTimeOfPayment(java.time.LocalDateTime.now());

            // Set ProcessedByUserID to current receptionist user
            Integer receptionistUserId = getReceptionistUserId();
            if (receptionistUserId != null) {
                transaction.setProcessedByUserId(receptionistUserId);
                logger.info("Set ProcessedByUserID to receptionist: {}", receptionistUserId);
            }

            // Store payment details in RefundReason field
            String paymentDetails = buildPaymentDetails(method, amountReceived, notes);
            transaction.setRefundReason(paymentDetails);

            // STEP 5: SAVE UPDATED transaction (UPDATE operation, not INSERT)
            Transaction savedTransaction = transactionRepository.save(transaction);
            logger.info("✅ UPDATED Transaction {} - Status: {} -> {}, Method: {}, ProcessedBy: {}",
                       savedTransaction.getTransactionId(), oldStatus, savedTransaction.getStatus(),
                       savedTransaction.getMethod(), savedTransaction.getProcessedByUserId());

            // STEP 6: Handle Receipt - find existing or create new
            Receipt receipt = findOrCreateReceipt(receiptIdStr, savedTransaction, patientId, totalAmount, notes);

            // STEP 7: Update Appointment status if needed
            updateAppointmentStatusIfNeeded(appointmentId);

            logger.info("Payment processing completed - UPDATED Transaction: {}, Receipt: {}",
                       savedTransaction.getTransactionId(), receipt.getReceiptId());

            return buildPaymentResult(savedTransaction, "Paid");

        } catch (Exception e) {
            logger.error("Error processing payment: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process payment: " + e.getMessage());
        }
    }

    /**
     * Validate that AppointmentID exists and belongs to the specified PatientID
     */
    private boolean validateAppointmentAndPatient(Integer appointmentId, Integer patientId) {
        try {
            logger.info("Validating appointmentId: {} and patientId: {}", appointmentId, patientId);

            if (appointmentId == null || patientId == null) {
                logger.error("AppointmentID or PatientID is null");
                return false;
            }

            Optional<Appointment> appointmentOpt = appointmentRepository.findById(appointmentId);
            if (appointmentOpt.isEmpty()) {
                logger.error("Appointment {} not found in database", appointmentId);
                return false;
            }

            Appointment appointment = appointmentOpt.get();

            // Check if patient exists and is not null
            if (appointment.getPatient() == null) {
                logger.error("Appointment {} has no associated patient", appointmentId);
                return false;
            }

            // Check if patient ID matches
            if (appointment.getPatient().getPatientId() == null) {
                logger.error("Appointment {} patient has null PatientID", appointmentId);
                return false;
            }

            if (!appointment.getPatient().getPatientId().equals(patientId)) {
                logger.error("Appointment {} does not belong to patient {}. Found patient: {}",
                           appointmentId, patientId, appointment.getPatient().getPatientId());
                return false;
            }

            logger.info("Validation passed - Appointment {} belongs to patient {}", appointmentId, patientId);
            return true;

        } catch (Exception e) {
            logger.error("Error validating appointment and patient: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Build payment details string for storage - CẢI THIỆN ĐỂ LƯU CHÍNH XÁC
     */
    private String buildPaymentDetails(String method, Double amountReceived, String notes) {
        StringBuilder details = new StringBuilder();

        try {
            // Luôn lưu amount received cho tất cả payment methods, không chỉ Cash
            if (amountReceived != null && amountReceived > 0) {
                details.append(String.format("Amount Received: $%.2f", amountReceived));
                logger.info("Storing amount received: ${}", amountReceived);
            }

            // Luôn lưu notes nếu có
            if (notes != null && !notes.trim().isEmpty()) {
                if (details.length() > 0) {
                    details.append(" | ");
                }
                details.append("Notes: ").append(notes.trim());
                logger.info("Storing notes: {}", notes.trim());
            }

            String finalDetails = details.toString();
            logger.info("Final payment details string: {}", finalDetails);
            return finalDetails;

        } catch (Exception e) {
            logger.warn("Error building payment details: {}", e.getMessage());
            return notes != null ? notes : "";
        }
    }

    /**
     * Find existing receipt or create new one with proper TransactionID
     */
    private Receipt findOrCreateReceipt(String receiptIdStr, Transaction savedTransaction,
                                      Integer patientId, Double totalAmount, String notes) {
        Receipt receipt = null;

        try {
            // Try to find existing receipt first
            if (receiptIdStr != null && !receiptIdStr.equals("N/A") && !receiptIdStr.equals("null")) {
                try {
                    Integer receiptId = Integer.parseInt(receiptIdStr);
                    Optional<Receipt> existingReceipt = receiptRepository.findById(receiptId);
                    if (existingReceipt.isPresent()) {
                        receipt = existingReceipt.get();
                        logger.info("Found existing receipt: {}", receiptId);
                    }
                } catch (NumberFormatException e) {
                    logger.warn("Invalid receipt ID format: {}", receiptIdStr);
                }
            }

            // Create new receipt if not found
            if (receipt == null) {
                receipt = new Receipt();
                receipt.setIssuerId(patientId != null ? patientId : 1); // Default to 1 if null
                receipt.setIssuedDate(java.time.LocalDate.now());
                receipt.setReceiptNumber("REC" + System.currentTimeMillis());
                logger.info("Creating new receipt for patient: {}", patientId);
            }

            // Validate savedTransaction is not null
            if (savedTransaction == null || savedTransaction.getTransactionId() == null) {
                throw new RuntimeException("SavedTransaction or TransactionID is null");
            }

            // Set TransactionID and update receipt details
            receipt.setTransactionId(savedTransaction.getTransactionId());
            receipt.setTotalAmount(java.math.BigDecimal.valueOf(totalAmount != null ? totalAmount : 0.0));
            receipt.setTaxAmount(java.math.BigDecimal.valueOf((totalAmount != null ? totalAmount : 0.0) * 0.1));
            receipt.setDiscountAmount(java.math.BigDecimal.valueOf(0));
            receipt.setNotes(notes != null ? notes : "");
            receipt.setFormat("Digital");

            // Save receipt
            Receipt savedReceipt = receiptRepository.save(receipt);
            logger.info("Receipt saved with ID: {} and TransactionID: {}",
                       savedReceipt.getReceiptId(), savedReceipt.getTransactionId());

            return savedReceipt;

        } catch (Exception e) {
            logger.error("Error finding or creating receipt: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create receipt: " + e.getMessage());
        }
    }

    /**
     * Update appointment status to Completed if payment is successful
     */
    private void updateAppointmentStatusIfNeeded(Integer appointmentId) {
        try {
            if (appointmentId == null) {
                logger.warn("AppointmentID is null, cannot update status");
                return;
            }

            Optional<Appointment> appointmentOpt = appointmentRepository.findById(appointmentId);
            if (appointmentOpt.isPresent()) {
                Appointment appointment = appointmentOpt.get();
                String currentStatus = appointment.getStatus();

                if ("Scheduled".equals(currentStatus) || "Pending".equals(currentStatus)) {
                    appointment.setStatus("Completed");
                    appointmentRepository.save(appointment);
                    logger.info("Updated appointment {} status from {} to Completed", appointmentId, currentStatus);
                } else {
                    logger.info("Appointment {} status is already {}, no update needed", appointmentId, currentStatus);
                }
            } else {
                logger.warn("Appointment {} not found for status update", appointmentId);
            }
        } catch (Exception e) {
            logger.warn("Could not update appointment status for {}: {}", appointmentId, e.getMessage());
        }
    }

    /**
     * Build standardized payment result
     */
    private Map<String, Object> buildPaymentResult(Transaction transaction, String status) {
        Map<String, Object> result = new HashMap<>();

        try {
            if (transaction == null) {
                throw new RuntimeException("Transaction is null");
            }

            result.put("transactionId", transaction.getTransactionId());
            result.put("status", status != null ? status : "Unknown");
            result.put("method", transaction.getMethod() != null ? transaction.getMethod() : "Unknown");
            result.put("timeOfPayment", transaction.getTimeOfPayment() != null ?
                      transaction.getTimeOfPayment().toString() : java.time.LocalDateTime.now().toString());
            result.put("appointmentId", transaction.getAppointmentId());
            result.put("userId", transaction.getUserId());

            // Add receipt info if available
            if (transaction.getReceipt() != null) {
                result.put("receiptId", transaction.getReceipt().getReceiptId());
                if (transaction.getReceipt().getTotalAmount() != null) {
                    result.put("totalAmount", transaction.getReceipt().getTotalAmount());
                }
            }

            return result;

        } catch (Exception e) {
            logger.error("Error building payment result: {}", e.getMessage(), e);
            // Return minimal result if error occurs
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("transactionId", transaction != null ? transaction.getTransactionId() : null);
            errorResult.put("status", "Error");
            errorResult.put("error", e.getMessage());
            return errorResult;
        }
    }

    // Phương thức để tìm Receptionist theo UserId
    public Receptionist findByUserId(Integer userId) {
        return receptionistEntityRepository.findByUserId(userId).orElse(null);
    }

    // Phương thức để lưu Receptionist
    public Receptionist save(Receptionist receptionist) {
        return receptionistEntityRepository.save(receptionist);
    }

    // Phương thức để tìm Receptionist theo ID
    public Optional<Receptionist> findById(Integer receptionistId) {
        return receptionistEntityRepository.findById(receptionistId);
    }
}
