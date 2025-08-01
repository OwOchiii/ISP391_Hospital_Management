package orochi.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Get project root directory
        String projectRoot = System.getProperty("user.dir");
        String uploadsPath = "file:" + projectRoot + File.separator + "uploads" + File.separator;

        // Configure static resource handler for uploads directory
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadsPath)
                .setCachePeriod(3600); // Cache for 1 hour

        // Additional resource handler for receptionist avatars specifically
        registry.addResourceHandler("/uploads/receptionist-avatars/**")
                .addResourceLocations(uploadsPath + "receptionist-avatars" + File.separator)
                .setCachePeriod(3600);
    }
}
