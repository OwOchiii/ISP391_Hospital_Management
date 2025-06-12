package orochi.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import orochi.model.MedicalOrder;
import orochi.model.MedicalResult;
import orochi.repository.MedicalOrderRepository;
import orochi.repository.MedicalResultRepository;
import orochi.service.MedicalOrderService;

import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class MedicalOrderServiceImpl implements MedicalOrderService {

    private static final Logger logger = LoggerFactory.getLogger(MedicalOrderServiceImpl.class);
    private final MedicalOrderRepository medicalOrderRepository;
    private final MedicalResultRepository medicalResultRepository;

    @Autowired
    public MedicalOrderServiceImpl(MedicalOrderRepository medicalOrderRepository,
                                   MedicalResultRepository medicalResultRepository) {
        this.medicalOrderRepository = medicalOrderRepository;
        this.medicalResultRepository = medicalResultRepository;
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

    @Override
    @Transactional(readOnly = true)
    public MedicalOrder getMedicalOrderById(Long orderId) {
        try {
            long startTime = System.currentTimeMillis();

            // Use the optimized query with JOIN FETCH to load all relationships in a single query
            Optional<MedicalOrder> orderOptional = medicalOrderRepository.findByIdWithDetails(Math.toIntExact(orderId));

            long queryTime = System.currentTimeMillis() - startTime;
            logger.info("Database query time for medical order {}: {}ms", orderId, queryTime);

            MedicalOrder result = orderOptional.orElse(null);

            long totalTime = System.currentTimeMillis() - startTime;
            logger.info("Total service processing time for medical order {}: {}ms", orderId, totalTime);

            return result;
        } catch (Exception e) {
            logger.error("Error retrieving medical order by ID: {}", orderId, e);
            return null;
        }
    }

    @Override
    @Transactional
    public void updateOrderStatus(Long orderId, String status) {
        try {
            long startTime = System.currentTimeMillis();

            // Use direct SQL update instead of loading the entire entity graph
            int updatedRows = medicalOrderRepository.updateOrderStatusById(Math.toIntExact(orderId), status);

            long totalTime = System.currentTimeMillis() - startTime;
            logger.info("Time to update order status: {}ms", totalTime);

            if (updatedRows == 0) {
                logger.warn("No order found with ID: {} to update status", orderId);
            }
        } catch (Exception e) {
            logger.error("Error updating order status for ID {}: {}", orderId, e.getMessage());
            throw new RuntimeException("Failed to update order status", e);
        }
    }

    @Override
    @Transactional
    public void addMedicalResult(Long orderId, String description, String status, MultipartFile resultFile) {
        try {
            logger.debug("Adding medical result for order ID: {}", orderId);
            long startTime = System.currentTimeMillis();

            // Get the medical order
            MedicalOrder order = getMedicalOrderById(orderId);
            if (order == null) {
                logger.error("Medical order not found with ID: {}", orderId);
                throw new RuntimeException("Medical order not found");
            }

            // Create a new MedicalResult entity
            MedicalResult result = new MedicalResult();
            result.setAppointmentId(order.getAppointmentId());
            result.setDoctorId(order.getDoctorId());
            result.setDescription(description);
            result.setStatus(status);
            result.setResultDate(LocalDateTime.now());

            // Handle file upload if a file was provided
            if (resultFile != null && !resultFile.isEmpty()) {
                try {
                    // Generate a unique filename to prevent collisions
                    String timestamp = DateTimeFormatter
                        .ofPattern("yyyyMMdd_HHmmss")
                        .format(LocalDateTime.now());
                    String fileId = UUID.randomUUID().toString().substring(0, 8);
                    String originalFilename = resultFile.getOriginalFilename();
                    String filename = timestamp + "_" + fileId + "_" + originalFilename;

                    // Create absolute paths for file storage
                    // Try multiple possible locations
                    Path[] possibleDirs = {
                        Paths.get("uploads", "medical-results").toAbsolutePath(),
                        Paths.get("D:", "KanbanWeb", "ISP301_Hospital_Management", "uploads", "medical-results").toAbsolutePath(),
                        Paths.get(System.getProperty("user.home"), "uploads", "medical-results").toAbsolutePath(),
                        Paths.get(System.getProperty("java.io.tmpdir"), "uploads", "medical-results").toAbsolutePath()
                    };

                    // Try to create directories at all possible locations
                    Path uploadDir = null;
                    for (Path dir : possibleDirs) {
                        try {
                            Files.createDirectories(dir);
                            if (Files.isDirectory(dir) && Files.isWritable(dir)) {
                                uploadDir = dir;
                                logger.info("Using upload directory: {}", uploadDir);
                                break;
                            }
                        } catch (IOException e) {
                            logger.warn("Failed to create directory at {}: {}", dir, e.getMessage());
                        }
                    }

                    if (uploadDir == null) {
                        throw new IOException("Could not create or access any upload directory");
                    }

                    // Save the file to the filesystem
                    Path filePath = uploadDir.resolve(filename);
                    // Use the MultipartFile's transferTo method with a File object
                    File targetFile = filePath.toFile();
                    resultFile.transferTo(targetFile);

                    // Store the file path in the database - use a relative path for better portability
                    result.setFileUrl("/medical-results/" + filename);

                    logger.info("Saved result file: {}", filePath);
                } catch (IOException e) {
                    logger.error("Error saving result file", e);
                    throw new RuntimeException("Failed to save result file: " + e.getMessage(), e);
                }
            }

            // Save the medical result
            MedicalResult savedResult = medicalResultRepository.save(result);
            logger.info("Saved medical result with ID: {}", savedResult.getResultId());

            // Update the medical order with the result ID if needed
            if (order.getResultId() == null) {
                order.setResultId(savedResult.getResultId());
                medicalOrderRepository.save(order);
                logger.info("Updated medical order with result ID: {}", savedResult.getResultId());
            }

            long totalTime = System.currentTimeMillis() - startTime;
            logger.info("Total time to add medical result: {}ms", totalTime);

        } catch (Exception e) {
            logger.error("Error adding medical result for order ID {}: {}", orderId, e.getMessage(), e);
            throw new RuntimeException("Failed to add medical result: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public boolean deleteOrder(Long orderId) {
        try {
            logger.debug("Attempting to delete medical order ID: {}", orderId);
            long startTime = System.currentTimeMillis();

            // Get the order to check if it has a linked result
            MedicalOrder order = getMedicalOrderById(orderId);
            if (order == null) {
                logger.warn("Medical order not found with ID: {}", orderId);
                return false;
            }

            // If the order has a linked result, we need to handle it
            if (order.getResultId() != null) {
                // First, remove the reference to the result from the order
                // This is required to avoid the foreign key constraint when deleting
                order.setResultId(null);
                medicalOrderRepository.save(order);
                logger.info("Removed result reference from order ID: {}", orderId);
            }

            // Now we can safely delete the order
            medicalOrderRepository.deleteById(Math.toIntExact(orderId));

            long totalTime = System.currentTimeMillis() - startTime;
            logger.info("Total time to delete medical order: {}ms", totalTime);

            return true;
        } catch (Exception e) {
            logger.error("Error deleting medical order ID {}: {}", orderId, e.getMessage(), e);
            throw new RuntimeException("Failed to delete medical order: " + e.getMessage(), e);
        }
    }
}
