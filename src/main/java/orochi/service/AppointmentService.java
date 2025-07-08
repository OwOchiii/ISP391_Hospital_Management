package orochi.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import orochi.dto.AppointmentFormDTO;
import orochi.model.Appointment;
import orochi.model.Doctor;
import orochi.model.Patient;
import orochi.model.Specialization;
import orochi.repository.AppointmentRepository;
import orochi.repository.DoctorRepository;
import orochi.repository.PatientRepository;
import orochi.repository.SpecializationRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AppointmentService {

    private static final int APPOINTMENT_DURATION = 30; // minutes

    @Autowired private AppointmentRepository appointmentRepository;
    @Autowired private DoctorRepository doctorRepository;
    @Autowired private PatientRepository patientRepository;
    @Autowired private SpecializationRepository specializationRepository;

    public List<Specialization> getAllSpecializations() {
        return specializationRepository.findAllByOrderBySpecNameAsc();
    }

    public List<Doctor> getDoctorsBySpecialization(Integer specId) {
        return doctorRepository.findBySpecializationId(specId);
    }

    /**
     * Returns booked and unavailable time slots for a given doctor and date.
     */
    public Map<String,List<String>> getDoctorAvailability(
            LocalDate date,
            Integer doctorId,
            Integer excludeAppointmentId  // pass null to fetch all
    ) {
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

        // any slot within 30 minutes of an existing booking is “unavailable”
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

    public Map<String,List<String>> getDoctorAvailability(LocalDate date, Integer doctorId) {
        return getDoctorAvailability(date, doctorId, null);
    }

    @Transactional
    public Appointment bookAppointment(AppointmentFormDTO dto) {
        // validations omitted for brevity...
        Doctor  doc = doctorRepository.findById(dto.getDoctorId())
                .orElseThrow(() -> new RuntimeException("Doctor not found"));
        Patient pat = patientRepository.findById(dto.getPatientId())
                .orElseThrow(() -> new RuntimeException("Patient not found"));

        LocalDateTime when = LocalDateTime.of(
                dto.getAppointmentDate(),
                LocalTime.parse(dto.getAppointmentTime())
        );

        // check availability
        var avail = getDoctorAvailability(dto.getAppointmentDate(), dto.getDoctorId());
        if (avail.get("bookedTimes").contains(dto.getAppointmentTime()) ||
                avail.get("unavailableTimes").contains(dto.getAppointmentTime()))
        {
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

    public Appointment updateAppointmentStatus(Integer appointmentId, String status) {
        Appointment appt = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Not found"));
        appt.setStatus(status);
        return appointmentRepository.save(appt);
    }

    public Page<Appointment> getAllAppointments(Pageable pg) {
        return appointmentRepository.findAll(pg);
    }

    public Page<Appointment> getAppointmentsByDoctorId(Integer docId, Pageable pg) {
        return new org.springframework.data.domain.PageImpl<>(
                appointmentRepository
                        .findByDoctorIdOrderByDateTimeDesc(docId)
                        .subList((int)pg.getOffset(),
                                Math.min((int)pg.getOffset() + pg.getPageSize(),
                                        appointmentRepository.findByDoctorIdOrderByDateTimeDesc(docId).size())),
                pg,
                appointmentRepository.findByDoctorIdOrderByDateTimeDesc(docId).size()
        );
    }

    public Page<Appointment> getAppointmentsByStatus(String status, Pageable pg) {
        return appointmentRepository.findByStatusOrderByDateTimeDesc(status, pg);
    }

    /**
     * Unified period‐counts method used by the controller
     */
    public Map<String, Long> fetchPeriodCounts(
            LocalDate fromDate,
            LocalDate toDate,
            Integer doctorId,    // may be null
            Integer specId,      // may be null
            String status,       // 'ALL' or specific
            String groupBy       // "month", "quarter", "year"
    ) {
        // 1) xác định khoảng thời gian
        LocalDateTime start = fromDate.atStartOfDay();
        LocalDateTime end   = toDate.atTime(23, 59, 59);

        // 2) Lấy tất cả appointment đã filter doctor/spec/status/ngày
        //    Truyền status = 'ALL' để không filter status, else filter đúng status
        String statusFilter = "ALL".equalsIgnoreCase(status) ? "ALL" : status;
        List<Appointment> list = appointmentRepository.findWithFilters(
                doctorId,          // doctorId null = all
                specId,            // specId null = all
                statusFilter,
                /*search*/ null,
                start, end,
                Pageable.unpaged() // lấy toàn bộ
        ).getContent();

        // 3) Stream grouping theo key
        Function<Appointment, String> classifier;
        switch(groupBy.toLowerCase()) {
            case "quarter":
                classifier = a -> {
                    int y = a.getDateTime().getYear();
                    int q = ((a.getDateTime().getMonthValue() - 1) / 3) + 1;
                    return String.format("%d-Q%d", y, q);
                };
                break;
            case "year":
                classifier = a -> String.valueOf(a.getDateTime().getYear());
                break;
            default: // month
                classifier = a -> a.getDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        }

        // 4) Gom nhóm và đếm
        return list.stream()
                .collect(Collectors.groupingBy(
                        classifier,
                        // preserve order: LinkedHashMap
                        LinkedHashMap::new,
                        Collectors.counting()
                ));
    }


    public List<Doctor> getAllDoctors() {
        return doctorRepository.findAll();
    }

    public Page<Appointment> searchAppointmentsByDoctorId(
            Integer doctorId,
            String search,
            Pageable pageable
    ) {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end   = LocalDate.now().plusYears(10).atTime(23,59,59);

        return appointmentRepository.findWithFilters(
                doctorId,                           // doctorId
                null,                               // specId (không lọc chuyên khoa)
                "ALL",                              // status
                (search == null || search.isBlank())? null : search,
                start,
                end,
                pageable
        );
    }

    public List<String> getBookedTimeSlots(LocalDate date, Integer doctorId) {
        // chỉ lấy bookedTimes, không cần unavailableTimes
        return getDoctorAvailability(date, doctorId)
                .get("bookedTimes");
    }

    /**
     * Book mới (được gọi từ controller với các tham số rời rạc).
     */
    @Transactional
    public Appointment bookAppointment(
            Integer patientId,
            Integer doctorId,
            LocalDate appointmentDate,
            String appointmentTime,
            String email,
            String phoneNumber,
            String description) {

        // validate giống trong bookAppointment(dto) của bạn
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

        LocalDateTime dateTime = LocalDateTime.of(appointmentDate, LocalTime.parse(appointmentTime));

        Map<String, List<String>> availability = getDoctorAvailability(appointmentDate, doctorId);
        if (availability.get("bookedTimes").contains(appointmentTime) ||
                availability.get("unavailableTimes").contains(appointmentTime)) {
            throw new RuntimeException("The selected time slot is unavailable. Please choose another time.");
        }

        Appointment appt = new Appointment();
        appt.setPatientId(patientId);
        appt.setDoctorId(doctorId);
        appt.setDateTime(dateTime);
        appt.setStatus("Pending");
        appt.setEmail(email);
        appt.setPhoneNumber(phoneNumber);
        appt.setDescription(description);

        return appointmentRepository.save(appt);
    }

    /**
     * Update (được gọi từ controller khi sửa appointment).
     */
    @Transactional
    public Appointment updateAppointment(
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

        // kiểm tra doctor hợp lệ
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
}
