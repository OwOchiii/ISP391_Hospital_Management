package orochi.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import orochi.security.CaptchaVerificationFilter;
import orochi.service.CaptchaService;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CaptchaService captchaService;

    @Bean
    public AuthenticationSuccessHandler authenticationSuccessHandler() {
        return new CustomAuthenticationSuccessHandler();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // Create captcha filter
        CaptchaVerificationFilter captchaFilter = new CaptchaVerificationFilter(
                "/auth/process-login", captchaService);

        http
                // Add the captcha filter before the username/password authentication filter
                .addFilterBefore(captchaFilter, UsernamePasswordAuthenticationFilter.class)

                // Disable CSRF for API endpoints
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/receptionist/api/**", "/admin/api/**", "/doctor/api/**", "/patient/api/**")
                )

                .authorizeHttpRequests(authorizeRequests ->
                        authorizeRequests
                                .requestMatchers("/auth/**","/auth/reset-password", "/css/**", "/js/**", "/images/**", "/", "/test/url-sanitizer", "/test/xss").permitAll()
                                .requestMatchers("/admin/**").hasAuthority("ADMIN")
                                .requestMatchers("/doctor/**").hasAuthority("DOCTOR")
                                .requestMatchers("/receptionist/**").hasAuthority("RECEPTIONIST")
                                .requestMatchers("/patient/**").hasAuthority("PATIENT")
                                .anyRequest().authenticated()
                )
                .formLogin(formLogin ->
                        formLogin
                                .loginPage("/auth/login")
                                .loginProcessingUrl("/auth/process-login")
                                .successHandler(authenticationSuccessHandler())
                                .failureUrl("/auth/login?error=true")
                                .permitAll()
                )
                .logout(logout ->
                        logout
                                .logoutUrl("/auth/logout")
                                .logoutSuccessUrl("/auth/login?logout=true")
                                .invalidateHttpSession(true)
                                .deleteCookies("JSESSIONID")
                                .permitAll()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .invalidSessionUrl("/auth/login?expired=true")
                        .maximumSessions(1)
                        .expiredUrl("/auth/login?expired=true")
                )
                .rememberMe(rememberMe -> rememberMe
                        .key("uniqueAndSecretKey")
                        .tokenValiditySeconds(86400) // 1 day
                        .rememberMeParameter("remember-me")
                );

        return http.build();
    }

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userDetailsService)
                .passwordEncoder(passwordEncoder);
    }
}