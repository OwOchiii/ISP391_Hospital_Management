package orochi.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import orochi.model.Specialization;
import java.math.BigDecimal;
import orochi.repository.DoctorSpecializationRepository;
import orochi.repository.ServiceRepository;
import orochi.repository.SpecializationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Pageable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Service
public class SpecializationServiceImpl {

    private static final Logger logger = LoggerFactory.getLogger(SpecializationServiceImpl.class); // ADDED: Logger for debugging

    @Autowired
    private SpecializationRepository specializationRepository;

    @Autowired
    private DoctorSpecializationRepository doctorSpecializationRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    public List<Specialization> getAllSpecializations() {
        return specializationRepository.findAll();
    }

    public Specialization saveSpecialization(Specialization specialization) {
        if (specialization == null) {
            logger.error("Specialization is null, cannot save"); // ADDED: Log error
            throw new IllegalArgumentException("Specialization cannot be null");
        }
        if (specialization.getSpecName() == null || specialization.getSpecName().trim().isEmpty()) {
            logger.error("Invalid specialty name: {}", specialization.getSpecName()); // ADDED: Log error
            throw new IllegalArgumentException("Specialty name cannot be empty");
        }
        if (specialization.getPrice() == null || specialization.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            logger.error("Invalid price: {}", specialization.getPrice()); // ADDED: Log error
            throw new IllegalArgumentException("Price must be positive");
        }
        logger.info("Saving specialization: {}", specialization); // ADDED: Log before saving
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

    public Page<Specialization> getSpecializationsPage(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("specName").ascending()); // MODIFIED: Added sorting by specName
        return specializationRepository.findAllSpecializations(pageable);
    }
}
