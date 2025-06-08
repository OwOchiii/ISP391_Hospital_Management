package orochi.service.impl;

import orochi.model.MedicalService;
import orochi.model.Specialization;
import orochi.repository.ServiceRepository;
import orochi.repository.SpecializationRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ServiceServiceImpl {

    private final ServiceRepository serviceRepository;
    private final SpecializationRepository specializationRepository;

    public ServiceServiceImpl(ServiceRepository serviceRepository, SpecializationRepository specializationRepository) {
        this.serviceRepository = serviceRepository;
        this.specializationRepository = specializationRepository;
    }

    public List<MedicalService> getAllServices() {
        return serviceRepository.findAll();
    }

    public MedicalService saveService(MedicalService service) {
        return serviceRepository.save(service);
    }

    public MedicalService getServiceById(Integer serviceId) {
        return serviceRepository.findById(serviceId).orElse(null);
    }

    public void deleteService(Integer serviceId) {
        serviceRepository.deleteById(serviceId);
    }

    public List<Specialization> getAllSpecializations() {
        return specializationRepository.findAll();
    }
}