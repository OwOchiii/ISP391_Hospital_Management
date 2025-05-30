package orochi.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;

@Controller
@RequestMapping("/files")
public class FileController {

    private static final Logger logger = LoggerFactory.getLogger(FileController.class);

    @Value("${file.upload.directory:upload-dir}")
    private String uploadDirectory;

    /**
     * Serves files from the upload directory
     *
     * @param directory The subdirectory where the file is stored
     * @param filename The name of the file to serve
     * @return The file as a resource
     */
    @GetMapping("/{directory}/{filename:.+}")
    public ResponseEntity<Resource> serveFile(@PathVariable String directory, @PathVariable String filename) {
        try {
            Path filePath = Paths.get(uploadDirectory + File.separator + directory + File.separator + filename);
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                logger.info("Serving file: {}", filePath);

                // Determine content type
                String contentType;
                String filenameLower = filename.toLowerCase();
                if (filenameLower.endsWith(".pdf")) {
                    contentType = MediaType.APPLICATION_PDF_VALUE;
                } else if (filenameLower.endsWith(".jpg") || filenameLower.endsWith(".jpeg")) {
                    contentType = MediaType.IMAGE_JPEG_VALUE;
                } else if (filenameLower.endsWith(".png")) {
                    contentType = MediaType.IMAGE_PNG_VALUE;
                } else {
                    contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
                }

                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                logger.warn("File not found or not readable: {}", filePath);
                return ResponseEntity.notFound().build();
            }
        } catch (MalformedURLException e) {
            logger.error("Malformed URL", e);
            return ResponseEntity.badRequest().build();
        } catch (IOException e) {
            logger.error("IO Exception", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
