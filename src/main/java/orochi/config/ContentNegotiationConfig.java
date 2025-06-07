package orochi.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ContentNegotiationConfig implements WebMvcConfigurer {

    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        // Disable automatic content type determination based on path extension
        configurer.favorPathExtension(false)
                 // Disable automatic content type determination based on parameters
                 .favorParameter(false)
                 // Use the Accept header for content negotiation
                 .ignoreAcceptHeader(false);
    }
}
