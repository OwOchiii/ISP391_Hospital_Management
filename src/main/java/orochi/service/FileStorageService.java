package orochi.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Logger logger = LoggerFactory.getLogger(FileStorageService.class);

    @Value("${file.upload.directory:upload-dir}")
    private String uploadDirectory;

    @Value("${file.base.url:http://localhost:8080/files}")
    private String baseUrl;

    /**
     * Stores a file in the specified subdirectory and returns the file URL
     *
     * @param file The file to store
     * @param subDirectory The subdirectory to store the file in (e.g., "medical-results")
     * @return The URL of the stored file
     * @throws IOException If an I/O error occurs
     */
    public String storeFile(MultipartFile file, String subDirectory) throws IOException {
        // Validate file
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty or null");
        }

        // Create directories if they don't exist
        String directoryPath = uploadDirectory + File.separator + subDirectory;
        Path directory = Paths.get(directoryPath);
        if (!Files.exists(directory)) {
            Files.createDirectories(directory);
            logger.info("Created directory: {}", directory);
        }

        // Generate a unique filename to avoid collisions
        String originalFilename = file.getOriginalFilename();
        String fileExtension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        // Format: timestamp_uuid_originalname.extension
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String uniqueID = UUID.randomUUID().toString().substring(0, 8);
        String safeOriginalName = originalFilename != null ?
                originalFilename.replaceAll("[^a-zA-Z0-9.-]", "_") : "file";

        String newFilename = timestamp + "_" + uniqueID + "_" + safeOriginalName;

        // Store the file
        Path filePath = directory.resolve(newFilename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        logger.info("Stored file: {} at path: {}", originalFilename, filePath);

        // Return the URL to access the file
        return baseUrl + "/" + subDirectory + "/" + newFilename;
    }

    /**
     * Deletes a file from storage
     *
     * @param fileUrl The URL of the file to delete
     * @return true if the file was deleted successfully, false otherwise
     */
    public boolean deleteFile(String fileUrl) {
        try {
            if (fileUrl == null || !fileUrl.startsWith(baseUrl)) {
                logger.warn("Invalid file URL: {}", fileUrl);
                return false;
            }

            // Extract the path from the URL
            String relativePath = fileUrl.substring(baseUrl.length());
            Path filePath = Paths.get(uploadDirectory + relativePath);

            // Delete the file
            boolean result = Files.deleteIfExists(filePath);
            if (result) {
                logger.info("Deleted file: {}", filePath);
            } else {
                logger.warn("File not found for deletion: {}", filePath);
            }

            return result;
        } catch (IOException e) {
            logger.error("Error deleting file: {}", fileUrl, e);
            return false;
        }
    }
}
