package orochi.service.impl;

import orochi.model.Specialization;
import orochi.repository.DoctorSpecializationRepository;
import orochi.repository.ServiceRepository;
import orochi.repository.SpecializationRepository;
import orochi.repository.DoctorRepository;
import orochi.model.Doctor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Pageable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.List;

@Service
public class SpecializationServiceImpl {

    private static final Logger logger = LoggerFactory.getLogger(SpecializationServiceImpl.class);

    @Autowired
    private SpecializationRepository specializationRepository;

    @Autowired
    private DoctorSpecializationRepository doctorSpecializationRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    public Page<Specialization> getSpecializationsPage(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("specName").ascending());
        return specializationRepository.findAllSpecializations(pageable);
    }

    public Page<Specialization> getSpecializationsPage(
            int page, int size, String search, String symptomFilter) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("specName").ascending());

        String qName = (search == null ? "" : search.trim());
        String qSymptom = (symptomFilter == null ? "" : symptomFilter.trim());
        if (qName.isEmpty() && qSymptom.isEmpty()) {
            return specializationRepository.findAllSpecializations(pageable);
        }
        return specializationRepository
                .findBySpecNameContainingIgnoreCaseAndSymptomContainingIgnoreCase(qName, qSymptom, pageable);
    }

    public List<String> getAllDistinctSymptoms() {
        return specializationRepository.findDistinctSymptoms();
    }

    public Specialization saveSpecialization(Specialization specialization) {
        if (specialization == null) {
            throw new IllegalArgumentException("Specialization cannot be null");
        }
        logger.info("Saving specialization: {}", specialization);
        return specializationRepository.save(specialization);
    }

    public Specialization getSpecializationById(Integer specId) {
        return specializationRepository.findById(specId).orElse(null);
    }

    @Transactional
    public void deleteSpecialization(Integer specId) {
        doctorSpecializationRepository.deleteBySpecId(specId);
        serviceRepository.deleteBySpecId(specId);
        specializationRepository.deleteById(specId);
    }

    public Page<Doctor> getDoctorsBySpecializationId(Integer specId, int page, int size, String search) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("user.fullName").ascending());

        if (search != null && !search.trim().isEmpty()) {
            Page<Object[]> resultPage = doctorRepository.findBySpecializationIdAndUserFullNameContainingIgnoreCaseWithUserName(
                    specId, search.trim(), pageable);
            return resultPage.map(objects -> (Doctor) objects[0]);
        } else {
            Page<Object[]> resultPage = doctorRepository.findBySpecializationIdWithUserName(specId, pageable);
            return resultPage.map(objects -> (Doctor) objects[0]);
        }
    }

    public List<Doctor> getAvailableDoctorsForSpecialization(Integer specId) {
        return doctorRepository.findDoctorsNotInSpecialization(specId);
    }

    @Transactional
    public void addDoctorToSpecialization(Integer doctorId, Integer specId) {
        // Verify doctor and specialization exist
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new IllegalArgumentException("Doctor not found"));
        Specialization specialization = specializationRepository.findById(specId)
                .orElseThrow(() -> new IllegalArgumentException("Specialization not found"));

        // Check if the relationship already exists
        if (doctor.getSpecializations().contains(specialization)) {
            throw new IllegalArgumentException("Doctor is already assigned to this specialization");
        }

        // Add the specialization to the doctor's list
        doctor.getSpecializations().add(specialization);
        doctorRepository.save(doctor);

        logger.info("Added doctor {} to specialization {}", doctorId, specId);
    }

    @Transactional
    public void removeDoctorFromSpecialization(Integer doctorId, Integer specId) {
        // Verify doctor and specialization exist
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new IllegalArgumentException("Doctor not found"));
        Specialization specialization = specializationRepository.findById(specId)
                .orElseThrow(() -> new IllegalArgumentException("Specialization not found"));

        // Check if the relationship exists
        if (!doctor.getSpecializations().contains(specialization)) {
            throw new IllegalArgumentException("Doctor is not assigned to this specialization");
        }

        // Remove the specialization from the doctor's list
        doctor.getSpecializations().remove(specialization);
        doctorRepository.save(doctor);

        logger.info("Removed doctor {} from specialization {}", doctorId, specId);
    }
}
