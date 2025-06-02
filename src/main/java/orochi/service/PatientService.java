package orochi.service;

import orochi.model.Appointment;
import orochi.model.Patient;
import orochi.model.Prescription;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PatientService {
    Optional<Patient> getPatientById(Integer patientId);
    List<Appointment> getUpcomingAppointments(Integer patientId);
    List<Appointment> getAllAppointments(Integer patientId);
    List<Prescription> getActivePrescriptions(Integer patientId);
    Patient savePatient(Patient patient);
}