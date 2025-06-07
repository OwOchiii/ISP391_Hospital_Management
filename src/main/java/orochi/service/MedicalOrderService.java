package orochi.service;

import orochi.model.MedicalOrder;
import org.springframework.web.multipart.MultipartFile;

public interface MedicalOrderService {
    Integer getPendingOrders();

    MedicalOrder getMedicalOrderById(Long orderId);

    void updateOrderStatus(Long orderId, String status);
    
    void addMedicalResult(Long orderId, String description, String status, MultipartFile resultFile);

    boolean deleteOrder(Long orderId);
}
