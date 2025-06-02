package orochi.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import orochi.model.Appointment;
import orochi.model.Patient;
import orochi.model.Prescription;
import orochi.repository.AppointmentRepository;
import orochi.repository.PatientRepository;
import orochi.repository.PrescriptionRepository;
import orochi.service.PatientService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class PatientServiceImpl implements PatientService {

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private PrescriptionRepository prescriptionRepository;

    @Override
    public Optional<Patient> getPatientById(Integer patientId) {
        return patientRepository.findById(patientId);
    }

    @Override
    public List<Appointment> getUpcomingAppointments(Integer patientId) {
        return appointmentRepository.findByPatientIdAndDateTimeAfterOrderByDateTime(
                patientId, LocalDateTime.now());
    }

    @Override
    public List<Appointment> getAllAppointments(Integer patientId) {
        return appointmentRepository.findByPatientIdOrderByDateTimeDesc(patientId);
    }

    @Override
    public List<Prescription> getActivePrescriptions(Integer patientId) {
        // Find prescriptions that are still active (not expired)
        return prescriptionRepository.findByPatientIdAndPrescriptionDateAfter(
                patientId, LocalDateTime.now().minusDays(30)); // Assuming prescriptions are active for 30 days
    }

    @Override
    public Patient savePatient(Patient patient) {
        return patientRepository.save(patient);
    }
}