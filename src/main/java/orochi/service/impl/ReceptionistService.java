package orochi.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import orochi.model.Appointment;
import orochi.model.Patient;
import orochi.model.PatientContact;
import orochi.model.Users;
import orochi.model.Schedule;
import orochi.repository.*;
import orochi.model.*;
import orochi.service.ScheduleService;

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

    // Currency constants - chỉ sử dụng VND
    private static final String CURRENCY_CODE_VND = "VND";
    private static final String CURRENCY_SYMBOL_VND = "₫";

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

    @Autowired
    private MedicalServiceRepository medicalServiceRepository;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private ScheduleService scheduleService;

    public ReceptionistService(
            UserRepository userRepository,
            AppointmentRepository appointmentRepository,
            PatientRepository patientRepository,
            ReceptionistRepository receptionistRepository,
            ReceptionistEntityRepository receptionistEntityRepository,
            PatientContactRepository patientContactRepository,
            DoctorRepository doctorRepository,
            ReceiptRepository receiptRepository,
            RoomRepository roomRepository,
            ScheduleService scheduleService) {
        this.userRepository = userRepository;
        this.appointmentRepository = appointmentRepository;
        this.patientRepository = patientRepository;
        this.receptionistRepository = receptionistRepository;
        this.receptionistEntityRepository = receptionistEntityRepository;
        this.patientContactRepository = patientContactRepository;
        this.DoctorRepository = doctorRepository;
        this.receiptRepository = receiptRepository;
        this.roomRepository = roomRepository;
        this.scheduleService = scheduleService;
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

            // 🔥 VALIDATE EMAIL AND PHONE UNIQUENESS FIRST
            logger.info("=== VALIDATING EMAIL AND PHONE UNIQUENESS ===");
            logger.info("Checking email: {}", email);
            logger.info("Checking phone: {}", phoneNumber);

            // Check if email already exists
            Optional<Users> existingUserByEmail = userRepository.findByEmail(email);
            if (existingUserByEmail.isPresent()) {
                logger.error("Email already exists: {}", email);
                throw new RuntimeException("Email already exists");
            }

            // Check if phone number already exists
            Optional<Users> existingUserByPhone = userRepository.findByPhoneNumber(phoneNumber);
            if (existingUserByPhone.isPresent()) {
                logger.error("Phone number already exists: {}", phoneNumber);
                throw new RuntimeException("Phone number already exists");
            }

            logger.info("✅ Email and phone number are unique, proceeding with registration");

            // Parse date of birth
            java.time.LocalDate dateOfBirth;
            try {
                dateOfBirth = java.time.LocalDate.parse(dateOfBirthStr);
            } catch (Exception e) {
                logger.error("Invalid date format: {}", dateOfBirthStr);
                throw new RuntimeException("Invalid date format. Please use YYYY-MM-DD format.");
            }

            // Create Users entity
            Users newUser = new Users();
            newUser.setFullName(fullName);
            newUser.setEmail(email.toLowerCase().trim()); // Normalize email
            newUser.setPhoneNumber(phoneNumber.trim());
            newUser.setPasswordHash(passwordHash); // In production, this should be encrypted
            newUser.setRoleId(4); // Patient role ID = 4
            newUser.setGuest(false);
            newUser.setStatus("Active");
            newUser.setCreatedAt(java.time.LocalDateTime.now());

            // 🔥 SAVE USERS FIRST TO GET UserID
            logger.info("=== SAVING USERS ENTITY ===");
            Users savedUser = userRepository.save(newUser);
            logger.info("✅ Users saved with ID: {}", savedUser.getUserId());

            // 🔥 CHECK IF PATIENT RECORD ALREADY EXISTS (double check)
            Optional<Patient> existingPatient = patientRepository.findByUserId(savedUser.getUserId());
            Patient savedPatient;

            if (existingPatient.isPresent()) {
                // Update existing patient instead of creating new one
                logger.info("Found existing patient record, updating...");
                savedPatient = existingPatient.get();
                savedPatient.setDateOfBirth(dateOfBirth);
                savedPatient.setGender(gender);
                savedPatient.setDescription(description != null ? description : "");
                // 🔥 ENSURE USER RELATIONSHIP IS SET
                savedPatient.setUser(savedUser);
                savedPatient = patientRepository.save(savedPatient);
                logger.info("✅ Updated existing patient record with ID: {}", savedPatient.getPatientId());
            } else {
                // 🔥 CREATE NEW PATIENT ENTITY WITH PROPER RELATIONSHIP
                logger.info("Creating new patient record...");
                Patient newPatient = new Patient();
                newPatient.setUserId(savedUser.getUserId()); // Set foreign key
                newPatient.setDateOfBirth(dateOfBirth);
                newPatient.setGender(gender);
                newPatient.setDescription(description != null ? description : "");
                // 🔥 CRUCIAL: Set the bidirectional relationship
                newPatient.setUser(savedUser);

                // Save Patient
                savedPatient = patientRepository.save(newPatient);
                logger.info("✅ Created new patient record with ID: {}", savedPatient.getPatientId());
            }

            // 🔥 ESTABLISH BIDIRECTIONAL RELATIONSHIP EXPLICITLY
            logger.info("=== ESTABLISHING BIDIRECTIONAL RELATIONSHIP ===");
            savedUser.setPatient(savedPatient); // Set patient relationship in user
            savedPatient.setUser(savedUser);     // Set user relationship in patient (redundant but safe)

            // 🔥 SAVE BOTH ENTITIES AGAIN TO ENSURE RELATIONSHIP IS PERSISTED
            savedUser = userRepository.save(savedUser);
            savedPatient = patientRepository.save(savedPatient);

            logger.info("✅ Bidirectional relationship established and persisted");

            // Create PatientContact entity for address information
            logger.info("=== CREATING PATIENT CONTACT ===");
            PatientContact patientContact = new PatientContact();
            patientContact.setPatientId(savedPatient.getPatientId());
            patientContact.setAddressType(addressType != null && !addressType.trim().isEmpty() ? addressType : "Home");
            patientContact.setStreetAddress(streetAddress != null ? streetAddress : "");

            // 🔥 NORMALIZE VIETNAMESE ADDRESS DATA BEFORE SAVING TO DATABASE
            logger.info("=== NORMALIZING VIETNAMESE ADDRESS DATA ===");
            logger.info("Original city: {}", city);
            logger.info("Original country: {}", country);

            // Normalize Vietnamese province/city names with proper diacritics
            String normalizedCity = normalizeVietnameseCity(city);
            String normalizedCountry = normalizeVietnameseCountry(country);

            logger.info("Normalized city: {}", normalizedCity);
            logger.info("Normalized country: {}", normalizedCountry);

            patientContact.setCity(normalizedCity);
            patientContact.setCountry(normalizedCountry);
            // Set default values for missing fields if needed
            patientContact.setState(""); // Default empty state
            patientContact.setPostalCode(""); // Default empty postal code

            // Save PatientContact
            PatientContact savedContact = patientContactRepository.save(patientContact);
            logger.info("✅ PatientContact saved with ID: {}", savedContact.getContactId());

            // 🔥 FINAL VALIDATION - VERIFY RELATIONSHIPS ARE WORKING
            logger.info("=== FINAL RELATIONSHIP VALIDATION ===");
            logger.info("SavedUser ID: {}, FullName: {}, Email: {}, Phone: {}",
                       savedUser.getUserId(), savedUser.getFullName(), savedUser.getEmail(), savedUser.getPhoneNumber());
            logger.info("SavedPatient ID: {}, UserID: {}, Gender: {}, DOB: {}",
                       savedPatient.getPatientId(), savedPatient.getUserId(), savedPatient.getGender(), savedPatient.getDateOfBirth());

            // Test the relationship
            if (savedPatient.getUser() != null) {
                logger.info("✅ Patient.User relationship: WORKING - User ID: {}, FullName: {}",
                           savedPatient.getUser().getUserId(), savedPatient.getUser().getFullName());
            } else {
                logger.error("❌ Patient.User relationship: BROKEN - User is NULL");
                throw new RuntimeException("Failed to establish Patient-User relationship");
            }

            if (savedUser.getPatient() != null) {
                logger.info("✅ User.Patient relationship: WORKING - Patient ID: {}",
                           savedUser.getPatient().getPatientId());
            } else {
                logger.warn("⚠️ User.Patient relationship: NOT SET (this might be normal for lazy loading)");
            }

            logger.info("=== PATIENT REGISTRATION COMPLETED SUCCESSFULLY ===");
            return savedUser;

        } catch (Exception e) {
            // Log the error with full stack trace
            logger.error("❌ Error registering new patient: {}", e.getMessage(), e);

            // Re-throw with more specific error message
            if (e.getMessage().contains("Email already exists")) {
                throw new RuntimeException("Email already exists");
            } else if (e.getMessage().contains("Phone number already exists")) {
                throw new RuntimeException("Phone number already exists");
            } else if (e.getMessage().contains("Invalid date format")) {
                throw new RuntimeException("Invalid date format. Please use YYYY-MM-DD format.");
            } else {
                throw new RuntimeException("Failed to register patient: " + e.getMessage());
            }
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

            String oldStatus = appointment.getStatus();
            appointment.setStatus(status);
            appointmentRepository.save(appointment);

            // 🔥 LOGIC TẠO TRANSACTION KHI CONFIRM APPOINTMENT (Pending -> Scheduled)
            if ("Scheduled".equals(status) && "Pending".equals(oldStatus)) {
                logger.info("=== TRIGGERING TRANSACTION CREATION ===");
                logger.info("Status changed from {} to {} for AppointmentID: {}", oldStatus, status, appointmentId);
                createTransactionForConfirmedAppointment(appointment);

                // 🔥 NEW: CREATE SCHEDULE WHEN APPOINTMENT IS CONFIRMED
                logger.info("=== TRIGGERING SCHEDULE CREATION ===");
                createScheduleForConfirmedAppointment(appointment);
            }

            return true;
        }
        return false;
    }

    /**
     * Tạo Transaction record khi confirm appointment
     * Logic theo yêu cầu:
     * - [AppointmentID] từ bảng Appointment
     * - [UserID] check theo [PatientID] ở bảng Patient
     * - [TimeOfPayment] lấy theo thời gian thực
     * - [Status] set mặc ������������������������������ định là Pending
     */
    @Transactional
    protected void createTransactionForConfirmedAppointment(Appointment appointment) {
        try {
            logger.info("=== CREATING TRANSACTION FOR CONFIRMED APPOINTMENT ===");
            logger.info("Processing AppointmentID: {}, PatientID: {}",
                       appointment.getAppointmentId(), appointment.getPatientId());

            // BƯỚC 1: Lấy UserID từ bảng Patient theo PatientID
            Optional<Patient> patientOptional = patientRepository.findById(appointment.getPatientId());
            if (patientOptional.isEmpty()) {
                logger.error("❌ Patient not found for PatientID: {}", appointment.getPatientId());
                return;
            }

            Patient patient = patientOptional.get();
            if (patient.getUser() == null) {
                logger.error("❌ User not found for PatientID: {}", appointment.getPatientId());
                return;
            }

            Integer userId = patient.getUser().getUserId();
            logger.info("✅ Found UserID: {} for PatientID: {}", userId, appointment.getPatientId());

            // BƯỚC 2: Kiểm tra xem đã c�� Transaction cho appointment này chưa (tránh duplicate)
            List<Transaction> existingTransactions = transactionRepository.findByAppointmentId(appointment.getAppointmentId());
            if (!existingTransactions.isEmpty()) {
                logger.info("⚠��� Transaction already exists for AppointmentID: {}, skipping creation",
                           appointment.getAppointmentId());
                return;
            }

            // BƯỚC 3: Tạo Transaction mới theo yêu cầu
            Transaction transaction = new Transaction();

            // [AppointmentID] từ bảng Appointment
            transaction.setAppointmentId(appointment.getAppointmentId());

            // [UserID] check theo [PatientID] ở bảng Patient
            transaction.setUserId(userId);

            // [TimeOfPayment] lấy theo thời gian thực
            transaction.setTimeOfPayment(java.time.LocalDateTime.now());

            // [Status] set mặc định là Pending
            transaction.setStatus("Pending");

            // 🔥 SỬA LỖI: Set Method với giá trị hợp lệ thay vì "Pending"
            // Method phải là payment method thực tế, không phải status
            transaction.setMethod("Cash"); // Default payment method cho transaction được tạo khi confirm

            // BƯỚC 4: Đảm bảo tất cả field NOT NULL được set
            // RefundReason có thể null nên không cần set
            // ProcessedByUserID có thể null khi tạo mới (s��� set khi process payment)

            // BƯỚC 5: Lưu Transaction vào database với proper error handling
            logger.info("🔥 ATTEMPTING TO SAVE TRANSACTION TO DATABASE...");
            logger.info("Transaction data before save:");
            logger.info("   AppointmentID: {}", transaction.getAppointmentId());
            logger.info("   UserID: {}", transaction.getUserId());
            logger.info("   Method: {}", transaction.getMethod());
            logger.info("   Status: {}", transaction.getStatus());
            logger.info("   TimeOfPayment: {}", transaction.getTimeOfPayment());

            Transaction savedTransaction = transactionRepository.save(transaction);

            logger.info("✅ SUCCESSFULLY SAVED Transaction to database:");
            logger.info("   TransactionID: {}", savedTransaction.getTransactionId());
            logger.info("   AppointmentID: {}", savedTransaction.getAppointmentId());
            logger.info("   UserID: {}", savedTransaction.getUserId());
            logger.info("   Method: {}", savedTransaction.getMethod());
            logger.info("   TimeOfPayment: {}", savedTransaction.getTimeOfPayment());
            logger.info("   Status: {}", savedTransaction.getStatus());

            // BƯỚC 6: Verify transaction was actually saved by querying back
            List<Transaction> verifyTransactions = transactionRepository.findByAppointmentId(appointment.getAppointmentId());
            logger.info("🔍 VERIFICATION: Found {} transactions for AppointmentID: {}",
                       verifyTransactions.size(), appointment.getAppointmentId());

            if (!verifyTransactions.isEmpty()) {
                Transaction verifiedTransaction = verifyTransactions.get(0);
                logger.info("✅ VERIFIED: Transaction {} exists in database with Status: {}",
                           verifiedTransaction.getTransactionId(), verifiedTransaction.getStatus());
            } else {
                logger.error("❌ VERIFICATION FAILED: No transactions found after save operation");
            }

        } catch (Exception e) {
            logger.error("❌ Error creating transaction for confirmed appointment: {}", e.getMessage(), e);
            logger.error("❌ Full stack trace:", e);
            // Không throw exception để không ảnh hưởng đến việc confirm appointment
            // Nhưng log chi tiết để debug
        }
    }

    /**
     * Create schedule when appointment is confirmed
     * Schedule will have:
     * - DoctorID from appointment
     * - RoomID from appointment
     * - PatientID from appointment
     * - AppointmentID from appointment
     * - ScheduleDate from appointment DateTime (date part)
     * - StartTime from appointment DateTime (time part)
     * - EndTime = StartTime + 30 minutes
     * - EventType = "Appointment"
     * - Description from appointment description
     * - IsCompleted = false (default)
     */
    @Transactional
    protected void createScheduleForConfirmedAppointment(Appointment appointment) {
        try {
            logger.info("=== CREATING SCHEDULE FOR CONFIRMED APPOINTMENT ===");
            logger.info("Processing AppointmentID: {}, PatientID: {}",
                       appointment.getAppointmentId(), appointment.getPatientId());

            // BƯỚC 1: Check if schedule already exists for this appointment
            Optional<Schedule> existingSchedule = scheduleRepository.findByAppointmentId(appointment.getAppointmentId());
            if (existingSchedule.isPresent()) {
                logger.info("⚠️ Schedule already exists for AppointmentID: {}, skipping creation",
                           appointment.getAppointmentId());
                return;
            }

            // BƯỚC 2: Validate required data from appointment
            if (appointment.getDoctor() == null) {
                logger.error("❌ Doctor not found for AppointmentID: {}", appointment.getAppointmentId());
                return;
            }

            if (appointment.getPatient() == null) {
                logger.error("❌ Patient not found for AppointmentID: {}", appointment.getAppointmentId());
                return;
            }

            // BƯỚC 3: Create new Schedule entity
            Schedule schedule = new Schedule();

            // Set IDs from appointment
            schedule.setDoctorId(appointment.getDoctor().getDoctorId());
            schedule.setPatientId(appointment.getPatient().getPatientId());
            schedule.setAppointmentId(appointment.getAppointmentId());

            // Set Room ID if available
            if (appointment.getRoom() != null) {
                schedule.setRoomId(appointment.getRoom().getRoomId());
            } else {
                // Try to get a room from the doctor's specialty
                try {
                    if (!appointment.getDoctor().getSpecializations().isEmpty()) {
                        Integer specialtyId = appointment.getDoctor().getSpecializations().get(0).getSpecId();
                        List<Map<String, Object>> rooms = getRoomsBySpecialtyAndDoctor(specialtyId, appointment.getDoctor().getDoctorId());
                        if (!rooms.isEmpty()) {
                            Object roomId = rooms.get(0).get("roomId");
                            if (roomId != null) {
                                schedule.setRoomId((Integer) roomId);
                                logger.info("✅ Assigned room {} from specialty {} to schedule", roomId, specialtyId);
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.warn("Could not assign room automatically: {}", e.getMessage());
                }
            }

            // Set date and time from appointment
            schedule.setScheduleDate(appointment.getDateTime().toLocalDate());
            schedule.setStartTime(appointment.getDateTime().toLocalTime());

            // Set end time = start time + 30 minutes
            LocalTime startTime = appointment.getDateTime().toLocalTime();
            LocalTime endTime = startTime.plusMinutes(30);
            schedule.setEndTime(endTime);

            // Set event type as "Appointment"
            schedule.setEventType("Appointment");

            // Set description from appointment
            String description = appointment.getDescription();
            if (description == null || description.trim().isEmpty()) {
                description = "Medical consultation appointment";
            }
            schedule.setDescription(description);

            // Set as not completed initially
            schedule.setIsCompleted(false);

            // BƯỚC 4: Save schedule to database
            Schedule savedSchedule = scheduleRepository.save(schedule);

            logger.info("✅ SCHEDULE CREATED SUCCESSFULLY:");
            logger.info("   ScheduleID: {}", savedSchedule.getScheduleId());
            logger.info("   DoctorID: {}", savedSchedule.getDoctorId());
            logger.info("   PatientID: {}", savedSchedule.getPatientId());
            logger.info("   RoomID: {}", savedSchedule.getRoomId());
            logger.info("   AppointmentID: {}", savedSchedule.getAppointmentId());
            logger.info("   ScheduleDate: {}", savedSchedule.getScheduleDate());
            logger.info("   StartTime: {}", savedSchedule.getStartTime());
            logger.info("   EndTime: {}", savedSchedule.getEndTime());
            logger.info("   EventType: {}", savedSchedule.getEventType());
            logger.info("   Description: {}", savedSchedule.getDescription());
            logger.info("   IsCompleted: {}", savedSchedule.getIsCompleted());

            // BƯỚC 5: Verify schedule was saved correctly
            Optional<Schedule> verifySchedule = scheduleRepository.findByAppointmentId(appointment.getAppointmentId());
            if (verifySchedule.isPresent()) {
                logger.info("✅ VERIFIED: Schedule {} exists in database for AppointmentID: {}",
                           verifySchedule.get().getScheduleId(), appointment.getAppointmentId());
            } else {
                logger.error("❌ VERIFICATION FAILED: No schedule found after save operation");
            }

        } catch (Exception e) {
            logger.error("❌ Error creating schedule for confirmed appointment: {}", e.getMessage(), e);
            logger.error("❌ Full stack trace:", e);
            // Don't throw exception to avoid affecting appointment confirmation
            // But log detailed error for debugging
        }
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

                    // Calculate age from dateOfBirth
                    if (appointment.getPatient().getDateOfBirth() != null) {
                        LocalDate today = LocalDate.now();
                        LocalDate birthDate = appointment.getPatient().getDateOfBirth();
                        int age = today.getYear() - birthDate.getYear();
                        if (today.getDayOfYear() < birthDate.getDayOfYear()) {
                            age--;
                        }
                        appointmentData.put("age", age);
                    } else {
                        appointmentData.put("age", "N/A");
                    }

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

                        // Calculate age from dateOfBirth
                        if (appointment.getPatient().getDateOfBirth() != null) {
                            LocalDate today = LocalDate.now();
                            LocalDate birthDate = appointment.getPatient().getDateOfBirth();
                            int age = today.getYear() - birthDate.getYear();
                            if (today.getDayOfYear() < birthDate.getDayOfYear()) {
                                age--;
                            }
                            appointmentData.put("age", age);
                        } else {
                            appointmentData.put("age", "N/A");
                        }

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

                    // ✅ Bio Description - Lấy từ Doctor.BioDescription
                    String bioDescription = "Not specified";
                    if (doctor.getBioDescription() != null && !doctor.getBioDescription().trim().isEmpty()) {
                        bioDescription = doctor.getBioDescription();
                    }
                    doctorMap.put("bio", bioDescription);
                    doctorMap.put("bioDescription", bioDescription); // Alias for consistency

                    // ✅ Education Information - Lấy Degree v�� Institution từ Education table
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
                    doctorMap.put("degree", degree);
                    doctorMap.put("institution", institution);
                    doctorMap.put("description", description);

                    // ✅ Room Information - Lấy từ Schedule hoặc Department
                    String roomInfo = "Not assigned";
                    try {
                        // Method 1: Get room from doctor's schedules
                        if (doctor.getSchedules() != null && !doctor.getSchedules().isEmpty()) {
                            Set<String> rooms = doctor.getSchedules().stream()
                                    .filter(schedule -> schedule.getRoom() != null)
                                    .map(schedule -> schedule.getRoom().getRoomNumber())
                                    .collect(Collectors.toSet());

                            if (!rooms.isEmpty()) {
                                roomInfo = String.join(", ", rooms);
                            }
                        }

                        // Method 2: If no room from schedule, get rooms by specialty
                        if ("Not assigned".equals(roomInfo) && doctor.getSpecializations() != null && !doctor.getSpecializations().isEmpty()) {
                            Integer specialtyId = doctor.getSpecializations().get(0).getSpecId();
                            Integer currentDoctorId = doctor.getDoctorId();

                            List<Map<String, Object>> rooms = getRoomsBySpecialtyAndDoctor(specialtyId, currentDoctorId);
                            if (!rooms.isEmpty()) {
                                Set<String> roomNumbers = rooms.stream()
                                        .map(room -> (String) room.get("roomNumber"))
                                        .filter(Objects::nonNull)
                                        .limit(2) // Limit to first 2 rooms
                                        .collect(Collectors.toSet());

                                if (!roomNumbers.isEmpty()) {
                                    roomInfo = String.join(", ", roomNumbers);
                                }
                            }
                        }
                    } catch (Exception e) {
                        logger.warn("Error getting room info for doctor {}: {}", doctor.getDoctorId(), e.getMessage());
                        roomInfo = "Not assigned";
                    }

                    doctorMap.put("room", roomInfo);

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

            // ✅ Bio Description - Lấy từ Doctor.BioDescription
            String bioDescription = "Not specified";
            if (doctor.getBioDescription() != null && !doctor.getBioDescription().trim().isEmpty()) {
                bioDescription = doctor.getBioDescription();
            }
            doctorDetails.put("bio", bioDescription);
            doctorDetails.put("bioDescription", bioDescription); // Alias for consistency

            // ✅ Education Information - Lấy Degree và Institution từ Education table
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

            // ✅ Room Information - Lấy từ Schedule hoặc Department (tương tự getAllDoctorsWithDetails)
            String roomInfo = "Not assigned";
            try {
                // Method 1: Get room from doctor's schedules
                if (doctor.getSchedules() != null && !doctor.getSchedules().isEmpty()) {
                    Set<String> rooms = doctor.getSchedules().stream()
                            .filter(schedule -> schedule.getRoom() != null)
                            .map(schedule -> schedule.getRoom().getRoomNumber())
                            .collect(Collectors.toSet());

                    if (!rooms.isEmpty()) {
                        roomInfo = String.join(", ", rooms);
                    }
                }

                // Method 2: If no room from schedule, get rooms by specialty
                if ("Not assigned".equals(roomInfo) && doctor.getSpecializations() != null && !doctor.getSpecializations().isEmpty()) {
                    Integer specialtyId = doctor.getSpecializations().get(0).getSpecId();
                    Integer currentDoctorId = doctor.getDoctorId();

                    List<Map<String, Object>> rooms = getRoomsBySpecialtyAndDoctor(specialtyId, currentDoctorId);
                    if (!rooms.isEmpty()) {
                        Set<String> roomNumbers = rooms.stream()
                                .map(room -> (String) room.get("roomNumber"))
                                .filter(Objects::nonNull)
                                .limit(2) // Limit to first 2 rooms
                                .collect(Collectors.toSet());

                        if (!roomNumbers.isEmpty()) {
                            roomInfo = String.join(", ", roomNumbers);
                        }
                    }
                }
            } catch (Exception e) {
                logger.warn("Error getting room info for doctor {}: {}", doctor.getDoctorId(), e.getMessage());
                roomInfo = "Not assigned";
            }

            doctorDetails.put("room", roomInfo);

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

                // Amount từ Receipt.TotalAmount - hiển thị số nguy��n
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
            // Th��� l��y dữ liệu thực từ Receipt và Transaction trước
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
                    logger.error("Error calculating revenue for appointment {}: {}", e.getMessage(), e);
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
     * Room được lấy từ RoomNumber trong bảng Room và check theo DepartmentID và HeadDoctorID và DoctorID, SpecID FROM DoctorSpecialization
     */
    public List<Map<String, Object>> getRoomsBySpecialtyAndDoctor(Integer specialtyId, Integer doctorId) {
        try {
            logger.info("=== GETTING ROOMS BY SPECIALTY AND DOCTOR ===");
            logger.info("SpecialtyID: {}, DoctorID: {}", specialtyId, doctorId);

            // Validate input parameters
            if (specialtyId == null || doctorId == null) {
                logger.warn("SpecialtyID or DoctorID is null");
                return new ArrayList<>();
            }

            // First validate that the doctor has the required specialty
            if (!validateDoctorSpecialty(doctorId, specialtyId)) {
                logger.warn("Doctor {} does not have specialty {}", doctorId, specialtyId);
                return new ArrayList<>();
            }

            List<Map<String, Object>> rooms = new ArrayList<>();

            // Use the new repository method that queries based on DepartmentID, HeadDoctorID, DoctorID, and SpecID
            List<Object[]> roomResults = roomRepository.findRoomsByDoctorSpecialtyAndDepartment(doctorId, specialtyId);

            logger.info("Found {} rooms from primary query", roomResults.size());

            // Process results from the primary query
            for (Object[] result : roomResults) {
                Map<String, Object> roomData = new HashMap<>();

                // Extract data from Object[] result
                // Query returns: RoomID, RoomNumber, RoomName, Type, Capacity, Status, DepartmentName, DepartmentID, DoctorID, DoctorName, SpecializationName, SpecID
                roomData.put("roomId", result[0]);
                roomData.put("roomNumber", result[1]);
                roomData.put("roomName", result[2]);
                roomData.put("type", result[3]);
                roomData.put("capacity", result[4]);
                roomData.put("status", result[5]);
                roomData.put("departmentName", result[6]);
                roomData.put("departmentId", result[7]);
                roomData.put("doctorId", result[8]);
                roomData.put("doctorName", result[9]);
                roomData.put("specializationName", result[10]);
                roomData.put("specId", result[11]);

                rooms.add(roomData);

                logger.info("Added room: {} - {} (Dept: {}, Doctor: {})",
                    result[1], result[2], result[6], result[9]);
            }

            // If no rooms found from primary query, try alternative method
            if (rooms.isEmpty()) {
                logger.info("No rooms found from primary query, trying alternative method...");

                List<Object[]> alternativeResults = roomRepository.findRoomsByDoctorAndSpecialtyWithDepartment(doctorId, specialtyId);
                logger.info("Found {} rooms from alternative query", alternativeResults.size());

                for (Object[] result : alternativeResults) {
                    Map<String, Object> roomData = new HashMap<>();

                    // Same structure as above
                    roomData.put("roomId", result[0]);
                    roomData.put("roomNumber", result[1]);
                    roomData.put("roomName", result[2]);
                    roomData.put("type", result[3]);
                    roomData.put("capacity", result[4]);
                    roomData.put("status", result[5]);
                    roomData.put("departmentName", result[6]);
                    roomData.put("departmentId", result[7]);
                    roomData.put("doctorId", result[8]);
                    roomData.put("doctorName", result[9]);
                    roomData.put("specializationName", result[10]);
                    roomData.put("specId", result[11]);

                    rooms.add(roomData);

                    logger.info("Added room (alternative): {} - {} (Dept: {}, Doctor: {})",
                        result[1], result[2], result[6], result[9]);
                }
            }

            // If still no rooms found, fall back to available rooms in departments matching the specialty
            if (rooms.isEmpty()) {
                logger.info("No rooms found from queries, using fallback method...");

                // Get doctor information for fallback
                Optional<orochi.model.Doctor> doctorOptional = DoctorRepository.findById(doctorId);
                if (doctorOptional.isPresent()) {
                    orochi.model.Doctor doctor = doctorOptional.get();

                    // Get specialty name for matching
                    String specializationName = doctor.getSpecializations().stream()
                            .filter(spec -> spec.getSpecId().equals(specialtyId))
                            .map(Specialization::getSpecName)
                            .findFirst()
                            .orElse("Unknown");

                    // Get all available rooms and filter by department matching specialty
                    List<Room> availableRooms = roomRepository.findAllAvailableRooms();
                    logger.info("Found {} available rooms for fallback", availableRooms.size());

                    for (Room room : availableRooms) {
                        if (room.getDepartment() != null) {
                            String deptName = room.getDepartment().getDeptName();

                            // Check if department name matches or contains the specialization
                            if (deptName.toLowerCase().contains(specializationName.toLowerCase()) ||
                                    specializationName.toLowerCase().contains(deptName.toLowerCase())) {

                                Map<String, Object> roomData = new HashMap<>();
                                roomData.put("roomId", room.getRoomId());
                                roomData.put("roomNumber", room.getRoomNumber());
                                roomData.put("roomName", room.getRoomName());
                                roomData.put("type", room.getType());
                                roomData.put("capacity", room.getCapacity());
                                roomData.put("status", room.getStatus());
                                roomData.put("departmentName", deptName);
                                roomData.put("departmentId", room.getDepartment().getDepartmentId());
                                roomData.put("doctorId", doctorId);
                                roomData.put("doctorName", doctor.getUser().getFullName());
                                roomData.put("specializationName", specializationName);
                                roomData.put("specId", specialtyId);

                                rooms.add(roomData);

                                logger.info("Added room (fallback): {} - {} (Dept: {})",
                                    room.getRoomNumber(), room.getRoomName(), deptName);
                            }
                        }
                    }
                }
            }

            // Sort by room number
            rooms.sort((r1, r2) -> {
                String room1 = (String) r1.get("roomNumber");
                String room2 = (String) r2.get("roomNumber");
                return room1.compareTo(room2);
            });

            logger.info("=== FINAL RESULTS ===");
            logger.info("Returning {} rooms for SpecialtyID: {}, DoctorID: {}", rooms.size(), specialtyId, doctorId);

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

                // DateTime từ Appointment table - format đ��� hiển thị
                Object dateTime = payment.get("dateTime");
                if (dateTime != null) {
                    processedPayment.put("dateTime", dateTime.toString());
                } else {
                    processedPayment.put("dateTime", "");
                }

                // Status từ Transaction table (chỉ Status = 'Paid')
                processedPayment.put("status", payment.get("status"));

                // Method t��� Transaction table
                Object method = payment.get("method");
                processedPayment.put("method", method != null ? method : "Unknown");

                // Amount từ Receipt.TotalAmount - format với $ và số
                Object amount = payment.get("amount");
                if (amount != null) {
                    try {
                        double amountValue = Double.parseDouble(amount.toString());
                        processedPayment.put("amount", String.format("%.0f", amountValue));
                    } catch (NumberFormatException e) {
                        processedPayment.put("amount", "0");
                    }
                } else {
                    processedPayment.put("amount", "0");
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
                        processedPayment.put("amount", String.format("%.0f", amountValue));
                    } catch (NumberFormatException e) {
                        processedPayment.put("amount", "0");
                    }
                } else {
                    processedPayment.put("amount", "0");
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
                        processedPayment.put("amount", String.format("%.0f", amountValue));
                    } catch (NumberFormatException e) {
                        processedPayment.put("amount", "0");
                    }
                } else {
                    processedPayment.put("amount", "0");
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
     * Full Name: lấy theo FullName trong b��ng User
     * Date of Birth: lấy theo dateOfBirth trong bảng Patient
     * Gender: lấy theo Gender trong bảng Patient
     * Appointment ID: lấy theo Patient ID trong b��ng Patient
     * Payer Name: lấy theo AppointmentID trong bảng Appointment
     * Contact: l��y theo PhoneNumber trong bảng User
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

            // T���o dữ liệu invoice
            Map<String, Object> invoiceData = new HashMap<>();

            // Patient Information - Đảm bảo lấy ��úng FullName t��� Users table
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

            // Service Information - Lấy dữ liệu từ b����ng Service
            List<Map<String, Object>> servicesUsed = getServicesUsedByPatient(patientId, latestAppointment);
            invoiceData.put("servicesUsed", servicesUsed);

            // Payment/Transaction Information
            if (latestAppointment != null) {
                try {
                    List<Transaction> transactions = transactionRepository.findByAppointmentId(latestAppointment.getAppointmentId());
                    if (!transactions.isEmpty()) {
                        Transaction latestTransaction = transactions.get(0);
                        invoiceData.put("transactionId", latestTransaction.getTransactionId());
                        invoiceData.put("transactionStatus", latestTransaction.getStatus());
                        invoiceData.put("paymentMethod", latestTransaction.getMethod());

                        // Generate receipt number based on transaction ID
                        invoiceData.put("receiptNumber", "REC" + latestTransaction.getTransactionId());

                        // IMPROVED: Calculate amount from service prices with fallback logic
                        double finalAmount = 0.0;
                        try {
                            // Primary: Calculate from services used
                            double calculatedFromServices = calculateTotalFromServices(servicesUsed);
                            if (calculatedFromServices > 0) {
                                finalAmount = calculatedFromServices;
                                logger.info("Amount calculated from services: {}", finalAmount);
                            } else {
                                // Fallback: Use receipt amount if service calculation fails
                                if (latestTransaction.getReceipt() != null &&
                                    latestTransaction.getReceipt().getTotalAmount() != null) {
                                    finalAmount = latestTransaction.getReceipt().getTotalAmount().doubleValue();
                                    logger.info("Fallback to receipt amount: {}", finalAmount);
                                } else {
                                    finalAmount = 0.0;
                                    logger.warn("No amount available from services or receipt");
                                }
                            }
                        } catch (Exception e) {
                            logger.error("Error calculating amount from services, using receipt fallback: {}", e.getMessage());
                            // Fallback: Use receipt amount if service calculation throws exception
                            if (latestTransaction.getReceipt() != null &&
                                latestTransaction.getReceipt().getTotalAmount() != null) {
                                finalAmount = latestTransaction.getReceipt().getTotalAmount().doubleValue();
                                logger.info("Exception fallback to receipt amount: {}", finalAmount);
                            } else {
                                finalAmount = 0.0;
                                logger.warn("No fallback amount available");
                            }
                        }

                        invoiceData.put("totalAmount", finalAmount);
                        invoiceData.put("taxAmount", finalAmount * 0.1); // 10% tax
                        invoiceData.put("discountAmount", 0.0);
                        invoiceData.put("issuedDate", LocalDate.now().toString());

                        // Add receipt information
                        if (latestTransaction.getReceipt() != null) {
                            invoiceData.put("receiptId", latestTransaction.getReceipt().getReceiptId());
                        } else {
                            invoiceData.put("receiptId", "N/A");
                        }

                        // Thêm thông tin payment details từ transaction
                        invoiceData.put("timeOfPayment", latestTransaction.getTimeOfPayment() != null ?
                               latestTransaction.getTimeOfPayment().toString() : "");

                        // 🔥 THÊM refundReason vào invoiceData để template có thể truy cập
                        invoiceData.put("refundReason", latestTransaction.getRefundReason());

                        // Parse payment details from refundReason field - UPDATED FOR VND CURRENCY
                        String refundReason = latestTransaction.getRefundReason();
                        logger.info("=== Parsing refundReason ===");
                        logger.info("Raw refundReason: {}", refundReason);

                        if (refundReason != null && !refundReason.trim().isEmpty()) {
                            // Parse Amount Received - Updated to handle both $ and VND formats
                            if (refundReason.contains("Amount Received:")) {
                                try {
                                    String amountPart;
                                    // Handle both "Amount Received: $" and "Amount Received: " (VND) formats
                                    if (refundReason.contains("Amount Received: $")) {
                                        amountPart = refundReason.substring(refundReason.indexOf("Amount Received: $") + 18);
                                    } else {
                                        amountPart = refundReason.substring(refundReason.indexOf("Amount Received:") + 16).trim();
                                    }

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

                                    // Calculate change using the final calculated amount
                                    double change = amountReceived - finalAmount;
                                    invoiceData.put("changeReturned", change > 0 ? change : 0.0);
                                    logger.info("Calculated change: {} (amountReceived: {} - total: {})", change, amountReceived, finalAmount);

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
                        invoiceData.put("notes", "No appointment found");
                        invoiceData.put("timeOfPayment", "");
                    }
                } catch (Exception e) {
                    logger.error("Error fetching transaction data for patient {}: {}", patientId, e.getMessage());
                    // IMPROVED: Calculate total from services for fallback with proper error handling
                    double calculatedTotal = 0.0;
                    try {
                        calculatedTotal = calculateTotalFromServices(servicesUsed);
                        logger.info("Fallback amount calculation successful: {}", calculatedTotal);
                    } catch (Exception serviceError) {
                        logger.error("Error in fallback amount calculation: {}", serviceError.getMessage());
                        calculatedTotal = 0.0;
                    }

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
            } else {
                // IMPROVED: Calculate total from services with error handling when no appointment
                double calculatedTotal = 0.0;
                try {
                    calculatedTotal = calculateTotalFromServices(servicesUsed);
                    logger.info("No appointment - calculated total from services: {}", calculatedTotal);
                } catch (Exception e) {
                    logger.error("Error calculating total from services (no appointment): {}", e.getMessage());
                    calculatedTotal = 0.0;
                }

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

            logger.info("=== Final Invoice Data ===");
            logger.info("Full Name in invoice: {}", invoiceData.get("fullName"));

            return invoiceData;

        } catch (Exception e) {
            logger.error("Error fetching invoice data for patient {}: {}", patientId, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Get receptionist staff information for current user
     * Used for displaying "Processed By" information in payment details
     */
    public Map<String, Object> getReceptionistStaffInfo() {
        try {
            // For now, return default staff info
            // In a real implementation, this would get the current logged-in user's information
            Map<String, Object> staffInfo = new HashMap<>();
            staffInfo.put("fullName", "Receptionist Staff");
            staffInfo.put("userId", 1);
            staffInfo.put("staffId", "REC001");
            staffInfo.put("department", "Reception");
            staffInfo.put("position", "Receptionist");

            logger.info("Generated staff info: {}", staffInfo);
            return staffInfo;
        } catch (Exception e) {
            logger.error("Error getting receptionist staff info: {}", e.getMessage(), e);
            // Return default values if error occurs
            Map<String, Object> defaultStaffInfo = new HashMap<>();
            defaultStaffInfo.put("fullName", "Unknown Staff");
            defaultStaffInfo.put("userId", 0);
            defaultStaffInfo.put("staffId", "N/A");
            defaultStaffInfo.put("department", "Reception");
            defaultStaffInfo.put("position", "Receptionist");
            return defaultStaffInfo;
        }
    }

    /**
     * Find a Receptionist by User ID
     * Used for avatar functionality
     */
    public Receptionist findByUserId(Integer userId) {
        return receptionistEntityRepository.findByUserId(userId).orElse(null);
    }

    /**
     * Save a Receptionist entity
     * Used for avatar functionality
     */
    public Receptionist save(Receptionist receptionist) {
        return receptionistEntityRepository.save(receptionist);
    }

    /**
     * Get services used by a patient for a specific appointment
     * Fetches REAL data from Service and Specialization tables according to user requirements:
     * - Service ID: lấy theo [ServiceID] trong bảng Service
     * - Service Name: lấy theo [ServiceName] trong bảng Service
     * - Symptom: lấy theo Symptom trong bảng Specialization
     * - Quantity: lấy theo count Service Name trong bảng Service
     * - Price: lấy theo [Price] trong bảng Service
     */
    private List<Map<String, Object>> getServicesUsedByPatient(Integer patientId, Appointment appointment) {
        List<Map<String, Object>> services = new ArrayList<>();

        try {
            logger.info("=== GETTING REAL SERVICES DATA FROM DATABASE ===");
            logger.info("PatientID: {}, AppointmentID: {}", patientId,
                       appointment != null ? appointment.getAppointmentId() : "NULL");

            List<Map<String, Object>> rawServiceData = null;

            if (appointment != null && appointment.getDoctor() != null &&
                !appointment.getDoctor().getSpecializations().isEmpty()) {

                // Get the first specialization of the doctor
                Specialization specialization = appointment.getDoctor().getSpecializations().get(0);
                Integer specId = specialization.getSpecId();

                logger.info("Doctor has specialization: {} (SpecID: {})",
                           specialization.getSpecName(), specId);

                // Get services with specialization data from database
                rawServiceData = medicalServiceRepository.getServicesWithSpecializationBySpecId(specId);
                logger.info("Found {} services for SpecID: {}", rawServiceData.size(), specId);

            } else {
                logger.info("No doctor/specialization found, getting default services");
                // Get default general consultation service
                rawServiceData = medicalServiceRepository.getDefaultGeneralConsultationService();

                if (rawServiceData.isEmpty()) {
                    // Fallback to all services if no default found
                    rawServiceData = medicalServiceRepository.getAllServicesWithSpecialization();
                    logger.info("No default service found, got {} services from all services", rawServiceData.size());
                }
            }

            // Process the raw service data according to user requirements
            if (!rawServiceData.isEmpty()) {
                for (Map<String, Object> rawService : rawServiceData) {
                    Map<String, Object> serviceData = new HashMap<>();

                    // Service ID: lấy theo [ServiceID] trong bảng Service
                    Object serviceId = rawService.get("serviceId");
                    serviceData.put("serviceId", serviceId != null ? serviceId.toString() : "SRV001");

                    // Service Name: lấy theo [ServiceName] trong bảng Service
                    String serviceName = (String) rawService.get("serviceName");
                    serviceData.put("serviceName", serviceName != null ? serviceName : "General Consultation");

                    // Symptom: lấy theo Symptom trong bảng Specialization
                    String symptom = (String) rawService.get("symptom");
                    serviceData.put("symptom", symptom != null ? symptom : "General Medicine");

                    // Quantity: lấy theo count Service Name trong bảng Service
                    // For now, default to 1, but we can implement actual count logic
                    Long serviceCount = medicalServiceRepository.countByServiceName(serviceName);
                    serviceData.put("quantity", serviceCount != null && serviceCount > 0 ? serviceCount.intValue() : 1);

                    // 🔥 ENHANCED: Price lấy trực tiếp từ [Price] trong bảng Service với validation
                    Object priceObj = rawService.get("price");
                    Double servicePrice = null;

                    logger.info("=== PROCESSING SERVICE PRICE FROM DATABASE ===");
                    logger.info("ServiceID: {}, ServiceName: {}", serviceId, serviceName);
                    logger.info("Raw price from database: {} (Type: {})", priceObj, priceObj != null ? priceObj.getClass().getSimpleName() : "NULL");

                    if (priceObj != null) {
                        try {
                            if (priceObj instanceof Number) {
                                servicePrice = ((Number) priceObj).doubleValue();
                                logger.info("✅ Price converted from Number: {} VND", servicePrice);
                            } else if (priceObj instanceof String) {
                                servicePrice = Double.parseDouble(priceObj.toString());
                                logger.info("✅ Price parsed from String: {} VND", servicePrice);
                            } else {
                                servicePrice = Double.parseDouble(priceObj.toString());
                                logger.info("✅ Price parsed from Object.toString(): {} VND", servicePrice);
                            }

                            // Validate price is positive
                            if (servicePrice <= 0) {
                                logger.warn("⚠️ Invalid price <= 0: {}, using default", servicePrice);
                                servicePrice = 500000.0; // Default 500,000 VND
                            }
                        } catch (NumberFormatException e) {
                            logger.error("❌ Could not parse price: {}, using default. Error: {}", priceObj, e.getMessage());
                            servicePrice = 500000.0; // Default 500,000 VND for consultation
                        }
                    } else {
                        logger.warn("⚠️ Price is NULL in database for ServiceID: {}, using default", serviceId);
                        servicePrice = 500000.0; // Default 500,000 VND
                    }

                    serviceData.put("price", servicePrice);
                    logger.info("Final service price set: {} VND", servicePrice);

                    // Tax percentage (standard 10%)
                    serviceData.put("taxPercent", 10);

                    // 🔥 ENHANCED: Total calculation with proper VND amounts
                    Integer quantity = (Integer) serviceData.get("quantity");
                    Double subtotal = servicePrice * quantity;
                    Double taxAmount = subtotal * 0.1; // 10% tax
                    Double total = subtotal + taxAmount; // Total = Price * Quantity + Tax

                    serviceData.put("subtotal", subtotal);
                    serviceData.put("taxAmount", taxAmount);
                    serviceData.put("total", total);

                    logger.info("=== SERVICE AMOUNT CALCULATION ===");
                    logger.info("Price: {} VND x Quantity: {} = Subtotal: {} VND", servicePrice, quantity, subtotal);
                    logger.info("Tax (10%): {} VND", taxAmount);
                    logger.info("Final Total: {} VND", total);

                    // Date - appointment date or current date
                    String serviceDate = appointment != null ?
                        appointment.getDateTime().toLocalDate().toString() :
                        java.time.LocalDate.now().toString();
                    serviceData.put("date", serviceDate);

                    services.add(serviceData);

                    logger.info("✅ Processed service: ID={}, Name={}, Price={} VND, Quantity={}, Total={} VND",
                               serviceId, serviceName, servicePrice, quantity, total);
                }
            }

            // If no services found from database, add a fallback default service
            if (services.isEmpty()) {
                logger.warn("No services found in database, adding fallback default service");
                Map<String, Object> defaultService = createDefaultService(appointment);
                services.add(defaultService);
            }

            logger.info("=== FINAL SERVICES DATA ===");
            logger.info("Total services returned: {}", services.size());

        } catch (Exception e) {
            logger.error("Error fetching services from database for patient {}: {}", patientId, e.getMessage(), e);

            // Fallback to default service on error
            logger.info("Adding fallback service due to error");
            Map<String, Object> defaultService = createDefaultService(appointment);
            services.add(defaultService);
        }

        return services;
    }

    /**
     * Create a default service when no services are found in database
     */
    private Map<String, Object> createDefaultService(Appointment appointment) {
        Map<String, Object> defaultService = new HashMap<>();

        // Default values according to user requirements
        defaultService.put("serviceId", "SRV001");
        defaultService.put("serviceName", "General Consultation");
        defaultService.put("symptom", "General Medicine");
        defaultService.put("quantity", 1);
        defaultService.put("price", 100.0);
        defaultService.put("taxPercent", 10);
        defaultService.put("total", 110.0); // 100 + 10% tax

        String serviceDate = appointment != null ?
            appointment.getDateTime().toLocalDate().toString() :
            java.time.LocalDate.now().toString();
        defaultService.put("date", serviceDate);

        logger.info("Created default service with total: {}", defaultService.get("total"));
        return defaultService;
    }

    /**
     * Helper method to get services by specialization - now uses real database data
     */
    private List<MedicalService> getServicesBySpecialization(Integer specId) {
        try {
            if (specId != null) {
                List<MedicalService> services = medicalServiceRepository.findBySpecId(specId);
                logger.info("Found {} MedicalService entities for SpecID: {}", services.size(), specId);
                return services;
            }
        } catch (Exception e) {
            logger.error("Error fetching MedicalService by SpecID {}: {}", specId, e.getMessage());
        }
        return new ArrayList<>();
    }

    /**
     * Calculate total from services for financial calculations
     */
    private double calculateTotalFromServices(List<Map<String, Object>> servicesUsed) {
        if (servicesUsed == null || servicesUsed.isEmpty()) {
            return 0.0;
        }

        double total = 0.0;
        for (Map<String, Object> service : servicesUsed) {
            Object totalObj = service.get("total");
            if (totalObj != null) {
                try {
                    if (totalObj instanceof Number) {
                        total += ((Number) totalObj).doubleValue();
                    } else {
                        total += Double.parseDouble(totalObj.toString());
                    }
                } catch (NumberFormatException e) {
                    logger.warn("Could not parse service total: {}", totalObj);
                }
            }
        }
        return total;
    }

    /**
     * Process payment for a patient appointment - FIXED: Prevents duplicate transactions
     * Chỉ sử dụng VND - không cần chuyển đổi tiền tệ
     */
    @Transactional
    public Map<String, Object> processPayment(Integer patientId, Integer appointmentId, String transactionIdStr,
                                            String receiptIdStr, String method, Double totalAmount,
                                            Double amountReceived, String notes, Integer issuerId) {
        try {
            logger.info("=== PROCESSING PAYMENT (FIXED DUPLICATE PREVENTION) ===");
            logger.info("PatientId: {}, AppointmentId: {}, Method: {}, Amount: {} VND",
                       patientId, appointmentId, method, totalAmount);

            // 🔥 STEP 1: Get patient and verify userId
            Optional<Patient> patientOptional = patientRepository.findById(patientId);
            if (patientOptional.isEmpty()) {
                throw new RuntimeException("Patient not found with ID: " + patientId);
            }
            Patient patient = patientOptional.get();
            Integer userId = patient.getUser().getUserId();

            logger.info("🔍 Looking for existing transaction: AppointmentID={}, UserID={}", appointmentId, userId);

            // 🔥 STEP 2: Find existing transaction with PRECISE matching
            Transaction transaction = null;

            // Method 1: Try to find by exact AppointmentID and UserID combination
            List<Transaction> appointmentTransactions = transactionRepository.findByAppointmentId(appointmentId);
            logger.info("📋 Found {} transactions for appointmentId: {}", appointmentTransactions.size(), appointmentId);

            for (Transaction t : appointmentTransactions) {
                logger.info("   - TransactionID: {}, UserID: {}, Status: {}",
                           t.getTransactionId(), t.getUserId(), t.getStatus());

                if (t.getUserId() != null && t.getUserId().equals(userId)) {
                    transaction = t;
                    logger.info("✅ FOUND MATCHING TRANSACTION: TransactionID={}, UserID={}, Status={}",
                               t.getTransactionId(), t.getUserId(), t.getStatus());
                    break;
                }
            }

            // 🔥 STEP 3: Only create new transaction if absolutely no existing transaction found
            if (transaction == null) {
                logger.warn("⚠️ NO EXISTING TRANSACTION FOUND - This should not happen in normal flow");
                logger.warn("   AppointmentID: {}, UserID: {}", appointmentId, userId);
                logger.warn("   Creating emergency transaction...");

                // Emergency transaction creation - should rarely happen
                transaction = new Transaction();
                transaction.setAppointmentId(appointmentId);
                transaction.setUserId(userId);
                transaction.setTimeOfPayment(java.time.LocalDateTime.now());
                transaction.setStatus("Pending");
                transaction.setMethod("Cash"); // Default method

                // Save emergency transaction
                transaction = transactionRepository.save(transaction);
                logger.info("🚨 EMERGENCY: Created new transaction with ID: {}", transaction.getTransactionId());
            } else {
                logger.info("🔄 UPDATING EXISTING TRANSACTION: {}", transaction.getTransactionId());
            }

            // 🔥 STEP 4: Prevent duplicate if already paid
            if ("Paid".equalsIgnoreCase(transaction.getStatus())) {
                logger.warn("⚠️ TRANSACTION {} ALREADY PAID - Preventing duplicate", transaction.getTransactionId());

                // Return existing transaction info instead of creating duplicate
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("transactionId", transaction.getTransactionId());
                response.put("status", "Paid");
                response.put("method", transaction.getMethod());
                response.put("message", "Transaction already paid - no duplicate created");
                response.put("totalAmount", totalAmount);
                response.put("currency", CURRENCY_CODE_VND);

                // Try to find existing receipt
                Receipt existingReceipt = transaction.getReceipt();
                if (existingReceipt != null) {
                    response.put("receiptId", existingReceipt.getReceiptId());
                }

                return response;
            }

            // 🔥 STEP 5: Update transaction status and details
            String oldStatus = transaction.getStatus();
            transaction.setStatus("Paid");
            transaction.setMethod(method);
            transaction.setProcessedByUserId(issuerId);
            transaction.setTimeOfPayment(java.time.LocalDateTime.now());

            // Store payment details in refundReason field for cash payments
            if ("Cash".equals(method) && amountReceived != null) {
                String paymentDetails = String.format("Amount Received: %.0f VND | Notes: %s",
                                                     amountReceived, notes != null ? notes : "Payment completed successfully");
                transaction.setRefundReason(paymentDetails);
            } else {
                transaction.setRefundReason(notes != null ? notes : "Payment completed successfully");
            }

            // Save updated transaction
            Transaction savedTransaction = transactionRepository.save(transaction);
            logger.info("✅ TRANSACTION {} UPDATED: Status='{}' → 'Paid', Method='{}'",
                       savedTransaction.getTransactionId(), oldStatus, method);

            // 🔥 STEP 6: Create or update receipt
            Receipt receipt = transaction.getReceipt();
            boolean isNewReceipt = (receipt == null);

            if (receipt == null) {
                receipt = new Receipt();
                receipt.setTransactionId(savedTransaction.getTransactionId());
                receipt.setIssuedDate(java.time.LocalDate.now());
                receipt.setIssuerId(issuerId);

                // Set required fields that cannot be NULL
                receipt.setReceiptNumber("REC" + savedTransaction.getTransactionId() + "_" + System.currentTimeMillis());
                receipt.setFormat("Digital"); // Valid values: 'Digital', 'Print', 'Both'
                receipt.setPdfPath(""); // Empty string instead of NULL

                // Calculate tax amount (10% of total amount)
                BigDecimal taxAmount = BigDecimal.valueOf(totalAmount * 0.1);
                receipt.setTaxAmount(taxAmount);

                // Set discount amount (default 0.0 for cash payments)
                receipt.setDiscountAmount(BigDecimal.ZERO);

                logger.info("📄 Creating NEW receipt for transaction: {}", savedTransaction.getTransactionId());
            } else {
                logger.info("📄 Updating EXISTING receipt: {}", receipt.getReceiptId());
            }

            // Update receipt amount and details
            receipt.setTotalAmount(java.math.BigDecimal.valueOf(totalAmount));
            receipt.setNotes(notes != null ? notes : "Payment completed successfully");

            // Save receipt
            Receipt savedReceipt = receiptRepository.save(receipt);
            logger.info("✅ Receipt {} {}: Amount={} VND",
                       savedReceipt.getReceiptId(),
                       isNewReceipt ? "CREATED" : "UPDATED",
                       savedReceipt.getTotalAmount());

            // Update transaction with receipt reference if it's a new receipt
            if (isNewReceipt) {
                savedTransaction.setReceipt(savedReceipt);
                transactionRepository.save(savedTransaction);
            }

            // 🔥 STEP 7: Prepare response - chỉ trả về VND
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("transactionId", savedTransaction.getTransactionId());
            response.put("receiptId", savedReceipt.getReceiptId());
            response.put("status", "Paid");
            response.put("method", method);
            response.put("timeOfPayment", savedTransaction.getTimeOfPayment().toString());
            response.put("processedBy", issuerId);
            response.put("totalAmount", totalAmount);
            response.put("amountReceived", amountReceived);
            response.put("currency", CURRENCY_CODE_VND);
            response.put("message", "Payment processed successfully - no duplicates");

            logger.info("✅ PAYMENT PROCESSING COMPLETED - DUPLICATE PREVENTION SUCCESSFUL");
            return response;

        } catch (Exception e) {
            logger.error("❌ Error processing payment: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process payment: " + e.getMessage());
        }
    }

    // 🔥 USD TO VND CONVERSION HELPER METHODS



    /**
     * Normalize payment method từ database để đảm bảo hiển thị chính xác
     * Xử lý tất cả các payment methods có thể có trong database
     */
    public Integer getDoctorPatientCountFromSchedule(Integer doctorId) {
        try {
            logger.info("=== GETTING DOCTOR PATIENT COUNT FROM SCHEDULE ===");
            logger.info("DoctorID: {}", doctorId);

            // Validate input parameters
            if (doctorId == null) {
                logger.warn("DoctorID is null, returning 0");
                return 0;
            }

            // Use the repository method to count distinct patients by doctor ID
            Integer count = scheduleRepository.countDistinctPatientsByDoctorId(doctorId);

            logger.info("Found {} distinct patients for DoctorID: {}", count != null ? count : 0, doctorId);
            return count != null ? count : 0;

        } catch (Exception e) {
            logger.error("Error counting patients for doctor {}: {}", doctorId, e.getMessage(), e);
            return 0;
        }
    }
    /**
     * Normalize payment method từ database để đảm bảo hiển thị chính xác
     * Xử lý tất cả các payment methods có thể có trong database
     */
    private String normalizePaymentMethod(String transactionMethod) {
        if (transactionMethod == null || transactionMethod.trim().isEmpty()) {
            logger.warn("Transaction.Method is null/empty, using fallback: Cash");
            return "Cash";
        }

        // Normalize string - remove extra spaces và convert case
        String normalized = transactionMethod.trim();

        // Map các payment methods phổ biến từ database
        switch (normalized.toLowerCase()) {
            case "cash":
                return "Cash";
            case "banking":
            case "bank":
            case "vnpay":
            case "momo":
            case "zalopay":
                return "Banking";
            case "credit card":
            case "creditcard":
            case "card":
                return "Credit Card";
            case "debit card":
            case "debitcard":
                return "Debit Card";
            case "online":
            case "online payment":
                return "Online Payment";
            default:
                // Giữ nguyên giá trị gốc n���u không match với pattern nào
                logger.info("Using original Transaction.Method: '{}'", normalized);
                return normalized;
        }
    }

    /**
     * Normalize Vietnamese address data to ensure proper Vietnamese province/city names with diacritics
     * are stored in the database correctly
     */
    private String normalizeVietnameseProvince(String province) {
        if (province == null || province.trim().isEmpty()) {
            return "";
        }

        String normalized = province.trim();

        // Map of common Vietnamese provinces with proper diacritics
        Map<String, String> provinceMap = new HashMap<>();

        // Major cities
        provinceMap.put("ha noi", "Hà Nội");
        provinceMap.put("hanoi", "Hà Nội");
        provinceMap.put("ho chi minh", "Hồ Chí Minh");
        provinceMap.put("hcm", "Hồ Chí Minh");
        provinceMap.put("sai gon", "Hồ Chí Minh");
        provinceMap.put("saigon", "Hồ Chí Minh");
        provinceMap.put("da nang", "Đà Nẵng");
        provinceMap.put("danang", "Đà Nẵng");

        // Northern provinces
        provinceMap.put("hai phong", "Hải Phòng");
        provinceMap.put("haiphong", "Hải Phòng");
        provinceMap.put("quang ninh", "Quảng Ninh");
        provinceMap.put("ha long", "Quảng Ninh");
        provinceMap.put("halong", "Quảng Ninh");
        provinceMap.put("nam dinh", "Nam Định");
        provinceMap.put("namdinh", "Nam Định");
        provinceMap.put("thai binh", "Thái Bình");
        provinceMap.put("thaibinh", "Thái Bình");
        provinceMap.put("ninh binh", "Ninh Bình");
        provinceMap.put("ninhbinh", "Ninh Bình");
        provinceMap.put("thanh hoa", "Thanh Hóa");
        provinceMap.put("thanhhoa", "Thanh Hóa");
        provinceMap.put("nghe an", "Nghệ An");
        provinceMap.put("nghean", "Nghệ An");
        provinceMap.put("ha tinh", "Hà Tĩnh");
        provinceMap.put("hatinh", "Hà Tĩnh");

        // Central provinces
        provinceMap.put("quang binh", "Quảng Bình");
        provinceMap.put("quangbinh", "Quảng Bình");
        provinceMap.put("quang tri", "Quảng Trị");
        provinceMap.put("quangtri", "Quảng Trị");
        provinceMap.put("hue", "Thừa Thiên Huế");
        provinceMap.put("thua thien hue", "Thừa Thiên Huế");
        provinceMap.put("quang nam", "Quảng Nam");
        provinceMap.put("quangnam", "Quảng Nam");
        provinceMap.put("quang ngai", "Quảng Ngãi");
        provinceMap.put("quangngai", "Quảng Ngãi");
        provinceMap.put("binh dinh", "Bình Định");
        provinceMap.put("binhdinh", "Bình Định");
        provinceMap.put("phu yen", "Phú Yên");
        provinceMap.put("phuyen", "Phú Yên");
        provinceMap.put("khanh hoa", "Khánh Hòa");
        provinceMap.put("khanhhoa", "Khánh Hòa");
        provinceMap.put("nha trang", "Khánh Hòa");
        provinceMap.put("nhatrang", "Khánh Hòa");

        // Southern provinces
        provinceMap.put("ninh thuan", "Ninh Thuận");
        provinceMap.put("ninhthuan", "Ninh Thuận");
        provinceMap.put("binh thuan", "Bình Thuận");
        provinceMap.put("binhthuan", "Bình Thuận");
        provinceMap.put("kon tum", "Kon Tum");
        provinceMap.put("kontum", "Kon Tum");
        provinceMap.put("gia lai", "Gia Lai");
        provinceMap.put("gialai", "Gia Lai");
        provinceMap.put("dak lak", "Đắk Lắk");
        provinceMap.put("daklak", "Đắk Lắk");
        provinceMap.put("dak nong", "Đắk Nông");
        provinceMap.put("daknong", "Đắk Nông");
        provinceMap.put("lam dong", "Lâm Đồng");
        provinceMap.put("lamdong", "Lâm Đồng");
        provinceMap.put("da lat", "Lâm Đồng");
        provinceMap.put("dalat", "Lâm Đồng");

        // Mekong Delta provinces
        provinceMap.put("dong nai", "Đồng Nai");
        provinceMap.put("dongnai", "Đồng Nai");
        provinceMap.put("binh duong", "Bình Dương");
        provinceMap.put("binhduong", "Bình Dương");
        provinceMap.put("tay ninh", "Tây Ninh");
        provinceMap.put("tayninh", "Tây Ninh");
        provinceMap.put("long an", "Long An");
        provinceMap.put("longan", "Long An");
        provinceMap.put("tien giang", "Tiền Giang");
        provinceMap.put("tiengiang", "Ti���n Giang");
        provinceMap.put("ben tre", "Bến Tre");
        provinceMap.put("bentre", "Bến Tre");
        provinceMap.put("tra vinh", "Trà Vinh");
        provinceMap.put("travinh", "Trà Vinh");
        provinceMap.put("vinh long", "Vĩnh Long");
        provinceMap.put("vinhlong", "Vĩnh Long");
        provinceMap.put("dong thap", "Đồng Tháp");
        provinceMap.put("dongthap", "Đồng Tháp");
        provinceMap.put("an giang", "An Giang");
        provinceMap.put("angiang", "An Giang");
        provinceMap.put("kien giang", "Kiên Giang");
        provinceMap.put("kiengiang", "Kiên Giang");
        provinceMap.put("can tho", "Cần Thơ");
        provinceMap.put("cantho", "Cần Thơ");
        provinceMap.put("hau giang", "Hậu Giang");
        provinceMap.put("haugiang", "Hậu Giang");
        provinceMap.put("soc trang", "Sóc Trăng");
        provinceMap.put("soctrang", "Sóc Trăng");
        provinceMap.put("bac lieu", "Bạc Liêu");
        provinceMap.put("baclieu", "Bạc Liêu");
        provinceMap.put("ca mau", "Cà Mau");
        provinceMap.put("camau", "Cà Mau");

        // Additional northern provinces
        provinceMap.put("lang son", "Lạng Sơn");
        provinceMap.put("langson", "Lạng Sơn");
        provinceMap.put("cao bang", "Cao Bằng");
        provinceMap.put("caobang", "Cao Bằng");
        provinceMap.put("ha giang", "Hà Giang");
        provinceMap.put("hagiang", "Hà Giang");
        provinceMap.put("lai chau", "Lai Châu");
        provinceMap.put("laichau", "Lai Châu");
        provinceMap.put("son la", "Sơn La");
        provinceMap.put("sonla", "Sơn La");
        provinceMap.put("dien bien", "Điện Biên");
        provinceMap.put("dienbien", "Điện Biên");
        provinceMap.put("lao cai", "Lào Cai");
        provinceMap.put("laocai", "Lào Cai");
        provinceMap.put("yen bai", "Yên Bái");
        provinceMap.put("yenbai", "Yên Bái");
        provinceMap.put("tuyen quang", "Tuyên Quang");
        provinceMap.put("tuyenquang", "Tuyên Quang");
        provinceMap.put("ha nam", "Hà Nam");
        provinceMap.put("hanam", "Hà Nam");
        provinceMap.put("hung yen", "Hưng Yên");
        provinceMap.put("hungyen", "Hưng Yên");
        provinceMap.put("bac giang", "Bắc Giang");
        provinceMap.put("bacgiang", "Bắc Giang");
        provinceMap.put("bac kan", "Bắc Kạn");
        provinceMap.put("backan", "Bắc Kạn");
        provinceMap.put("bac ninh", "Bắc Ninh");
        provinceMap.put("bacninh", "Bắc Ninh");
        provinceMap.put("thai nguyen", "Thái Nguyên");
        provinceMap.put("thainguyen", "Thái Nguyên");
        provinceMap.put("phu tho", "Phú Thọ");
        provinceMap.put("phutho", "Phú Thọ");
        provinceMap.put("vinh phuc", "Vĩnh Phúc");
        provinceMap.put("vinhphuc", "Vĩnh Phúc");

        // Check for exact match first (case insensitive)
        String normalizedKey = normalized.toLowerCase();
        if (provinceMap.containsKey(normalizedKey)) {
            return provinceMap.get(normalizedKey);
        }

        // If no exact match, return the original input (might already be correct Vietnamese)
        return normalized;
    }

    /**
     * Normalize Vietnamese city/district names - PUBLIC method for controller access
     */
    public String normalizeVietnameseCity(String city) {
        if (city == null || city.trim().isEmpty()) {
            return "";
        }

        String normalized = city.trim();

        // Map of common Vietnamese cities/districts with proper diacritics
        Map<String, String> cityMap = new HashMap<>();

        // Ho Chi Minh City districts
        cityMap.put("quan 1", "Quận 1");
        cityMap.put("quan 2", "Quận 2");
        cityMap.put("quan 3", "Quận 3");
        cityMap.put("quan 4", "Quận 4");
        cityMap.put("quan 5", "Quận 5");
        cityMap.put("quan 6", "Qu��n 6");
        cityMap.put("quan 7", "Quận 7");
        cityMap.put("quan 8", "Quận 8");
        cityMap.put("quan 9", "Quận 9");
        cityMap.put("quan 10", "Quận 10");
        cityMap.put("quan 11", "Quận 11");
        cityMap.put("quan 12", "Quận 12");
        cityMap.put("binh thanh", "Bình Thạnh");
        cityMap.put("binhthanh", "Bình Thạnh");
        cityMap.put("tan binh", "Tân Bình");
        cityMap.put("tanbinh", "Tân Bình");
        cityMap.put("phu nhuan", "Phú Nhuận");
        cityMap.put("phunhuan", "Phú Nhuận");
        cityMap.put("go vap", "Gò Vấp");
        cityMap.put("govap", "Gò Vấp");
        cityMap.put("thu duc", "Thủ Đức");
        cityMap.put("thuduc", "Thủ Đức");

        // Hanoi districts
        cityMap.put("ba dinh", "Ba Đình");
        cityMap.put("badinh", "Ba Đình");
        cityMap.put("hoan kiem", "Hoàn Kiếm");
        cityMap.put("hoankiem", "Hoàn Kiếm");
        cityMap.put("hai ba trung", "Hai Bà Trưng");
        cityMap.put("haibatrung", "Hai Bà Trưng");
        cityMap.put("dong da", "Đống Đa");
        cityMap.put("dongda", "Đống Đa");
        cityMap.put("tay ho", "Tây Hồ");
        cityMap.put("tayho", "Tây Hồ");
        cityMap.put("cau giay", "Cầu Giấy");
        cityMap.put("caugiay", "Cầu Giấy");
        cityMap.put("thanh xuan", "Thanh Xuân");
        cityMap.put("thanhxuan", "Thanh Xuân");
        cityMap.put("hoang mai", "Hoàng Mai");
        cityMap.put("hoangmai", "Hoàng Mai");
        cityMap.put("long bien", "Long Biên");
        cityMap.put("longbien", "Long Biên");

        // Check for exact match first (case insensitive)
        String normalizedKey = normalized.toLowerCase();
        if (cityMap.containsKey(normalizedKey)) {
            return cityMap.get(normalizedKey);
        }

        // If no exact match, return the original input (might already be correct Vietnamese)
        return normalized;
    }

    /**
     * Normalize Vietnamese country names - PUBLIC method for controller access
     */
    public String normalizeVietnameseCountry(String country) {
        if (country == null || country.trim().isEmpty()) {
            return "Việt Nam";
        }

        String normalized = country.trim().toLowerCase();

        if (normalized.equals("vietnam") || normalized.equals("viet nam") ||
            normalized.equals("vietname") || normalized.equals("vn")) {
            return "Việt Nam";
        }

        return country.trim();
    }

    /**
     * Get appointments for the next 7 days (including today) grouped by date
     * Returns a map with date groups and appointment counts for each day
     * OPTIMIZED: Only fetch appointments within the 7-day range instead of all appointments
     */
    public Map<String, Object> getNext7DaysAppointmentTableData() {
        try {
            LocalDate today = LocalDate.now();
            LocalDate endDate = today.plusDays(6); // 7 days total (today + 6 more days)

            // Convert to LocalDateTime for database query
            java.time.LocalDateTime startDateTime = today.atStartOfDay();
            java.time.LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

            logger.info("=== FETCHING APPOINTMENTS FOR NEXT 7 DAYS ===");
            logger.info("Date range: {} to {}", startDateTime, endDateTime);

            Map<String, Object> result = new HashMap<>();
            Map<String, List<Map<String, Object>>> appointmentsByDate = new HashMap<>();
            Map<String, Integer> countsByDate = new HashMap<>();

            // OPTIMIZED: Only get appointments within the 7-day range
            List<Appointment> next7DaysAppointments = appointmentRepository.findByDateTimeBetween(startDateTime, endDateTime);
            logger.info("Found {} appointments in the next 7 days", next7DaysAppointments.size());

            // Process each of the next 7 days
            for (int i = 0; i < 7; i++) {
                final int dayIndex = i; // Create final copy for lambda usage
                LocalDate targetDate = today.plusDays(i);
                String dateKey = targetDate.toString();
                String dayName = targetDate.getDayOfWeek().toString();

                // Filter appointments for this specific date from the already filtered list
                List<Map<String, Object>> dayAppointments = next7DaysAppointments.stream()
                    .filter(appointment -> {
                        LocalDate appointmentDate = appointment.getDateTime().toLocalDate();
                        return appointmentDate.equals(targetDate);
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
                        appointmentData.put("dayName", dayName);
                        appointmentData.put("dayIndex", dayIndex); // Use final copy instead of i

                        // Calculate age
                        if (appointment.getPatient().getDateOfBirth() != null) {
                            LocalDate birthDate = appointment.getPatient().getDateOfBirth();
                            int age = today.getYear() - birthDate.getYear();
                            if (today.getDayOfYear() < birthDate.getDayOfYear()) {
                                age--;
                            }
                            appointmentData.put("age", age);
                        } else {
                            appointmentData.put("age", "N/A");
                        }

                        // Add doctor name if available
                        if (appointment.getDoctor() != null && appointment.getDoctor().getUser() != null) {
                            appointmentData.put("doctorName", appointment.getDoctor().getUser().getFullName());
                        } else {
                            appointmentData.put("doctorName", "Not Assigned");
                        }

                        return appointmentData;
                    })
                    .collect(Collectors.toList());

                appointmentsByDate.put(dateKey, dayAppointments);
                countsByDate.put(dateKey, dayAppointments.size());

                logger.info("Date {} ({}): {} appointments found", dateKey, dayName, dayAppointments.size());
            }

            // Prepare result structure
            result.put("appointmentsByDate", appointmentsByDate);
            result.put("countsByDate", countsByDate);
            result.put("dateRange", Map.of(
                "startDate", today.toString(),
                "endDate", today.plusDays(6).toString(),
                "totalDays", 7
            ));

            // Calculate total appointments across all 7 days
            int totalAppointments = countsByDate.values().stream().mapToInt(Integer::intValue).sum();
            result.put("totalAppointments", totalAppointments);

            // Add summary information for frontend
            result.put("summary", Map.of(
                "totalAppointments", totalAppointments,
                "daysWithAppointments", countsByDate.values().stream().mapToInt(count -> count > 0 ? 1 : 0).sum(),
                "averagePerDay", totalAppointments / 7.0,
                "queryRange", startDateTime + " to " + endDateTime
            ));

            logger.info("✅ Successfully retrieved appointments for next 7 days. Total: {} appointments", totalAppointments);
            return result;

        } catch (Exception e) {
            logger.error("❌ Error retrieving next 7 days appointment data: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve next 7 days appointment data", e);
        }
    }
}
