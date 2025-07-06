// src/main/java/orochi/service/impl/ServiceServiceImpl.java
package orochi.service.impl;

import orochi.model.MedicalService;
import orochi.model.Specialization;
import orochi.repository.ServiceRepository;
import orochi.repository.SpecializationRepository;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ServiceServiceImpl {

    private final ServiceRepository serviceRepository;
    private final SpecializationRepository specializationRepository;

    public ServiceServiceImpl(ServiceRepository serviceRepository,
                              SpecializationRepository specializationRepository) {
        this.serviceRepository = serviceRepository;
        this.specializationRepository = specializationRepository;
    }

    public Page<MedicalService> getServicesPage(int page, int size,
                                                String search, Integer specFilter) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("serviceName"));
        boolean hasText = (search != null && !search.isBlank());
        boolean hasSpec  = (specFilter != null);

        if (hasText && hasSpec) {
            return serviceRepository
                    .findByServiceNameContainingIgnoreCaseAndSpecId(search.trim(), specFilter, pageable);
        } else if (hasText) {
            return serviceRepository
                    .findByServiceNameContainingIgnoreCase(search.trim(), pageable);
        } else if (hasSpec) {
            return serviceRepository
                    .findBySpecId(specFilter, pageable);
        } else {
            return serviceRepository.findAll(pageable);
        }
    }

    public List<Specialization> getAllSpecializations() {
        return specializationRepository.findAll();
    }

    public MedicalService getServiceById(Integer id) {
        return serviceRepository.findById(id).orElse(null);
    }

    public MedicalService saveService(MedicalService svc) {
        return serviceRepository.save(svc);
    }

    public void deleteService(Integer id) {
        serviceRepository.deleteById(id);
    }
}
