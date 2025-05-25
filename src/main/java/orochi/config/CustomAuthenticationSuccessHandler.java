package orochi.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.io.IOException;
import java.util.Set;

public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        Set<String> roles = AuthorityUtils.authorityListToSet(authentication.getAuthorities());

        if (roles.contains("ADMIN")) {
            response.sendRedirect("/admin/dashboard");
        } else if (roles.contains("DOCTOR")) {
            // For doctors, you need to handle the doctorId parameter
            // You may need to retrieve the doctor's ID from your user service
            // For now, redirecting to doctor dashboard which will need to handle missing doctorId
            response.sendRedirect("/doctor/dashboard");
        } else if (roles.contains("RECEPTIONIST")) {
            response.sendRedirect("/receptionist/dashboard");
        } else if (roles.contains("PATIENT")) {
            response.sendRedirect("/patient/dashboard");
        } else {
            // Default fallback
            response.sendRedirect("/dashboard");
        }
    }
}