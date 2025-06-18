package orochi.security;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class UrlValidator {

    private static final List<String> ALLOWED_PROTOCOLS = Arrays.asList("http", "https");

    // Pattern to check for javascript: protocol and other dangerous patterns
    private static final Pattern DANGEROUS_PATTERN =
        Pattern.compile("(?i)^(javascript|data|vbscript|file):|\\s*on\\w+\\s*=");

    /**
     * Validates if a URL is safe to use in href attributes
     *
     * @param url The URL to validate
     * @return true if the URL is safe, false otherwise
     */
    public static boolean isUrlSafe(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }

        // Check for dangerous patterns
        if (DANGEROUS_PATTERN.matcher(url).find()) {
            return false;
        }

        try {
            URI uri = new URI(url);
            String scheme = uri.getScheme();

            // If relative URL (no scheme), it's generally safe for same-origin links
            if (scheme == null) {
                return true;
            }

            // Only allow http and https protocols
            return ALLOWED_PROTOCOLS.contains(scheme.toLowerCase());

        } catch (URISyntaxException e) {
            return false;
        }
    }

    /**
     * Sanitizes a URL to make it safe
     *
     * @param url The URL to sanitize
     * @return A safe URL or null if completely invalid
     */
    public static String sanitizeUrl(String url) {
        if (url == null || url.isEmpty()) {
            return "#"; // Default to harmless fragment
        }

        // If already safe, return as is
        if (isUrlSafe(url)) {
            return url;
        }

        // Try to convert to a safe URL
        if (url.startsWith("www.")) {
            return "https://" + url;
        }

        // If nothing works, return a safe default
        return "#";
    }
}
