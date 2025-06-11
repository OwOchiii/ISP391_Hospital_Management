package orochi.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import orochi.service.AppointmentService;
import orochi.service.EmailService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class AppointmentServiceImpl extends AppointmentService {
    private static final Logger logger = LoggerFactory.getLogger(AppointmentServiceImpl.class);

    private final AppointmentRepository appointmentRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final SpecializationRepository specializationRepository;
    private final EmailService emailService;

    // Standard appointment duration in minutes
    private static final int APPOINTMENT_DURATION = 30;

    @Autowired
    public AppointmentServiceImpl(AppointmentRepository appointmentRepository,
                                  DoctorRepository doctorRepository,
                                  PatientRepository patientRepository,
                                  SpecializationRepository specializationRepository,
                                  EmailService emailService) {
        this.appointmentRepository = appointmentRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
        this.specializationRepository = specializationRepository;
        this.emailService = emailService;
    }

    @Override
    public List<Specialization> getAllSpecializations() {
        return specializationRepository.findAllByOrderBySpecNameAsc();
    }

    @Override
    public List<Doctor> getDoctorsBySpecialization(Integer specId) {
        return doctorRepository.findBySpecializationId(specId);
    }

    @Override
    public Map<String, List<String>> getDoctorAvailability(LocalDate date, Integer doctorId) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59);

        List<Appointment> bookedAppointments = appointmentRepository
                .findByDoctorIdAndDateTimeBetweenOrderByDateTime(doctorId, startOfDay, endOfDay);

        // Get the exact booked times
        List<String> bookedTimes = bookedAppointments.stream()
                .map(appointment -> appointment.getDateTime().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm:ss")))
                .collect(Collectors.toList());

        // Get the unavailable times (booked time ± buffer period)
        List<String> unavailableTimes = new ArrayList<>();

        // Create a set of all possible time slots
        Set<String> allTimeSlots = new HashSet<>(Arrays.asList(
                "08:00:00", "08:30:00", "09:00:00", "09:30:00", "10:00:00", "10:30:00",
                "11:00:00", "11:30:00", "14:00:00", "14:30:00", "15:00:00", "15:30:00",
                "16:00:00", "16:30:00", "17:00:00", "17:30:00"
        ));

        // For each booked appointment, mark surrounding time slots as unavailable
        for (Appointment appointment : bookedAppointments) {
            LocalTime appointmentTime = appointment.getDateTime().toLocalTime();

            // Mark time slots within 30 minutes as unavailable
            for (String timeSlot : allTimeSlots) {
                LocalTime slotTime = LocalTime.parse(timeSlot);

                // Calculate time difference in minutes
                int minutesDifference = Math.abs(
                        (slotTime.getHour() * 60 + slotTime.getMinute()) -
                                (appointmentTime.getHour() * 60 + appointmentTime.getMinute())
                );

                // If the time slot is within 30 minutes of a booked appointment
                // and it's not the exact booked time (which is in bookedTimes)
                if (minutesDifference < APPOINTMENT_DURATION && !bookedTimes.contains(timeSlot)) {
                    unavailableTimes.add(timeSlot);
                }
            }
        }

        // Remove duplicates
        unavailableTimes = unavailableTimes.stream().distinct().collect(Collectors.toList());

        // Create result map
        Map<String, List<String>> result = new HashMap<>();
        result.put("bookedTimes", bookedTimes);
        result.put("unavailableTimes", unavailableTimes);

        return result;
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