package orochi.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import orochi.model.Appointment;
import orochi.model.Patient;
import orochi.model.PatientForm;
import orochi.model.Prescription;
import orochi.model.Users;
import orochi.repository.AppointmentRepository;
import orochi.repository.PatientRepository;
import orochi.repository.PrescriptionRepository;
import orochi.repository.UserRepository;
import orochi.service.PatientService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class PatientServiceImpl implements PatientService {

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private PrescriptionRepository prescriptionRepository;

    // *** Bổ sung ***
    @Autowired
    private UserRepository userRepository;

    // --- Các phương thức cũ không đổi ---
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
        // Giữ nguyên logic cũ
        return prescriptionRepository.findByPatientIdAndPrescriptionDateAfter(
                patientId, LocalDateTime.now().minusDays(30));
    }

    @Override
    public Patient savePatient(Patient patient) {
        return patientRepository.save(patient);
    }

    // --- Các phương thức mới cho Admin UI ---

    /**
     * Search & phân trang bệnh nhân theo tên/email/status
     */
    public Page<Patient> searchPatients(String search, String statusFilter, int page, int size) {
        String s  = (search       != null && !search.isBlank())       ? search.trim()       : null;
        String st = (statusFilter != null && !statusFilter.isBlank())? statusFilter.trim(): null;
        Pageable pg = PageRequest.of(page, size);
        return patientRepository.searchPatients(s, st, pg);
    }

    /**
     * Load dữ liệu vào PatientForm để edit
     */
    public PatientForm loadForm(Integer patientId) {
        Patient p = patientRepository.findById(patientId)
                .orElseThrow(() -> new IllegalArgumentException("Patient not found: " + patientId));
        Users u = p.getUser();

        PatientForm f = new PatientForm();
        // Thông tin chung (Users)
        f.setPatientId   (p.getPatientId());
        f.setUserId      (u.getUserId());
        f.setFullName    (u.getFullName());
        f.setEmail       (u.getEmail());
        f.setPhoneNumber (u.getPhoneNumber());
        f.setStatus      (u.getStatus());
        // Thông tin Patient-specific
        f.setDateOfBirth (p.getDateOfBirth());
        f.setGender      (p.getGender());
        f.setDescription (p.getDescription());
        return f;
    }

    /**
     * Lưu hoặc cập nhật cả Users + Patient từ PatientForm,
     * có xử lý tạo mới vs edit, tránh duplicate key.
     */
    public void saveFromForm(PatientForm form) {
        Users u;
        Patient existing = null;

        // 1) Users: edit hay add mới?
        if (form.getPatientId() != null) {
            existing = patientRepository.findById(form.getPatientId())
                    .orElseThrow(() -> new IllegalArgumentException("Patient not found: " + form.getPatientId()));
            u = existing.getUser();
        } else {
            u = new Users();
            u.setRoleId(3);              // 3 = ROLE_PATIENT
            u.setGuest(false);
            u.setCreatedAt(LocalDateTime.now());
        }

        // cập nhật chung
        u.setFullName   (form.getFullName());
        u.setEmail      (form.getEmail());
        u.setPhoneNumber(form.getPhoneNumber());
        u.setStatus     (form.getStatus());
        userRepository.save(u);

        // 2) Patient: edit hay add mới?
        Patient p = (existing != null) ? existing : new Patient();
        p.setUserId      (u.getUserId());
        p.setDateOfBirth (form.getDateOfBirth());
        p.setGender      (form.getGender());
        p.setDescription (form.getDescription());
        patientRepository.save(p);
    }
}
