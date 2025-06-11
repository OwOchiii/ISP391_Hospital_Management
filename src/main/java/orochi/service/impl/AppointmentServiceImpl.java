package orochi.service.impl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import orochi.repository.AppointmentRepository;
import orochi.service.AppointmentMetricService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;

@Service
public class AppointmentMetricServiceImpl implements AppointmentMetricService {
    private final AppointmentRepository appointmentRepository;

    @Autowired
    public AppointmentMetricServiceImpl(AppointmentRepository appointmentRepository) {
        this.appointmentRepository = appointmentRepository;
    }

    @Override
    public Integer getTotalAppointments() {
        try {
            return Math.toIntExact(appointmentRepository.count());
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public Integer getTodayAppointments() {
        try {
            LocalDate today = LocalDate.now();
            LocalDateTime startOfDay = today.atStartOfDay();
            LocalDateTime endOfDay = today.plusDays(1).atStartOfDay().minusSeconds(1);

            // Assuming you'll add a method to find appointments by date range
            // If not available, you may need to add this method to the repository
            long count = appointmentRepository.findAll().stream()
                    .filter(a -> a.getDateTime() != null &&
                            !a.getDateTime().isBefore(startOfDay) &&
                            !a.getDateTime().isAfter(endOfDay))
                    .count();
            return Math.toIntExact(count);
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public Integer getPendingAppointments() {
        try {
            // Assuming your Appointment entity has a status field and "pending" is a valid status
            long count = appointmentRepository.findAll().stream()
                    .filter(a -> "pending".equalsIgnoreCase(a.getStatus()))
                    .count();
            return Math.toIntExact(count);
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public Integer getGrowthPercentage() {
        try {
            // Calculate growth by comparing current month appointments with previous month
            YearMonth currentMonth = YearMonth.now();
            YearMonth previousMonth = currentMonth.minusMonths(1);

            LocalDateTime currentMonthStart = currentMonth.atDay(1).atStartOfDay();
            LocalDateTime currentMonthEnd = currentMonth.atEndOfMonth().plusDays(1).atStartOfDay().minusSeconds(1);

            LocalDateTime previousMonthStart = previousMonth.atDay(1).atStartOfDay();
            LocalDateTime previousMonthEnd = previousMonth.atEndOfMonth().plusDays(1).atStartOfDay().minusSeconds(1);

            long currentMonthCount = appointmentRepository.findAll().stream()
                    .filter(a -> a.getDateTime() != null &&
                            !a.getDateTime().isBefore(currentMonthStart) &&
                            !a.getDateTime().isAfter(currentMonthEnd))
                    .count();

            long previousMonthCount = appointmentRepository.findAll().stream()
                    .filter(a -> a.getDateTime() != null &&
                            !a.getDateTime().isBefore(previousMonthStart) &&
                            !a.getDateTime().isAfter(previousMonthEnd))
                    .count();

            if (previousMonthCount == 0) {
                return currentMonthCount > 0 ? 100 : 0;
            }

            return (int) ((currentMonthCount - previousMonthCount) * 100 / previousMonthCount);
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
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

    @Override
    public Appointment bookAppointment(AppointmentFormDTO appointmentDTO) {
        // Validate doctor and patient existence
        Doctor doctor = doctorRepository.findById(appointmentDTO.getDoctorId())
                .orElseThrow(() -> new RuntimeException("Doctor not found with ID: " + appointmentDTO.getDoctorId()));

        Patient patient = patientRepository.findById(appointmentDTO.getPatientId())
                .orElseThrow(() -> new RuntimeException("Patient not found with ID: " + appointmentDTO.getPatientId()));

        // Parse time string to LocalTime
        LocalTime time = LocalTime.parse(appointmentDTO.getAppointmentTime());

        // Combine date and time into LocalDateTime
        LocalDateTime dateTime = LocalDateTime.of(appointmentDTO.getAppointmentDate(), time);

        // Check if the appointment slot is available
        Map<String, List<String>> availability = getDoctorAvailability(appointmentDTO.getAppointmentDate(), appointmentDTO.getDoctorId());
        if (availability.get("bookedTimes").contains(appointmentDTO.getAppointmentTime()) ||
                availability.get("unavailableTimes").contains(appointmentDTO.getAppointmentTime())) {
            throw new RuntimeException("The selected time slot is unavailable. Please choose another time.");
        }

        // Create new appointment
        Appointment appointment = new Appointment();
        appointment.setPatientId(appointmentDTO.getPatientId());
        appointment.setDoctorId(appointmentDTO.getDoctorId());
        appointment.setDateTime(dateTime);
        appointment.setStatus("Pending");
        appointment.setEmail(appointmentDTO.getEmail());
        appointment.setPhoneNumber(appointmentDTO.getPhoneNumber());
        appointment.setDescription(appointmentDTO.getDescription());

        // Save appointment
        Appointment savedAppointment = appointmentRepository.save(appointment);
        emailService.sendSimpleMessage(
                appointmentDTO.getEmail(),
                "Appointment Confirmation",
                "Your appointment with Dr. " + doctor.getUser().getFullName() +
                        " on " + dateTime + " has been scheduled."
        );
        logger.info("Appointment booked successfully: ID {}", savedAppointment.getAppointmentId());
        return savedAppointment;
    }

    public Page<Appointment> getAppointmentsByStatus(String status, Pageable pageable) {
        return appointmentRepository.findByStatus(status, pageable);
    }

    public Page<Appointment> getAllAppointments(Pageable pageable) {
        return appointmentRepository.findAllWithDetails(pageable);
    }

    public Appointment updateAppointmentStatus(Integer appointmentId, String status) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid appointment ID: " + appointmentId));
        appointment.setStatus(status);
        Appointment updatedAppointment = appointmentRepository.save(appointment);
        emailService.sendSimpleMessage(
                appointment.getEmail(),
                "Appointment Status Update",
                "Your appointment (ID: " + appointmentId + ") has been updated to " + status + "."
        );
        logger.info("Appointment {} status updated to {}", appointmentId, status);
        return updatedAppointment;
    }

    @Override
    public List<Appointment> getAppointmentsByDoctorIdAndPatientName(Integer doctorId, String patientName) {
        if (patientName == null || patientName.isBlank()) {
            return appointmentRepository.findByDoctorIdOrderByDateTimeDesc(doctorId);
        }
        return appointmentRepository.findByDoctorIdAndPatientUserFullNameContainingIgnoreCase(doctorId, patientName.trim());
    }
}
