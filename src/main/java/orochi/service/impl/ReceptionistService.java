import orochi.model.Appointment;
import orochi.model.Doctor;
import orochi.model.Patient;

import java.time.LocalDateTime;
import java.util.List;

public List<Appointment> getAppointmentsByStatus(String status) {
    return appointmentRepository.findByStatus(status);
}

public List<Doctor> getAllDoctors() {
    return doctorRepository.findAll();
}

public Patient getPatientById(Integer patientId) {
    return patientRepository.findById(patientId)
            .orElseThrow(() -> new RuntimeException("Patient not found"));
}

public long countAppointmentsByPeriod(String period) {
    LocalDateTime start;
    switch (period) {
        case "month":
            start = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
            break;
        case "year":
            start = LocalDateTime.now().withMonth(1).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
            break;
        default:
            return 0;
    }
    return appointmentRepository.countByDateTimeAfter(start);
}