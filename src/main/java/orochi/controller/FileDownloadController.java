package orochi.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import orochi.model.MedicalReport;
import orochi.model.MedicalResult;
import orochi.repository.MedicalReportRepository;
import orochi.repository.MedicalResultRepository;

import java.io.File;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

@Controller
@RequestMapping("/download")
public class FileDownloadController {

    private static final Logger logger = LoggerFactory.getLogger(FileDownloadController.class);

    @Value("${file.upload.directory:D:/KanbanWeb/ISP301_Hospital_Management/uploads}")
    private String uploadDir;

    @Autowired
    private MedicalReportRepository medicalReportRepository;

    @Autowired
    private MedicalResultRepository medicalResultRepository;

    /**
     * Download a report by report ID
     * @param reportId The ID of the report to download
     * @param download If true, download the file; if false (default), view it inline
     */
    @GetMapping("/report/{reportId:\\d+}")
    @ResponseBody
    public ResponseEntity<Resource> downloadReportById(
            @PathVariable Integer reportId,
            @RequestParam(required = false, defaultValue = "false") boolean download) {
        try {
            // Get the report from the database
            Optional<MedicalReport> reportOpt = medicalReportRepository.findById(reportId);
            if (reportOpt.isEmpty()) {
                logger.error("Report not found for ID: {}", reportId);
                return ResponseEntity.notFound().build();
            }

            MedicalReport report = reportOpt.get();
            String fileUrl = report.getFileUrl();

            // Check if fileUrl is null or empty
            if (fileUrl == null || fileUrl.isEmpty()) {
                logger.error("No file URL for report ID: {}", reportId);
                return ResponseEntity.notFound().build();
            }

            // Extract the filename from the URL
            String fileName = fileUrl;
            if (fileUrl.contains("/")) {
                fileName = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
            }

            return serveFile(fileName, !download); // Invert the boolean - inline is now default
        } catch (Exception e) {
            logger.error("Error downloading report by ID: {}", reportId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Download a report file directly by filename
     * @param fileName The name of the file to download
     * @param download If true, download the file; if false (default), view it inline
     */
    @GetMapping("/report/{fileName:.+\\.pdf}")
    @ResponseBody
    public ResponseEntity<Resource> downloadReportByFileName(
            @PathVariable String fileName,
            @RequestParam(required = false, defaultValue = "false") boolean download) {
        try {
            logger.info("Requested file by name: {}", fileName);
            return serveFile(fileName, !download); // Invert the boolean - inline is now default
        } catch (Exception e) {
            logger.error("Error serving file by filename: {}", fileName, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Download a medical result file directly by filename
     * @param filename The name of the file to download
     * @param inline If true, view the file inline; if false (default), download the file
     */
    @GetMapping("/medical-results/{filename:.+}")
    @ResponseBody
    public ResponseEntity<Resource> downloadMedicalResultByFileName(
            @PathVariable String filename,
            @RequestParam(required = false, defaultValue = "false") boolean inline) {
        try {
            logger.info("Requested medical result file: {}", filename);

            // If the filename contains a full URL, extract just the filename part
            if (filename.contains("http://") || filename.contains("https://")) {
                // Extract the path part after the last slash in the URL
                String[] parts = filename.split("/");
                filename = parts[parts.length - 1];
                logger.info("Extracted filename from full URL: {}", filename);
            } else if (filename.contains("/")) {
                filename = filename.substring(filename.lastIndexOf("/") + 1);
                logger.info("Extracted filename from path: {}", filename);
            }

            return serveFile(filename, inline);
        } catch (Exception e) {
            logger.error("Error serving medical result file: {}", filename, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Download a medical result file by result ID
     * @param resultId The ID of the medical result to download the file for
     * @param inline If true, view the file inline; if false (default), download the file
     */
    @GetMapping("/medical-result/{resultId:\\d+}")
    @ResponseBody
    public ResponseEntity<Resource> downloadMedicalResultById(
            @PathVariable Integer resultId,
            @RequestParam(required = false, defaultValue = "false") boolean inline) {
        try {
            logger.info("Requested medical result file by ID: {}", resultId);

            // Get the medical result from the database
            Optional<MedicalResult> resultOpt = medicalResultRepository.findById(resultId);
            if (resultOpt.isEmpty()) {
                logger.error("Medical result not found for ID: {}", resultId);
                return ResponseEntity.notFound().build();
            }

            MedicalResult result = resultOpt.get();
            String fileUrl = result.getFileUrl();

            // Check if fileUrl is null or empty
            if (fileUrl == null || fileUrl.isEmpty()) {
                logger.error("No file URL for medical result ID: {}", resultId);
                return ResponseEntity.notFound().build();
            }

            // Extract the filename from the URL
            String fileName = fileUrl;
            if (fileUrl.contains("/")) {
                fileName = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
            }

            logger.info("Extracted filename '{}' from medical result ID: {}", fileName, resultId);
            return serveFile(fileName, inline);
        } catch (Exception e) {
            logger.error("Error downloading medical result by ID: {}", resultId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Download a support ticket attachment by filename
     * @param filename The name of the file to download
     * @param inline If true, view the file inline; if false (default), download the file
     */
    @GetMapping("/support-tickets/{filename:.+}")
    @ResponseBody
    public ResponseEntity<Resource> downloadSupportTicketAttachment(
            @PathVariable String filename,
            @RequestParam(required = false, defaultValue = "false") boolean inline) {
        try {
            logger.info("Requested support ticket attachment: {}", filename);

            // If the filename contains a full URL, extract just the filename part
            if (filename.contains("http://") || filename.contains("https://")) {
                // Extract the path part after the last slash in the URL
                String[] parts = filename.split("/");
                filename = parts[parts.length - 1];
                logger.info("Extracted filename from full URL: {}", filename);
            } else if (filename.contains("/")) {
                filename = filename.substring(filename.lastIndexOf("/") + 1);
                logger.info("Extracted filename from path: {}", filename);
            }

            // Create a new array of subdirectories that includes support-tickets
            String[] subdirs = {"support-tickets", "medical-results", "reports", ""};

            // Create a list of possible file paths using the configured uploadDir as the base
            Path[] possiblePaths = new Path[subdirs.length * 2];

            int index = 0;
            // Try with the primary uploadDir first
            for (String subdir : subdirs) {
                possiblePaths[index++] = Paths.get(uploadDir, subdir, filename).normalize();
            }

            // Try with a fallback "upload-dir" directory in case uploadDir is configured differently
            for (String subdir : subdirs) {
                possiblePaths[index++] = Paths.get("upload-dir", subdir, filename).normalize();
            }

            // Try each path until we find the file
            Path filePath = null;
            for (Path path : possiblePaths) {
                logger.debug("Looking for file at: {}", path.toAbsolutePath());
                if (Files.exists(path)) {
                    filePath = path;
                    logger.info("Found file at: {}", path.toAbsolutePath());
                    break;
                }
            }

            // If we couldn't find the file, return 404
            if (filePath == null) {
                logger.error("Support ticket attachment not found in any location: {}", filename);
                return ResponseEntity.notFound().build();
            }

            Resource resource = new UrlResource(filePath.toUri());

            // Determine content type based on file extension
            String contentType = determineContentType(filename);

            // Set content disposition based on inline parameter
            String contentDisposition = inline
                    ? "inline; filename=\"" + filename + "\""
                    : "attachment; filename=\"" + filename + "\"";

            // Log download attempt
            logger.info("Serving support ticket attachment '{}' with disposition: {}", filename, contentDisposition);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                    .body(resource);
        } catch (MalformedURLException e) {
            logger.error("Error creating URL for file: {}", filename, e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Helper method to determine content type based on file extension
     */
    private String determineContentType(String fileName) {
        String lowerCaseFileName = fileName.toLowerCase();
        if (lowerCaseFileName.endsWith(".pdf")) {
            return MediaType.APPLICATION_PDF_VALUE;
        } else if (lowerCaseFileName.endsWith(".jpg") || lowerCaseFileName.endsWith(".jpeg")) {
            return MediaType.IMAGE_JPEG_VALUE;
        } else if (lowerCaseFileName.endsWith(".png")) {
            return MediaType.IMAGE_PNG_VALUE;
        } else if (lowerCaseFileName.endsWith(".docx")) {
            return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        } else if (lowerCaseFileName.endsWith(".mp3")) {
            return "audio/mpeg";
        } else if (lowerCaseFileName.endsWith(".ipynb")) {
            return "application/json";
        } else {
            return MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }
    }

    /**
     * Helper method to serve a file from the medical-results directory
     * @param fileName The name of the file to serve
     * @param inline Whether to display the file inline or as an attachment
     * @return ResponseEntity with the file resource
     */
    private ResponseEntity<Resource> serveFile(String fileName, boolean inline) {
        try {
            // Sanitize the filename to remove any path information
            if (fileName.contains("/")) {
                fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
            }

            logger.info("Attempting to serve file: {}", fileName);

            // Define common subdirectories
            String[] subdirs = {"medical-results", "reports", ""};

            // Create a list of possible file paths using the configured uploadDir as the base
            Path[] possiblePaths = new Path[subdirs.length * 2];

            int index = 0;
            // Try with the primary uploadDir first
            for (String subdir : subdirs) {
                possiblePaths[index++] = Paths.get(uploadDir, subdir, fileName).normalize();
            }

            // Try with a fallback "upload-dir" directory in case uploadDir is configured differently
            for (String subdir : subdirs) {
                possiblePaths[index++] = Paths.get("upload-dir", subdir, fileName).normalize();
            }

            // Try each path until we find the file
            Path filePath = null;
            for (Path path : possiblePaths) {
                logger.debug("Looking for file at: {}", path.toAbsolutePath());
                if (Files.exists(path)) {
                    filePath = path;
                    logger.info("Found file at: {}", path.toAbsolutePath());
                    break;
                }
            }

            // If we couldn't find the file, return 404
            if (filePath == null) {
                logger.error("File not found in any location: {}", fileName);
                return ResponseEntity.notFound().build();
            }

            Resource resource = new UrlResource(filePath.toUri());

            // Determine content type based on file extension
            String contentType;
            String lowerCaseFileName = fileName.toLowerCase();
            if (lowerCaseFileName.endsWith(".pdf")) {
                contentType = MediaType.APPLICATION_PDF_VALUE;
            } else if (lowerCaseFileName.endsWith(".jpg") || lowerCaseFileName.endsWith(".jpeg")) {
                contentType = MediaType.IMAGE_JPEG_VALUE;
            } else if (lowerCaseFileName.endsWith(".png")) {
                contentType = MediaType.IMAGE_PNG_VALUE;
            } else if (lowerCaseFileName.endsWith(".docx")) {
                contentType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            } else if (lowerCaseFileName.endsWith(".mp3")) {
                contentType = "audio/mpeg";
            } else if (lowerCaseFileName.endsWith(".ipynb")) {
                contentType = "application/json";
            } else {
                contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            }

            // Set content disposition based on inline parameter
            String contentDisposition = inline
                    ? "inline; filename=\"" + fileName + "\""
                    : "attachment; filename=\"" + fileName + "\"";

            // Log download attempt
            logger.info("Serving file '{}' with disposition: {}", fileName, contentDisposition);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                    .body(resource);
        } catch (MalformedURLException e) {
            logger.error("Error creating URL for file: {}", fileName, e);
            return ResponseEntity.badRequest().build();
        }
    }
}
