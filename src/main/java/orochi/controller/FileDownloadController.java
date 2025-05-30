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
import org.springframework.web.bind.annotation.ResponseBody;

import orochi.model.MedicalReport;
import orochi.repository.MedicalReportRepository;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

@Controller
@RequestMapping("/download")
public class FileDownloadController {

    private static final Logger logger = LoggerFactory.getLogger(FileDownloadController.class);

    @Value("${file.upload-dir:/uploads}")
    private String uploadDir;

    @Autowired
    private MedicalReportRepository medicalReportRepository;

    @GetMapping("/report/{reportId}")
    @ResponseBody
    public ResponseEntity<Resource> downloadReport(@PathVariable Integer reportId) {
        try {
            // Get the report from the database
            Optional<MedicalReport> reportOpt = medicalReportRepository.findById(reportId);
            if (reportOpt.isEmpty()) {
                logger.error("Report not found for ID: {}", reportId);
                return ResponseEntity.notFound().build();
            }

            MedicalReport report = reportOpt.get();
            String fileName = report.getFileUrl();

            // Create the file path
            Path filePath = Paths.get(uploadDir + "/reports/" + fileName);
            Resource resource;

            try {
                resource = new UrlResource(filePath.toUri());
                if (!resource.exists()) {
                    logger.error("File not found: {}", filePath);
                    return ResponseEntity.notFound().build();
                }
            } catch (MalformedURLException e) {
                logger.error("Error creating URL for file: {}", filePath, e);
                return ResponseEntity.badRequest().build();
            }

            // Set content type and attachment header
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .body(resource);

        } catch (Exception e) {
            logger.error("Error downloading report: {}", reportId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
