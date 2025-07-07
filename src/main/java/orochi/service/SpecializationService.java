package orochi.service;

import orochi.model.Specialization;

import java.util.List;

public interface SpecializationService {
    List<Specialization> getAll();

    List<Specialization> getAllSpecializations();
}
