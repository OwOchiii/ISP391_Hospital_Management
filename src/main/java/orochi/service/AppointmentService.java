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

    // Standard appointment duration in minutes
    private static final int APPOINTMENT_DURATION = 30;

    public List<Specialization> getAllSpecializations() {
        return specializationRepository.findAllByOrderBySpecNameAsc();
    }

    public List<Doctor> getDoctorsBySpecialization(Integer specId) {
        return doctorRepository.findBySpecializationId(specId);
    }

    /**
     * Get doctor availability information for a specific date
     * @param date The date to check
     * @param doctorId The doctor ID
     * @return Map containing booked and unavailable time slots
     */
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

    /**
     * Get a list of booked time slots for a specific date and doctor
     * @deprecated Use getDoctorAvailability instead for more comprehensive availability information
     */
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
                                     String description, String emergencyContact) {
        // Validate doctor and patient existence
        Doctor doctor = doctorRepository.findById(doctorId)
            .orElseThrow(() -> new RuntimeException("Doctor not found with ID: " + doctorId));

        Patient patient = patientRepository.findById(patientId)
            .orElseThrow(() -> new RuntimeException("Patient not found with ID: " + patientId));

        // Parse time string to LocalTime
        LocalTime time = LocalTime.parse(appointmentTime);

        // Combine date and time into LocalDateTime
        LocalDateTime dateTime = LocalDateTime.of(appointmentDate, time);

        // Check if the appointment slot is available
        Map<String, List<String>> availability = getDoctorAvailability(appointmentDate, doctorId);
        if (availability.get("bookedTimes").contains(appointmentTime) ||
            availability.get("unavailableTimes").contains(appointmentTime)) {
            throw new RuntimeException("The selected time slot is unavailable. Please choose another time.");
        }

        // Create new appointment
        Appointment appointment = new Appointment();
        appointment.setPatientId(patientId);
        appointment.setDoctorId(doctorId);
        appointment.setDateTime(dateTime);
        appointment.setStatus("schedule");
        appointment.setEmail(email);
        appointment.setPhoneNumber(phoneNumber);
        appointment.setDescription(description);

        // Save appointment
        return appointmentRepository.save(appointment);
    }

    public abstract Appointment bookAppointment(AppointmentFormDTO appointmentDTO);

    public List<Appointment> getAppointmentsByDoctorIdAndPatientName(Integer doctorId, String search) {
        return null;
    }

    public abstract Page<Appointment> getAppointmentsByDoctorId(Integer doctorId, Pageable pageable);
}
