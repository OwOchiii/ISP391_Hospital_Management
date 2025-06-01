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
import orochi.repository.MedicalReportRepository;

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
     * Helper method to serve a file from the medical-results directory
     * @param fileName The name of the file to serve
     * @param inline Whether to display the file inline or as an attachment
     * @return ResponseEntity with the file resource
     */
    private ResponseEntity<Resource> serveFile(String fileName, boolean inline) {
        try {
            // Try multiple possible locations for the file
            // First try the primary location
            Path filePath = Paths.get(uploadDir, "medical-results", fileName);
            logger.info("Looking for file at: {}", filePath.toAbsolutePath());

            if (!Files.exists(filePath)) {
                // Try looking in reports directory
                filePath = Paths.get(uploadDir, "reports", fileName);
                logger.info("File not found, trying reports directory: {}", filePath.toAbsolutePath());

                if (!Files.exists(filePath)) {
                    // Try fallback location with upload-dir
                    filePath = Paths.get("D:/KanbanWeb/ISP301_Hospital_Management/upload-dir/medical-results", fileName);
                    logger.info("File not found, trying fallback location: {}", filePath.toAbsolutePath());

                    if (!Files.exists(filePath)) {
                        // Try one more location with absolute path
                        filePath = Paths.get("/uploads/reports", fileName);
                        logger.info("File not found, trying absolute path: {}", filePath.toAbsolutePath());

                        if (!Files.exists(filePath)) {
                            logger.error("File not found in any location: {}", fileName);
                            return ResponseEntity.notFound().build();
                        }
                    }
                }
            }

            Resource resource = new UrlResource(filePath.toUri());
            logger.info("Successfully located file: {}", filePath.toAbsolutePath());

            // Determine content type based on file extension
            String contentType;
            if (fileName.toLowerCase().endsWith(".pdf")) {
                contentType = MediaType.APPLICATION_PDF_VALUE;
            } else if (fileName.toLowerCase().endsWith(".jpg") || fileName.toLowerCase().endsWith(".jpeg")) {
                contentType = MediaType.IMAGE_JPEG_VALUE;
            } else if (fileName.toLowerCase().endsWith(".png")) {
                contentType = MediaType.IMAGE_PNG_VALUE;
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
