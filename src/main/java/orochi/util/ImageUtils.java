package orochi.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

/**
 * Utility class for handling Base64 encoded images
 */
public class ImageUtils {

    private static final Logger logger = LoggerFactory.getLogger(ImageUtils.class);

    /**
     * Converts a Base64 encoded image string to a MultipartFile
     *
     * @param base64Image Base64 encoded image string (format: data:image/jpeg;base64,/9j/4AAQSkZ...)
     * @param filename The filename to use for the MultipartFile
     * @return MultipartFile created from the Base64 image
     * @throws IOException If the conversion fails
     */
    public static MultipartFile base64ToMultipartFile(String base64Image, String filename) throws IOException {
        // Extract the actual Base64 string from the data URL
        String[] parts = base64Image.split(",");
        String imageString = parts.length > 1 ? parts[1] : parts[0];

        // Determine the file format
        String fileExtension = "jpg"; // Default to jpg
        if (base64Image.contains("image/png")) {
            fileExtension = "png";
        } else if (base64Image.contains("image/gif")) {
            fileExtension = "gif";
        }

        // Convert Base64 to byte array
        byte[] imageBytes = Base64.getDecoder().decode(imageString);

        // Create and return a MultipartFile from the byte array
        return new Base64MultipartFile(imageBytes, filename + "." + fileExtension);
    }

    /**
     * Validates if the image dimensions are within specified limits
     *
     * @param base64Image Base64 encoded image string
     * @param maxWidth Maximum allowed width
     * @param maxHeight Maximum allowed height
     * @return true if the image dimensions are valid, false otherwise
     */
    public static boolean validateImageDimensions(String base64Image, int maxWidth, int maxHeight) {
        try {
            // Extract the actual Base64 string from the data URL
            String[] parts = base64Image.split(",");
            String imageString = parts.length > 1 ? parts[1] : parts[0];

            // Convert Base64 to byte array
            byte[] imageBytes = Base64.getDecoder().decode(imageString);

            // Read image
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));

            // Check dimensions
            return image.getWidth() <= maxWidth && image.getHeight() <= maxHeight;
        } catch (IOException | IllegalArgumentException e) {
            logger.error("Error validating image dimensions", e);
            return false;
        }
    }

    /**
     * Custom implementation of MultipartFile for a Base64 encoded image
     */
    private static class Base64MultipartFile implements MultipartFile {
        private final byte[] content;
        private final String filename;

        public Base64MultipartFile(byte[] content, String filename) {
            this.content = content;
            this.filename = filename;
        }

        @Override
        public String getName() {
            return filename;
        }

        @Override
        public String getOriginalFilename() {
            return filename;
        }

        @Override
        public String getContentType() {
            if (filename.endsWith(".png")) {
                return "image/png";
            } else if (filename.endsWith(".gif")) {
                return "image/gif";
            } else {
                return "image/jpeg";
            }
        }

        @Override
        public boolean isEmpty() {
            return content == null || content.length == 0;
        }

        @Override
        public long getSize() {
            return content.length;
        }

        @Override
        public byte[] getBytes() {
            return content;
        }

        @Override
        public ByteArrayInputStream getInputStream() {
            return new ByteArrayInputStream(content);
        }

        @Override
        public void transferTo(java.io.File dest) throws IOException, IllegalStateException {
            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(dest)) {
                fos.write(content);
            }
        }
    }
}
