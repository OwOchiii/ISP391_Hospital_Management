package orochi.service;

import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class CaptchaService {
    private static final Logger logger = LoggerFactory.getLogger(CaptchaService.class);

    @Value("${google.recaptcha.key.secret}")
    private String recaptchaSecret;

    @Value("${google.recaptcha.verify.url}")
    private String verifyUrl;

    // For development environments, set to true in application-dev.properties
    @Value("${google.recaptcha.local.bypass:false}")
    private boolean localBypass;

    public boolean validateCaptcha(String captchaResponse) {
        if (captchaResponse == null || captchaResponse.isEmpty()) {
            logger.warn("Empty captcha response");
            return false;
        }

        // Local development bypass option
        if (localBypass) {
            logger.warn("⚠️ BYPASSING CAPTCHA VERIFICATION - FOR DEVELOPMENT ONLY ⚠️");
            return true;
        }

        try {
            Map<String, String> body = new HashMap<>();
            body.put("secret", recaptchaSecret);
            body.put("response", captchaResponse);

            RestTemplate restTemplate = new RestTemplate();
            logger.debug("Sending reCAPTCHA verification request to {}", verifyUrl);
            CaptchaResponse response = restTemplate.postForObject(
                verifyUrl, body, CaptchaResponse.class);

            if (response == null) {
                logger.error("Null response received from reCAPTCHA verification");
                return false;
            }

            logger.debug("reCAPTCHA verification result: success={}, hostname={}",
                response.isSuccess(), response.getHostname());

            if (!response.isSuccess()) {
                logger.warn("reCAPTCHA verification failed with errors: {}",
                    response.getErrorCodes() != null ? String.join(", ", response.getErrorCodes()) : "none");

                // Log the full response for debugging
                logger.debug("Full reCAPTCHA response: success={}, challenge_ts={}, hostname={}, errorCodes={}",
                    response.isSuccess(),
                    response.getChallenge_ts(),
                    response.getHostname(),
                    response.getErrorCodes() != null ? String.join(",", response.getErrorCodes()) : "none");
            }

            return response.isSuccess();
        } catch (RestClientException e) {
            logger.error("Error during reCAPTCHA verification: {}", e.getMessage());
            return false;
        }
    }

    @Setter
    @Getter
    private static class CaptchaResponse {
        private boolean success;
        private String challenge_ts;
        private String hostname;
        private String[] errorCodes;
        private String[] error_codes; // Alternative field name used by Google

        // Handle both field name conventions for error codes
        public String[] getErrorCodes() {
            return errorCodes != null ? errorCodes : error_codes;
        }
    }
}
