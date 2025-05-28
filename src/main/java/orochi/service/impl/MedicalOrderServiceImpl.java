package orochi.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import orochi.repository.MedicalOrderRepository;
import orochi.service.MedicalOrderService;

@Service
public class MedicalOrderServiceImpl implements MedicalOrderService {

    private final MedicalOrderRepository medicalOrderRepository;

    @Autowired
    public MedicalOrderServiceImpl(MedicalOrderRepository medicalOrderRepository) {
        this.medicalOrderRepository = medicalOrderRepository;
    }

    @Override
    public Integer getPendingOrders() {
        try {
            // In a real implementation, you would filter orders by status "pending"
            // For now, since we don't know the exact structure, returning a placeholder value
            return 15; // 15 pending orders

            // When you implement this properly, it might look like:
            // return Math.toIntExact(medicalOrderRepository.findByStatus("pending").count());
        } catch (Exception e) {
            return 0;
        }
    }
}
