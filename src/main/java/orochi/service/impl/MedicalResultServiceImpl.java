package orochi.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import orochi.model.MedicalResult;
import orochi.repository.MedicalOrderRepository;
import orochi.repository.MedicalResultRepository;
import orochi.service.MedicalResultService;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class MedicalResultServiceImpl implements MedicalResultService {

    private static final Logger logger = LoggerFactory.getLogger(MedicalResultServiceImpl.class);

    private final MedicalResultRepository medicalResultRepository;
    private final MedicalOrderRepository medicalOrderRepository;
    private final JdbcTemplate jdbcTemplate;

    @Value("${file.upload.directory:uploads}")
    private String uploadDir;

    @Autowired
    public MedicalResultServiceImpl(MedicalResultRepository medicalResultRepository,
                                   MedicalOrderRepository medicalOrderRepository,
                                   JdbcTemplate jdbcTemplate) {
        this.medicalResultRepository = medicalResultRepository;
        this.medicalOrderRepository = medicalOrderRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MedicalResult> getResultsForOrder(Long orderId) {
        try {
            logger.debug("Fetching medical results for order ID: {}", orderId);
            long startTime = System.currentTimeMillis();

            // Use the new repository method that returns all results for an order
            List<MedicalResult> results = medicalResultRepository.findAllByOrderId(Math.toIntExact(orderId));

            // If no results found, initialize an empty list
            if (results == null) {
                results = new ArrayList<>();
            }

            long queryTime = System.currentTimeMillis() - startTime;
            logger.info("Time to fetch {} medical results for order {}: {}ms",
                    results.size(), orderId, queryTime);

            return results;
        } catch (Exception e) {
            logger.error("Error fetching medical results for order ID: {}", orderId, e);
            return new ArrayList<>(); // Return empty list on error
        }
    }

    @Override
    @Transactional(readOnly = true)
    public MedicalResult getResultById(Integer resultId) {
        try {
            logger.debug("Fetching medical result with ID: {}", resultId);
            return medicalResultRepository.findById(resultId).orElse(null);
        } catch (Exception e) {
            logger.error("Error fetching medical result with ID: {}", resultId, e);
            return null;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<MedicalResult> getResultsForAppointment(Integer appointmentId) {
        try {
            logger.debug("Fetching medical results for appointment ID: {}", appointmentId);
            return medicalResultRepository.findByAppointmentIdOrderByResultDateDesc(appointmentId);
        } catch (Exception e) {
            logger.error("Error fetching medical results for appointment ID: {}", appointmentId, e);
            return new ArrayList<>(); // Return empty list on error
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<MedicalResult> getResultsByDoctor(Integer doctorId) {
        try {
            logger.debug("Fetching medical results created by doctor ID: {}", doctorId);
            return medicalResultRepository.findByDoctorIdOrderByResultDateDesc(doctorId);
        } catch (Exception e) {
            logger.error("Error fetching medical results for doctor ID: {}", doctorId, e);
            return new ArrayList<>(); // Return empty list on error
        }
    }

    @Override
    @Transactional
    public MedicalResult updateMedicalResult(Integer resultId, String description, String status, MultipartFile resultFile) {
        try {
            logger.debug("Updating medical result with ID: {}", resultId);
            long startTime = System.currentTimeMillis();

            // Get the existing result
            Optional<MedicalResult> resultOptional = medicalResultRepository.findById(resultId);
            if (resultOptional.isEmpty()) {
                logger.error("Medical result not found with ID: {}", resultId);
                throw new RuntimeException("Medical result not found");
            }

            MedicalResult result = resultOptional.get();

            // Update the fields
            result.setDescription(description);
            result.setStatus(status);

            // Handle file upload if a file was provided
            if (resultFile != null && !resultFile.isEmpty()) {
                try {
                    // Delete old file if it exists
                    if (result.getFileUrl() != null && !result.getFileUrl().isEmpty()) {
                        String oldFilePath = result.getFileUrl();
                        if (oldFilePath.startsWith("/")) {
                            oldFilePath = oldFilePath.substring(1);
                        }

                        // Try multiple potential file locations
                        boolean fileDeleted = false;
                        Path[] possiblePaths = {
                            Paths.get(uploadDir, oldFilePath),
                            Paths.get(uploadDir, "medical-results", new File(oldFilePath).getName()),
                            Paths.get("uploads", "medical-results", new File(oldFilePath).getName()),
                            Paths.get("D:/KanbanWeb/ISP301_Hospital_Management/uploads/medical-results", new File(oldFilePath).getName())
                        };

                        for (Path path : possiblePaths) {
                            try {
                                if (Files.deleteIfExists(path)) {
                                    logger.info("Deleted old file: {}", path);
                                    fileDeleted = true;
                                    break;
                                }
                            } catch (IOException e) {
                                logger.warn("Could not delete old file at {}: {}", path, e.getMessage());
                            }
                        }

                        if (!fileDeleted) {
                            logger.warn("Could not find or delete old file: {}", oldFilePath);
                        }
                    }

                    // Generate a unique filename to prevent collisions
                    String timestamp = DateTimeFormatter
                        .ofPattern("yyyyMMdd_HHmmss")
                        .format(LocalDateTime.now());
                    String fileId = UUID.randomUUID().toString().substring(0, 8);
                    String originalFilename = resultFile.getOriginalFilename();
                    String filename = timestamp + "_" + fileId + "_" + originalFilename;

                    // Set up the directory where files will be stored
                    // Ensure both the base directory and its subdirectory exist
                    Path baseDir = Paths.get(uploadDir);
                    if (!Files.exists(baseDir)) {
                        Files.createDirectories(baseDir);
                        logger.info("Created base upload directory: {}", baseDir);
                    }

                    Path uploadPath = baseDir.resolve("medical-results");
                    if (!Files.exists(uploadPath)) {
                        Files.createDirectories(uploadPath);
                        logger.info("Created medical-results directory: {}", uploadPath);
                    }

                    // Save the file to the filesystem
                    Path filePath = uploadPath.resolve(filename);
                    resultFile.transferTo(filePath.toFile());
                    logger.info("Saved updated result file to: {}", filePath.toAbsolutePath());

                    // Store the file path in the database - store the relative path
                    result.setFileUrl("/medical-results/" + filename);
                    logger.info("Set file URL in database to: {}", result.getFileUrl());

                } catch (IOException e) {
                    logger.error("Error saving updated result file", e);
                    throw new RuntimeException("Failed to save updated result file: " + e.getMessage(), e);
                }
            }

            // Save the updated result
            MedicalResult updatedResult = medicalResultRepository.save(result);

            long totalTime = System.currentTimeMillis() - startTime;
            logger.info("Total time to update medical result: {}ms", totalTime);

            return updatedResult;
        } catch (Exception e) {
            logger.error("Error updating medical result with ID {}: {}", resultId, e.getMessage(), e);
            throw new RuntimeException("Failed to update medical result: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public boolean deleteMedicalResult(Integer resultId) {
        try {
            logger.debug("Deleting medical result with ID: {}", resultId);
            long startTime = System.currentTimeMillis();

            // Get the existing result
            Optional<MedicalResult> resultOptional = medicalResultRepository.findById(resultId);
            if (resultOptional.isEmpty()) {
                logger.error("Medical result not found with ID: {}", resultId);
                return false;
            }

            MedicalResult result = resultOptional.get();

            // Delete the file if it exists
            if (result.getFileUrl() != null && !result.getFileUrl().isEmpty()) {
                String fileUrl = result.getFileUrl();
                if (fileUrl.startsWith("/")) {
                    fileUrl = fileUrl.substring(1);
                }

                Path filePath = Paths.get(uploadDir, fileUrl);
                try {
                    Files.deleteIfExists(filePath);
                    logger.info("Deleted file: {}", filePath);
                } catch (IOException e) {
                    logger.warn("Could not delete file: {}", filePath, e);
                    // Continue with the deletion even if we couldn't delete the file
                }
            }

            // Use JDBC to update related orders directly - this avoids Hibernate issues with transient objects
            String updateOrdersSql = "UPDATE MedicalOrder SET resultID = NULL WHERE resultID = ?";
            int updatedRows = jdbcTemplate.update(updateOrdersSql, resultId);
            logger.info("Updated {} order records to remove result reference", updatedRows);

            // Delete the result
            medicalResultRepository.deleteById(resultId);

            long totalTime = System.currentTimeMillis() - startTime;
            logger.info("Total time to delete medical result: {}ms", totalTime);

            return true;
        } catch (Exception e) {
            logger.error("Error deleting medical result with ID {}: {}", resultId, e.getMessage(), e);
            return false;
        }
    }
}
