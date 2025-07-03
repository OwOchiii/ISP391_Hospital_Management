package orochi.service;

import orochi.model.Appointment;
import orochi.model.Patient;
import orochi.model.PatientForm;
import orochi.model.Prescription;
import org.springframework.data.domain.Page;


import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PatientService {
    Optional<Patient> getPatientById(Integer patientId);
    List<Appointment> getUpcomingAppointments(Integer patientId);
    List<Appointment> getAllAppointments(Integer patientId);
    List<Prescription> getActivePrescriptions(Integer patientId);
    Patient savePatient(Patient patient);
    Page<Patient> searchPatients(String search, String statusFilter, int page, int size);
    PatientForm loadForm(Integer patientId);
    void saveFromForm(PatientForm form);
}