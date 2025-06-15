package orochi.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import orochi.model.Specialization;
import orochi.repository.DoctorSpecializationRepository;
import orochi.repository.ServiceRepository;
import orochi.repository.SpecializationRepository;

import java.util.List;

@Service
public class SpecializationServiceImpl {

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
}