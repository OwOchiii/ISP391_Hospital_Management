package orochi.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import orochi.model.Specialization;
import orochi.repository.SpecializationRepository;
import orochi.service.SpecializationService;

import java.util.List;

@Service
public class SpecializationImpl implements SpecializationService {

    private final SpecializationRepository specializationRepository;

    @Autowired
    public SpecializationImpl(SpecializationRepository specializationRepository) {
        this.specializationRepository = specializationRepository;
    }

    @Override
    public List<Specialization> getAll() {
        return specializationRepository.findAll();
    }

    @Override
    public List<Specialization> getAllSpecializations() {
        return List.of();
    }
}
