package orochi.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import orochi.model.Appointment;
import orochi.model.Patient;
import orochi.model.Users;
import orochi.repository.AppointmentRepository;
import orochi.repository.PatientRepository;
import orochi.repository.ReceptionistRepository;
import orochi.repository.UserRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;

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
        Users patientRole = receptionistRepository.findByEmail("PATIENT")
                .orElseThrow(() -> new RuntimeException("Patient role not found"));
        user.setRoleId(patientRole.getRoleId());
        user.setGuest(false);
        user.setStatus("Active");
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

    // Update appointment status
    public boolean updateAppointmentStatus(Integer appointmentId, String status) {
        Optional<Appointment> appointmentOptional = appointmentRepository.findById(appointmentId);
        if (appointmentOptional.isPresent()) {
            Appointment appointment = appointmentOptional.get();
            if (!isValidAppointmentStatus(status)) {
                throw new IllegalArgumentException("Invalid appointment status: " + status);
            }
            appointment.setStatus(status);
            appointmentRepository.save(appointment);
            return true;
        }
        return false;
    }

    private boolean isValidAppointmentStatus(String status) {
        return status != null && (status.equals("Scheduled") || status.equals("Completed") ||
                status.equals("Cancel") || status.equals("Pending"));
    }

    // Fetch all Receptionists with pagination and filtering
    public Page<Users> getAllReceptionists(Pageable pageable) {
        return receptionistRepository.findAllReceptionistsFiltered(null, null, pageable);
    }

    public Page<Users> getAllReceptionists(String search, String statusFilter, Pageable pageable) {
        return receptionistRepository.findAllReceptionistsFiltered(search, statusFilter, pageable);
    }

//    public int newPatients(){
//        return receptionistRepository.newPatients();
//    }

    public int ourDoctors(){
        return receptionistRepository.ourDoctors();
    }

    public int totalAppointment(){
        return receptionistRepository.totalAppointments();
    }

    public int activeStaff(){
        return receptionistRepository.activeStaff();
    }

    public int newPatients(){
        return receptionistRepository.newPatients();
    }

    public Map<String, Object> getPatientStatusChartData(String period) {

        List<Object[]> results = new ArrayList<>();

        switch (period) {
            case "day" -> results = receptionistRepository.getPatientStatsByDay();
            case "month" -> results = receptionistRepository.getPatientStatsByMonth();
            case "year" -> results = receptionistRepository.getPatientStatsByYear();
            default -> results = receptionistRepository.getPatientStatsByMonth();
        }

        List<String> labels = new ArrayList<>();
        List<Integer> data = new ArrayList<>();

        for (Object[] row : results) {
            labels.add((String) row[0]);  // yyyy-MM t? SQL Server tr? v?
            data.add(((Number) row[1]).intValue());
        }

        Map<String, Object> response = new HashMap<>();
        response.put("labels", labels);
        response.put("data", data);

        return response;
    }

    public List<Map<String, Object>> getAppointmentTableData() {
        // G?i repository ?? l?y d? li?u
        return receptionistRepository.fetchAppointmentTableData();
    }

    @Transactional
    public boolean confirmAppointment(Integer appointmentId) {
        return receptionistRepository.confirmAppointment(appointmentId) > 0;
    }

}