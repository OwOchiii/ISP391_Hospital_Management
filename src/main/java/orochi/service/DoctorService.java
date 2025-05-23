package orochi.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import orochi.model.Appointment;
import orochi.model.Patient;
import orochi.repository.AppointmentRepository;
import orochi.repository.PatientRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class DoctorService {

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;

    @Autowired
    public DoctorService(AppointmentRepository appointmentRepository,
                         PatientRepository patientRepository) {
        this.appointmentRepository = appointmentRepository;
        this.patientRepository = patientRepository;
    }

    /**
     * Get all appointments for a doctor
     */
    public List<Appointment> getAppointments(Integer doctorId) {
        return appointmentRepository.findByDoctorIdOrderByDateTimeDesc(doctorId);
    }

    /**
     * Get appointments for a doctor on a specific date
     */
    public List<Appointment> getAppointmentsByDate(Integer doctorId, LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.plusDays(1).atStartOfDay().minusSeconds(1);

        // Using the @Query method since the specific method isn't available
        return appointmentRepository.findUpcomingAppointmentsForDoctor(
                doctorId, startOfDay).stream()
                .filter(a -> a.getDateTime().isBefore(endOfDay))
                .toList();
    }

    /**
     * Get appointments by status (pending, completed, cancelled, etc.)
     */
    public List<Appointment> getAppointmentsByStatus(Integer doctorId, String status) {
        // Need to implement a custom method or use a stream filter since this method doesn't exist
        return appointmentRepository.findByDoctorIdOrderByDateTimeDesc(doctorId).stream()
                .filter(a -> a.getStatus().equals(status))
                .toList();
    }

    /**
     * Get today's appointments for a doctor
     */
    public List<Appointment> getTodayAppointments(Integer doctorId) {
        return appointmentRepository.findTodayAppointmentsForDoctor(doctorId);
    }

    /**
     * Get upcoming appointments for a doctor
     */
    public List<Appointment> getUpcomingAppointments(Integer doctorId) {
        return appointmentRepository.findUpcomingAppointmentsForDoctor(
                doctorId, LocalDateTime.now());
    }

    /**
     * Get appointment details by appointmentId
     */
    public Optional<Appointment> getAppointmentDetails(Integer appointmentId, Integer doctorId) {
        Appointment appointment = appointmentRepository.findByAppointmentIdAndDoctorId(appointmentId, doctorId);
        return Optional.ofNullable(appointment);
    }

    /**
     * Get patient details by patientId
     */
    public Optional<Patient> getPatientDetails(Integer patientId) {
        return patientRepository.findById(patientId);
    }

    /**
     * Get patients with appointments for this doctor
     */
    public List<Patient> getPatientsWithAppointments(Integer doctorId) {
        return patientRepository.findPatientsWithAppointmentsByDoctorId(doctorId);
    }

    /**
     * Search patients by name
     */
    public List<Patient> searchPatientsByName(String name) {
        return patientRepository.findByFullNameContainingIgnoreCase(name);
    }
}