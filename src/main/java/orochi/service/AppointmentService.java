package orochi.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import orochi.dto.AppointmentDTO;
import orochi.dto.AppointmentFormDTO;
import orochi.model.*;
import orochi.repository.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AppointmentService {

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private SpecializationRepository specializationRepository;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    private static final List<String> ALL_TIME_SLOTS = Arrays.asList(
            "08:00:00", "08:30:00", "09:00:00", "09:30:00", "10:00:00", "10:30:00",
            "11:00:00", "11:30:00", "14:00:00", "14:30:00", "15:00:00", "15:30:00",
            "16:00:00", "16:30:00", "17:00:00", "17:30:00"
    );

    // Standard appointment duration in minutes
    private static final int APPOINTMENT_DURATION = 30;

    public List<Specialization> getAllSpecializations() {
        return specializationRepository.findAllByOrderBySpecNameAsc();
    }

    public List<Doctor> getDoctorsBySpecialization(Integer specId) {
        return doctorRepository.findBySpecializationId(specId);
    }

    public AppointmentDTO getAppointmentDetails(Integer appointmentId) {
        // Tìm kiếm lịch hẹn trong cơ sở dữ liệu dựa trên appointmentId - avoid loading transactions
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElse(null);

        List<Specialization> specializations = specializationRepository.findAll();

        // Nếu không tìm thấy lịch hẹn, trả về null
        if (appointment == null) {
            return null;
        }

        // Ánh xạ từ entity Appointment sang DTO
        AppointmentDTO appointmentDTO = new AppointmentDTO();
        appointmentDTO.setId(Long.valueOf(appointment.getAppointmentId()));
        appointmentDTO.setFullName(appointment.getPatient().getFullName());
        appointmentDTO.setPatientID(String.valueOf(appointment.getPatient().getPatientId()));
        appointmentDTO.setGender(appointment.getPatient().getGender());
        appointmentDTO.setDateOfBirth(appointment.getPatient().getDateOfBirth());
        appointmentDTO.setPhoneNumber(appointment.getPatient().getUser().getPhoneNumber());
        appointmentDTO.setEmail(appointment.getPatient().getUser().getEmail());
        appointmentDTO.setSpecializations(specializations);
        appointmentDTO.setAppointmentTime(appointment.getDateTime().toLocalTime());

        // Lấy thông tin liên hệ từ PatientContact (lấy contact đầu tiên nếu có)
        if (appointment.getPatient().getContacts() != null && !appointment.getPatient().getContacts().isEmpty()) {
            orochi.model.PatientContact contact = appointment.getPatient().getContacts().get(0);
            appointmentDTO.setAddressType(contact.getAddressType());
            appointmentDTO.setStreetAddress(contact.getStreetAddress());
            appointmentDTO.setCity(contact.getCity());
            appointmentDTO.setState(contact.getState());
            appointmentDTO.setPostalCode(contact.getPostalCode());
            appointmentDTO.setCountry(contact.getCountry());
        }

        // Set appointment specific information
        appointmentDTO.setRoom(appointment.getRoom().getRoomNumber());
        appointmentDTO.setDoctor(appointment.getDoctor());
        appointmentDTO.setSpecialtyId(String.valueOf(appointment.getDoctor().getSpecializations().get(0).getSpecId()));
        appointmentDTO.setSpecialtyName(appointment.getDoctor().getSpecializations().get(0).getSpecName());
        appointmentDTO.setAppointmentStatus(appointment.getStatus());
        appointmentDTO.setReasonForVisit(appointment.getDescription());
        appointmentDTO.setAppointmentDate(appointment.getDateTime().toLocalDate());

        // Fetch payment status separately to avoid SQL grammar exception
        String paymentStatus = getPaymentStatusForAppointment(appointmentId);
        appointmentDTO.setPaymentStatus(paymentStatus);

        return appointmentDTO;
    }

    /**
     * Get payment status for an appointment by querying transactions separately
     */
    private String getPaymentStatusForAppointment(Integer appointmentId) {
        try {
            // Query transactions directly using a custom repository method
            List<Transaction> transactions = transactionRepository.findByAppointmentId(appointmentId);

            if (transactions.isEmpty()) {
                return "Unpaid";
            }

            // Get the latest transaction status
            Transaction latestTransaction = transactions.get(0);
            String transactionStatus = latestTransaction.getStatus();

            // Map transaction status to payment status
            switch (transactionStatus.toLowerCase()) {
                case "completed":
                case "success":
                    return "Paid";
                case "pending":
                    return "Pending";
                case "failed":
                case "cancelled":
                    return "Failed";
                case "refunded":
                    return "Refunded";
                default:
                    return "Unpaid";
            }
        } catch (Exception e) {
            // Log the error and return default status
            System.err.println("Error fetching payment status for appointment " + appointmentId + ": " + e.getMessage());
            return "Unknown";
        }
    }

    public Map<String, List<String>> getDoctorAvailability(LocalDate date, Integer doctorId, Integer excludeAppointmentId) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59);

        // Fetch booked appointments
        List<Appointment> bookedAppointments;
        if (excludeAppointmentId != null) {
            bookedAppointments = appointmentRepository.findByDoctorIdAndDateTimeBetweenAndAppointmentIdNotOrderByDateTime(
                    doctorId, startOfDay, endOfDay, excludeAppointmentId);
        } else {
            bookedAppointments = appointmentRepository.findByDoctorIdAndDateTimeBetweenOrderByDateTime(
                    doctorId, startOfDay, endOfDay);
        }

        List<String> bookedTimes = bookedAppointments.stream()
                .map(appointment -> appointment.getDateTime().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm:ss")))
                .collect(Collectors.toList());

        // Fetch on-call schedules
        List<Schedule> onCallSchedules = scheduleRepository.findByDoctorIdAndScheduleDateAndEventType(doctorId, date, "oncall");

        // Calculate unavailable times due to on-call periods
        Set<String> onCallUnavailableTimes = new HashSet<>();
        for (String timeSlot : ALL_TIME_SLOTS) {
            LocalTime slotTime = LocalTime.parse(timeSlot);
            LocalTime slotEnd = slotTime.plusMinutes(30);
            for (Schedule onCall : onCallSchedules) {
                LocalTime onCallStart = onCall.getStartTime();
                LocalTime onCallEnd = onCall.getEndTime();
                if (slotTime.isBefore(onCallEnd) && slotEnd.isAfter(onCallStart)) {
                    onCallUnavailableTimes.add(timeSlot);
                    break;
                }
            }
        }

        // Calculate unavailable times due to proximity to booked appointments
        List<String> proximityUnavailableTimes = new ArrayList<>();
        for (Appointment appointment : bookedAppointments) {
            LocalTime appointmentTime = appointment.getDateTime().toLocalTime();
            for (String timeSlot : ALL_TIME_SLOTS) {
                LocalTime slotTime = LocalTime.parse(timeSlot);
                long minutesDifference = Math.abs(ChronoUnit.MINUTES.between(slotTime, appointmentTime));
                if (minutesDifference < APPOINTMENT_DURATION && !bookedTimes.contains(timeSlot)) {
                    proximityUnavailableTimes.add(timeSlot);
                }
            }
        }

        // Combine all unavailable times, excluding booked times
        Set<String> allUnavailableTimes = new HashSet<>(onCallUnavailableTimes);
        allUnavailableTimes.addAll(proximityUnavailableTimes);
        List<String> unavailableTimes = allUnavailableTimes.stream()
                .filter(time -> !bookedTimes.contains(time))
                .distinct()
                .collect(Collectors.toList());

        Map<String, List<String>> result = new HashMap<>();
        result.put("bookedTimes", bookedTimes);
        result.put("unavailableTimes", unavailableTimes);
        return result;
    }

    public Map<String, List<String>> getDoctorAvailability(LocalDate date, Integer doctorId) {
        return getDoctorAvailability(date, doctorId, null);
    }

    @Deprecated
    public List<String> getBookedTimeSlots(LocalDate date, Integer doctorId) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59);

        List<Appointment> bookedAppointments = appointmentRepository
            .findByDoctorIdAndDateTimeBetweenOrderByDateTime(doctorId, startOfDay, endOfDay);

        return bookedAppointments.stream()
            .map(appointment -> appointment.getDateTime().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm:ss")))
            .collect(Collectors.toList());
    }

    @Transactional
    public Appointment bookAppointment(Integer patientId, Integer doctorId, LocalDate appointmentDate,
                                       String appointmentTime, String email, String phoneNumber,
                                       String description) {
        if (email == null || email.isEmpty()) {
            throw new RuntimeException("Email is required.");
        }
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            throw new RuntimeException("Phone number is required.");
        }
        if (!phoneNumber.matches("^0\\d{9}$|^0\\d{11}$")) {
            throw new RuntimeException("Phone number must start with 0 and be either 10 or 12 digits.");
        }

        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new RuntimeException("Doctor not found with ID: " + doctorId));
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new RuntimeException("Patient not found with ID: " + patientId));

        LocalTime time = LocalTime.parse(appointmentTime);
        LocalDateTime dateTime = LocalDateTime.of(appointmentDate, time);

        Map<String, List<String>> availability = getDoctorAvailability(appointmentDate, doctorId);
        if (availability.get("bookedTimes").contains(appointmentTime) ||
                availability.get("unavailableTimes").contains(appointmentTime)) {
            throw new RuntimeException("The selected time slot is unavailable. Please choose another time.");
        }

        Appointment appointment = new Appointment();
        appointment.setPatientId(patientId);
        appointment.setDoctorId(doctorId);
        appointment.setDateTime(dateTime);
        appointment.setStatus("Pending");
        appointment.setEmail(email);
        appointment.setPhoneNumber(phoneNumber);
        appointment.setDescription(description);

        return appointmentRepository.save(appointment);
    }

    @Transactional
    public Appointment updateAppointment(Integer appointmentId, Integer patientId, Integer doctorId,
                                         LocalDate appointmentDate, String appointmentTime,
                                         String email, String phoneNumber, String description) {
        if (email == null || email.isEmpty()) {
            throw new RuntimeException("Email is required.");
        }
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            throw new RuntimeException("Phone number is required.");
        }
        if (!phoneNumber.matches("^0\\d{9}$|^0\\d{11}$")) {
            throw new RuntimeException("Phone number must start with 0 and be either 10 or 12 digits.");
        }

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found with ID: " + appointmentId));
        if (!appointment.getPatientId().equals(patientId)) {
            throw new RuntimeException("You do not have permission to update this appointment.");
        }

        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new RuntimeException("Doctor not found with ID: " + doctorId));

        LocalTime time = LocalTime.parse(appointmentTime);
        LocalDateTime dateTime = LocalDateTime.of(appointmentDate, time);

        Map<String, List<String>> availability = getDoctorAvailability(appointmentDate, doctorId, appointmentId);
        if (availability.get("bookedTimes").contains(appointmentTime) ||
                availability.get("unavailableTimes").contains(appointmentTime)) {
            throw new RuntimeException("The selected time slot is unavailable. Please choose another time.");
        }

        appointment.setDoctorId(doctorId);
        appointment.setDateTime(dateTime);
        appointment.setEmail(email);
        appointment.setPhoneNumber(phoneNumber);
        appointment.setDescription(description);

        return appointmentRepository.save(appointment);
    }

    //public abstract Appointment bookAppointment(AppointmentFormDTO appointmentDTO);
    public Appointment updateAppointmentStatus(Integer appointmentId, String status) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
            .orElseThrow(() -> new RuntimeException("Appointment not found with ID: " + appointmentId));
        appointment.setStatus(status); // Direct assignment of enum value instead of toString()
        appointmentRepository.save(appointment);
        return appointment;
    }

    public Page<Appointment> searchAppointmentsByDoctorId(Integer doctorId, String search, Pageable pageable) {
        // Search appointments for a specific doctor with a search term (patient name)
        // Convert list to page for pagination
        List<Appointment> appointments = appointmentRepository.findByDoctorIdAndPatientUserFullNameContainingIgnoreCase(doctorId, search);

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), appointments.size());

        List<Appointment> pageContent = appointments.subList(start, end);
        return new org.springframework.data.domain.PageImpl<>(pageContent, pageable, appointments.size());
    }

    public Page<Appointment> getAppointmentsByDoctorId(Integer doctorId, Pageable pageable) {
        List<Appointment> appointments = appointmentRepository.findByDoctorIdOrderByDateTimeDesc(doctorId);

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), appointments.size());

        List<Appointment> pageContent = appointments.subList(start, end);
        return new org.springframework.data.domain.PageImpl<>(pageContent, pageable, appointments.size());
    }

    public Page<Appointment> getAllAppointments(Pageable pageable) {
        return appointmentRepository.findAll(pageable);
    }

    /**
     * Get appointments filtered by status
     * @param status The status to filter by (Scheduled, Completed, Cancel, Pending)
     * @param pageable Pagination information
     * @return Page of filtered appointments
     */
    public Page<Appointment> getAppointmentsByStatus(String status, Pageable pageable) {
        return appointmentRepository.findByStatusOrderByDateTimeDesc(status, pageable);
    }

    @Transactional
    public Appointment bookAppointment(AppointmentFormDTO appointmentDTO) {
        // Validate doctor and patient existence
        Doctor doctor = doctorRepository.findById(appointmentDTO.getDoctorId())
            .orElseThrow(() -> new RuntimeException("Doctor not found with ID: " + appointmentDTO.getDoctorId()));

        Patient patient = patientRepository.findById(appointmentDTO.getPatientId())
            .orElseThrow(() -> new RuntimeException("Patient not found with ID: " + appointmentDTO.getPatientId()));

        // Get date and time
        LocalDate appointmentDate = appointmentDTO.getAppointmentDate(); // Already a LocalDate, no need to parse
        LocalTime appointmentTime = LocalTime.parse(appointmentDTO.getAppointmentTime());
        LocalDateTime dateTime = LocalDateTime.of(appointmentDate, appointmentTime);

        // Check time slot availability
        Map<String, List<String>> availability = getDoctorAvailability(appointmentDate, appointmentDTO.getDoctorId());
        if (availability.get("bookedTimes").contains(appointmentDTO.getAppointmentTime()) ||
                availability.get("unavailableTimes").contains(appointmentDTO.getAppointmentTime())) {
            throw new RuntimeException("The selected time slot is unavailable. Please choose another time.");
        }

        // Create new appointment
        Appointment appointment = new Appointment();
        appointment.setPatientId(appointmentDTO.getPatientId());
        appointment.setDoctorId(appointmentDTO.getDoctorId());
        appointment.setRoomId(appointmentDTO.getRoomId()); // SET ROOM ID HERE
        appointment.setDateTime(dateTime);
        appointment.setStatus("Pending"); // Default status
        appointment.setEmail(appointmentDTO.getEmail());
        appointment.setPhoneNumber(appointmentDTO.getPhoneNumber());
        appointment.setDescription(appointmentDTO.getDescription());

        // Save appointment
        return appointmentRepository.save(appointment);
    }

    @Transactional
    public boolean updateAppointment(Integer appointmentId, AppointmentFormDTO appointmentFormDTO) {
        try {
            Optional<Appointment> optionalAppointment = appointmentRepository.findById(appointmentId);
            if (optionalAppointment.isPresent()) {
                Appointment appointment = optionalAppointment.get();

                // Update appointment fields
                if (appointmentFormDTO.getDoctorId() != null) {
                    appointment.setDoctorId(appointmentFormDTO.getDoctorId());
                }

                if (appointmentFormDTO.getAppointmentDate() != null && appointmentFormDTO.getAppointmentTime() != null) {
                    LocalDateTime dateTime = LocalDateTime.of(
                        appointmentFormDTO.getAppointmentDate(),
                            LocalTime.parse(appointmentFormDTO.getAppointmentTime())
                    );
                    appointment.setDateTime(dateTime);
                }

                if (appointmentFormDTO.getDescription() != null) {
                    appointment.setDescription(appointmentFormDTO.getDescription());
                }

                appointmentRepository.save(appointment);
                return true;
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<Appointment> getAppointmentsByDoctorIdAndPatientName(Integer doctorId, String search) {
        return null;
    }

    /**
     * Get all appointments for a specific doctor on a specific date
     * Used for checking time slot conflicts
     */
    public List<Appointment> getAppointmentsByDoctorAndDate(Integer doctorId, LocalDate date) {
        try {
            LocalDateTime startOfDay = date.atStartOfDay();
            LocalDateTime endOfDay = date.atTime(23, 59, 59);

            return appointmentRepository.findByDoctorIdAndDateTimeBetween(doctorId, startOfDay, endOfDay);
        } catch (Exception e) {
            System.err.println("Error fetching appointments by doctor and date: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
  
  // 1) Overload lấy availability (full logic)
public Map<String, List<String>> getDoctorAvailability2(
        LocalDate date,
        Integer doctorId,
        Integer excludeAppointmentId) {
    LocalDateTime startOfDay = date.atStartOfDay();
    LocalDateTime endOfDay   = date.atTime(23,59,59);

    List<Appointment> booked = (excludeAppointmentId != null
        ? appointmentRepository.findByDoctorIdAndDateTimeBetweenAndAppointmentIdNotOrderByDateTime(
            doctorId, startOfDay, endOfDay, excludeAppointmentId)
        : appointmentRepository.findByDoctorIdAndDateTimeBetweenOrderByDateTime(
            doctorId, startOfDay, endOfDay)
    );

    List<String> bookedTimes = booked.stream()
        .map(a -> a.getDateTime().toLocalTime()
            .format(DateTimeFormatter.ofPattern("HH:mm:ss")))
        .collect(Collectors.toList());

    Set<String> allSlots = Set.of(
        "08:00:00","08:30:00","09:00:00","09:30:00",
        "10:00:00","10:30:00","11:00:00","11:30:00",
        "14:00:00","14:30:00","15:00:00","15:30:00",
        "16:00:00","16:30:00","17:00:00","17:30:00"
    );

    Set<String> unavailable = new HashSet<>();
    for (Appointment a : booked) {
        LocalTime t = a.getDateTime().toLocalTime();
        for (String slot : allSlots) {
            LocalTime s = LocalTime.parse(slot);
            int diffMin = Math.abs(
                (t.getHour()*60 + t.getMinute()) -
                (s.getHour()*60 + s.getMinute())
            );
            if (diffMin < APPOINTMENT_DURATION && !bookedTimes.contains(slot)) {
                unavailable.add(slot);
            }
        }
    }

    return Map.of(
        "bookedTimes", bookedTimes,
        "unavailableTimes", new ArrayList<>(unavailable)
    );
}

public Map<String, List<String>> getDoctorAvailability2(LocalDate date, Integer doctorId) {
    return getDoctorAvailability2(date, doctorId, null);
}

// 2) Book appointment từ DTO (đầy đủ)
@Transactional
public Appointment bookAppointment2(AppointmentFormDTO dto) {
    Doctor doc = doctorRepository.findById(dto.getDoctorId())
        .orElseThrow(() -> new RuntimeException("Doctor not found"));
    Patient pat = patientRepository.findById(dto.getPatientId())
        .orElseThrow(() -> new RuntimeException("Patient not found"));

    LocalDateTime when = LocalDateTime.of(
        dto.getAppointmentDate(),
        LocalTime.parse(dto.getAppointmentTime())
    );

    Map<String, List<String>> avail = getDoctorAvailability(dto.getAppointmentDate(), dto.getDoctorId());
    if (avail.get("bookedTimes").contains(dto.getAppointmentTime()) ||
        avail.get("unavailableTimes").contains(dto.getAppointmentTime())) {
        throw new RuntimeException("Time slot unavailable");
    }

    Appointment appt = new Appointment();
    appt.setDoctorId(doc.getDoctorId());
    appt.setPatientId(pat.getPatientId());
    appt.setDateTime(when);
    appt.setStatus("Pending");
    appt.setEmail(dto.getEmail());
    appt.setPhoneNumber(dto.getPhoneNumber());
    appt.setDescription(dto.getDescription());

    return appointmentRepository.save(appt);
}

// 3) Update status
public Appointment updateAppointmentStatus2(Integer appointmentId, String status) {
    Appointment appt = appointmentRepository.findById(appointmentId)
        .orElseThrow(() -> new RuntimeException("Not found"));
    appt.setStatus(status);
    return appointmentRepository.save(appt);
}

// 4) Get all appointments paging
public Page<Appointment> getAllAppointments2(Pageable pg) {
    return appointmentRepository.findAll(pg);
}

// 5) Get appointments by doctor paging (full subList logic)
public Page<Appointment> getAppointmentsByDoctorId2(Integer docId, Pageable pg) {
    List<Appointment> list = appointmentRepository.findByDoctorIdOrderByDateTimeDesc(docId);
    int start = (int) pg.getOffset();
    int end   = Math.min(start + pg.getPageSize(), list.size());
    return new org.springframework.data.domain.PageImpl<>(list.subList(start, end), pg, list.size());
}

// 6) Get appointments by status paging
public Page<Appointment> getAppointmentsByStatus2(String status, Pageable pg) {
    return appointmentRepository.findByStatusOrderByDateTimeDesc(status, pg);
}

// 7) Fetch period counts (unified)
public Map<String, Long> fetchPeriodCounts2(
        LocalDate fromDate,
        LocalDate toDate,
        Integer doctorId,
        Integer specId,
        String status,
        String groupBy) {
    LocalDateTime start = fromDate.atStartOfDay();
    LocalDateTime end   = toDate.atTime(23,59,59);

    String statusFilter = "ALL".equalsIgnoreCase(status) ? "ALL" : status;
    List<Appointment> list = appointmentRepository.findWithFilters(
        doctorId, specId, statusFilter, null, start, end, Pageable.unpaged()
    ).getContent();

    Function<Appointment, String> classifier;
    switch (groupBy.toLowerCase()) {
        case "quarter":
            classifier = a -> {
                int y = a.getDateTime().getYear();
                int q = ((a.getDateTime().getMonthValue() - 1) / 3) + 1;
                return String.format("%d-Q%d", y, q);
            }; break;
        case "year":
            classifier = a -> String.valueOf(a.getDateTime().getYear()); break;
        default:
            classifier = a -> a.getDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM"));
    }

    return list.stream()
        .collect(Collectors.groupingBy(
            classifier,
            LinkedHashMap::new,
            Collectors.counting()
        ));
}

// 8) Search appointments
public Page<Appointment> searchAppointmentsByDoctorId2(
        Integer doctorId,
        String search,
        Pageable pageable) {
    LocalDateTime start = LocalDate.now().atStartOfDay();
    LocalDateTime end   = LocalDate.now().plusYears(10).atTime(23,59,59);

    return appointmentRepository.findWithFilters(
        doctorId, null, "ALL",
        (search == null || search.isBlank()) ? null : search,
        start, end, pageable
    );
}

// 9) Lấy booked time slots
public List<String> getBookedTimeSlots2(LocalDate date, Integer doctorId) {
    return getDoctorAvailability(date, doctorId).get("bookedTimes");
}

// 10) Update full appointment
@Transactional
public Appointment updateAppointment2(
        Integer appointmentId,
        Integer patientId,
        Integer doctorId,
        LocalDate appointmentDate,
        String appointmentTime,
        String email,
        String phoneNumber,
        String description) {

    if (email == null || email.isEmpty()) {
        throw new RuntimeException("Email is required.");
    }
    if (phoneNumber == null || phoneNumber.isEmpty()) {
        throw new RuntimeException("Phone number is required.");
    }
    if (!phoneNumber.matches("^0\\d{9}$|^0\\d{11}$")) {
        throw new RuntimeException("Phone number must start with 0 and be either 10 or 12 digits.");
    }

    Appointment appt = appointmentRepository.findById(appointmentId)
        .orElseThrow(() -> new RuntimeException("Appointment not found with ID: " + appointmentId));
    if (!appt.getPatientId().equals(patientId)) {
        throw new RuntimeException("You do not have permission to update this appointment.");
    }

    doctorRepository.findById(doctorId)
        .orElseThrow(() -> new RuntimeException("Doctor not found with ID: " + doctorId));

    LocalDateTime dateTime = LocalDateTime.of(appointmentDate, LocalTime.parse(appointmentTime));
    Map<String, List<String>> availability =
        getDoctorAvailability(appointmentDate, doctorId, appointmentId);
    if (availability.get("bookedTimes").contains(appointmentTime) ||
        availability.get("unavailableTimes").contains(appointmentTime)) {
        throw new RuntimeException("The selected time slot is unavailable. Please choose another time.");
    }

    appt.setDoctorId(doctorId);
    appt.setDateTime(dateTime);
    appt.setEmail(email);
    appt.setPhoneNumber(phoneNumber);
    appt.setDescription(description);

    return appointmentRepository.save(appt);
}

    public List<Doctor> getAllDoctors() {
        return doctorRepository.findAll();
    }

}