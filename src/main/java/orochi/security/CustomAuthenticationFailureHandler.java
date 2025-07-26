package orochi.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class CustomAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private static final Logger logger = LoggerFactory.getLogger(CustomAuthenticationFailureHandler.class);

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {

        String errorMessage;
        String redirectUrl = "/auth/login?error=true";

        // Log the exception details for debugging
        logger.warn("Authentication failed: {} - {}", exception.getClass().getSimpleName(), exception.getMessage());

        // Check the type of authentication exception
        if (exception instanceof LockedException ||
            exception instanceof DisabledException ||
            exception instanceof AccountExpiredException) {
            errorMessage = "account_locked";
            logger.warn("Account locked/disabled for user: {}", request.getParameter("username"));
        } else if (exception instanceof CredentialsExpiredException) {
            errorMessage = "credentials_expired";
            logger.warn("Credentials expired for user: {}", request.getParameter("username"));
        } else if (exception instanceof UsernameNotFoundException &&
                   exception.getMessage().contains("Account is locked")) {
            errorMessage = "account_locked";
            logger.warn("Account locked (custom check) for user: {}", request.getParameter("username"));
        } else if (exception instanceof org.springframework.security.authentication.InternalAuthenticationServiceException) {
            // Check if the wrapped exception is a LockedException
            Throwable cause = exception.getCause();
            if (cause instanceof LockedException ||
                (exception.getMessage() != null && exception.getMessage().contains("Account is locked"))) {
                errorMessage = "account_locked";
                logger.warn("Account locked (wrapped exception) for user: {}", request.getParameter("username"));
            } else {
                errorMessage = "authentication_failed";
                logger.warn("Internal authentication error for user: {} - {}", request.getParameter("username"), exception.getMessage());
            }
        } else if (exception instanceof BadCredentialsException) {
            errorMessage = "bad_credentials";
            logger.warn("Bad credentials for user: {}", request.getParameter("username"));
        } else {
            errorMessage = "authentication_failed";
            logger.warn("Authentication failed for user: {} - {}", request.getParameter("username"), exception.getMessage());
        }

        // Encode the error message and add it to the redirect URL
        redirectUrl += "&errorType=" + URLEncoder.encode(errorMessage, StandardCharsets.UTF_8);

        // Store the exception in session for detailed error handling
        request.getSession().setAttribute("SPRING_SECURITY_LAST_EXCEPTION", exception);

        // Redirect to login page with error parameters
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}
