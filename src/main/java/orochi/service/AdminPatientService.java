// src/main/java/orochi/service/PatientService.java
package orochi.service;

import orochi.model.Patient;
import orochi.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import orochi.model.Users;
import orochi.repository.UserRepository;

import java.util.Collections;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class AdminPatientService {
    private final PatientRepository patientRepository;
    private final UserRepository userRepository;

    public List<Patient> getAllPatients() {
        try {
            return patientRepository.findAll();
        } catch (DataAccessException e) {
            log.error("Failed to fetch all patients", e);
            return Collections.emptyList();
        }
    }

    public Patient getPatientById(int id) {
        return patientRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid patient Id: " + id));
    }

    public Patient savePatient(Patient p) {
        return patientRepository.save(p);
    }

    public void togglePatientLock(int id) {
        // Láº¥y Patient vÃ  liÃªn káº¿t Ä‘áº¿n Users
        Patient p = getPatientById(id);
        Users user = p.getUser(); // Patient entity pháº£i map tá»›i Users
        // Äá»•i tráº¡ng thÃ¡i trÃªn Users
        String newStatus = "LOCKED".equals(user.getStatus()) ? "ACTIVE" : "LOCKED";
        user.setStatus(newStatus);
        userRepository.save(user);
    }

    public List<Patient> searchPatients(String search, String statusFilter) {
        // náº¿u param trá»‘ng thÃ¬ truyá»n null Ä‘á»ƒ query khÃ´ng filter
        String s = (search  != null && !search.isBlank()) ? search.trim() : null;
        String st = (statusFilter != null && !statusFilter.isBlank()) ? statusFilter : null;
        try {
            return patientRepository.searchPatients(s, st);
        } catch (DataAccessException e) {
            log.error("Failed to search/filter patients", e);
            return Collections.emptyList();
        }
    }
}