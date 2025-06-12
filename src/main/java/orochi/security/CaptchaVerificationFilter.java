package orochi.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import orochi.service.CaptchaService;

import java.io.IOException;

public class CaptchaVerificationFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(CaptchaVerificationFilter.class);

    private final CaptchaService captchaService;
    private final RequestMatcher requiresAuthenticationRequestMatcher;

    public CaptchaVerificationFilter(String loginProcessingUrl, CaptchaService captchaService) {
        this.requiresAuthenticationRequestMatcher = new AntPathRequestMatcher(loginProcessingUrl, "POST");
        this.captchaService = captchaService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (requiresAuthenticationRequestMatcher.matches(request)) {
            logger.debug("Processing login request with CAPTCHA verification");

            String recaptchaResponse = request.getParameter("g-recaptcha-response");
            logger.debug("CAPTCHA response received: {}", recaptchaResponse != null ?
                (recaptchaResponse.length() > 20 ? recaptchaResponse.substring(0, 20) + "..." : recaptchaResponse) : "null");

            // For development purposes, bypass CAPTCHA if parameter is present (remove in production)
            if (request.getParameter("bypass-captcha") != null) {
                logger.warn("CAPTCHA verification bypassed - FOR DEVELOPMENT ONLY");
                filterChain.doFilter(request, response);
                return;
            }

            if (recaptchaResponse == null || recaptchaResponse.isEmpty()) {
                logger.warn("Empty CAPTCHA response, redirecting to login with error");
                saveCaptchaErrorRedirect(request, "EMPTY_RESPONSE");
                response.sendRedirect("/auth/login?captchaError=EMPTY_RESPONSE");
                return;
            }

            boolean isValid = captchaService.validateCaptcha(recaptchaResponse);
            logger.debug("CAPTCHA validation result: {}", isValid);

            if (!isValid) {
                logger.warn("Invalid CAPTCHA response, redirecting to login with error");
                saveCaptchaErrorRedirect(request, "VALIDATION_FAILED");
                response.sendRedirect("/auth/login?captchaError=VALIDATION_FAILED");
                return;
            }

            logger.debug("CAPTCHA verification successful, continuing authentication");
        }

        // Continue the filter chain to process authentication
        filterChain.doFilter(request, response);
    }

    private void saveCaptchaErrorRedirect(HttpServletRequest request, String errorCode) {
        // Save the username from the login form in the session
        String username = request.getParameter("username");
        if (username != null) {
            request.getSession().setAttribute("LAST_USERNAME", username);
            logger.debug("Saved username '{}' to session for CAPTCHA error redirect", username);
        }

        // Set a session attribute to indicate captcha error with specific error code
        request.getSession().setAttribute("CAPTCHA_ERROR", errorCode);

        // Save CAPTCHA debugging information in session
        request.getSession().setAttribute("CAPTCHA_DEBUG_INFO",
                "Error code: " + errorCode +
                ", Time: " + java.time.LocalDateTime.now() +
                ", RemoteAddr: " + request.getRemoteAddr());
    }
}
