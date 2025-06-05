package orochi.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

    private static final int APPOINTMENT_DURATION = 30;

    public List<Specialization> getAllSpecializations() {
        return specializationRepository.findAllByOrderBySpecNameAsc();
    }

    public List<Doctor> getDoctorsBySpecialization(Integer specId) {
        return doctorRepository.findBySpecializationId(specId);
    }

    public Map<String, List<String>> getDoctorAvailability(LocalDate date, Integer doctorId, Integer excludeAppointmentId) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59);

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

        List<String> unavailableTimes = new ArrayList<>();
        Set<String> allTimeSlots = new HashSet<>(Arrays.asList(
                "08:00:00", "08:30:00", "09:00:00", "09:30:00", "10:00:00", "10:30:00",
                "11:00:00", "11:30:00", "14:00:00", "14:30:00", "15:00:00", "15:30:00",
                "16:00:00", "16:30:00", "17:00:00", "17:30:00"
        ));

        for (Appointment appointment : bookedAppointments) {
            LocalTime appointmentTime = appointment.getDateTime().toLocalTime();
            for (String timeSlot : allTimeSlots) {
                LocalTime slotTime = LocalTime.parse(timeSlot);
                int minutesDifference = Math.abs(
                        (slotTime.getHour() * 60 + slotTime.getMinute()) -
                                (appointmentTime.getHour() * 60 + appointmentTime.getMinute())
                );
                if (minutesDifference < APPOINTMENT_DURATION && !bookedTimes.contains(timeSlot)) {
                    unavailableTimes.add(timeSlot);
                }
            }
        }

        unavailableTimes = unavailableTimes.stream().distinct().collect(Collectors.toList());
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
                                       String description, String emergencyContact) {
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
        appointment.setStatus("Scheduled");
        appointment.setEmail(email);
        appointment.setPhoneNumber(phoneNumber);
        appointment.setDescription(description);

        return appointmentRepository.save(appointment);
    }

    @Transactional
    public Appointment updateAppointment(Integer appointmentId, Integer patientId, Integer doctorId,
                                         LocalDate appointmentDate, String appointmentTime,
                                         String email, String phoneNumber, String description,
                                         String emergencyContact) {
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
}