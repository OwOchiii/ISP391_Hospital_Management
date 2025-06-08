package orochi.service.impl;

import orochi.model.Service;
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

    public List<Service> getAllServices() {
        return serviceRepository.findAll();
    }

    public Service saveService(Service service) {
        return serviceRepository.save(service);
    }

    public Service getServiceById(Integer serviceId) {
        return serviceRepository.findById(serviceId).orElse(null);
    }

    public void deleteService(Integer serviceId) {
        serviceRepository.deleteById(serviceId);
    }

    public List<Specialization> getAllSpecializations() {
        return specializationRepository.findAll();
    }
}