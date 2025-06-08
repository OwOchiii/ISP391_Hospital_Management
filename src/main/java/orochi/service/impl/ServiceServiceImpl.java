package orochi.service.impl;

import orochi.model.Service;
import orochi.model.Specialization;
import orochi.repository.ServiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ServiceServiceImpl {

    @Autowired
    private ServiceRepository serviceRepository;

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

    // Lấy danh sách Specialization để hiển thị trong dropdown
    public List<Specialization> getAllSpecializations() {
        return null; // Bạn cần triển khai SpecializationRepository nếu muốn dùng
    }
}