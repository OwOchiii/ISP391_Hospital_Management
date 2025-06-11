package orochi.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class FileStorageConfig implements WebMvcConfigurer {

    @Value("${file.upload.directory:upload-dir}")
    private String uploadDirectory;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Register resource handler for files
        Path uploadDir = Paths.get(uploadDirectory);
        String uploadPath = uploadDir.toFile().getAbsolutePath();

        registry.addResourceHandler("/files/**")
                .addResourceLocations("file:" + uploadPath + "/")
                .setCachePeriod(3600) // Cache for 1 hour
                .resourceChain(true);
    }
}
