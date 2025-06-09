package orochi.service.impl;

import org.springframework.stereotype.Service;
import orochi.model.Appointment;
import orochi.model.Patient;
import orochi.model.Role;
import orochi.model.Users;
import orochi.repository.AppointmentRepository;
import orochi.repository.PatientRepository;
import orochi.repository.ReceptionistRepository;
import orochi.repository.UserRepository;

import java.util.List;

@Service
public class ReceptionistService {

    private final UserRepository userRepository;
    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final ReceptionistRepository receptionistRepository;

    public ReceptionistService(
            UserRepository userRepository,
            AppointmentRepository appointmentRepository,
            PatientRepository patientRepository,
            ReceptionistRepository receptionistRepository) {
        this.userRepository = userRepository;
        this.appointmentRepository = appointmentRepository;
        this.patientRepository = patientRepository;
        this.receptionistRepository = receptionistRepository;
    }

    // Fetch all appointments for scheduling purposes
    public List<Appointment> getAllAppointments() {
        return appointmentRepository.findAll();
    }

    // Register a new patient
    public Users registerPatient(Users user) {
        // Set the role to PATIENT (RoleID = 4 based on your SQL insertion order)
        Users patientRole = receptionistRepository.findByEmail("PATIENT")
                .orElseThrow(() -> new RuntimeException("Patient role not found"));
        user.setRoleId(patientRole.getRoleId());
        user.setGuest(false);
        user.setStatus("Active"); // Set default status as per your Users model

        // Save the user (this will trigger the trg_AddPatientOnUserCreation trigger to create a Patient entry)
        return userRepository.save(user);
    }

    // Fetch all patients for registration management
    public List<Patient> getAllPatients() {
        return patientRepository.findAll();
    }

    // Check if the user is a Receptionist
    public boolean isReceptionist(Users user) {
        return user.getRole().getRoleName().equals("RECEPTIONIST");
    }
}