package orochi.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.charset.StandardCharsets;

/**
 * Enhanced encoding configuration to ensure proper handling of all UTF-8 characters,
 * including Vietnamese combined diacritics (like 'ữ' and 'ờ')
 */
@Configuration
public class EncodingConfig implements WebMvcConfigurer {

    /**
     * Registers a character encoding filter with high priority to ensure
     * it's applied early in the filter chain
     */
    @Bean
    public FilterRegistrationBean<CharacterEncodingFilter> characterEncodingFilter() {
        CharacterEncodingFilter filter = new CharacterEncodingFilter();
        filter.setEncoding(StandardCharsets.UTF_8.name());
        filter.setForceEncoding(true); // Force the encoding for both request and response

        FilterRegistrationBean<CharacterEncodingFilter> registrationBean =
            new FilterRegistrationBean<>();
        registrationBean.setFilter(filter);
        registrationBean.addUrlPatterns("/*");
        registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE);

        return registrationBean;
    }
}
