/**
 * URL Sanitizer
 * This script provides functions to sanitize URLs to prevent XSS attacks through malicious links
 */

/**
 * Sanitizes a URL to prevent XSS attacks
 * @param {string} url - The URL to sanitize
 * @returns {string} - The sanitized URL
 */
function sanitizeUrl(url) {
    if (!url) return '#';

    // Trim whitespace
    url = url.trim();

    // Check for javascript: and other dangerous protocols
    const dangerousProtocols = /^(?:javascript|data|vbscript|file):/i;
    if (dangerousProtocols.test(url)) {
        console.warn('Potentially malicious URL detected and blocked:', url);
        return '#';
    }

    // Check for HTML injection in the URL
    const htmlInjection = /<[^>]*>/;
    if (htmlInjection.test(url)) {
        console.warn('HTML injection in URL detected and blocked:', url);
        return '#';
    }

    // Allow http, https, and relative URLs
    const safeProtocols = /^(?:https?:\/\/|\/|#)/i;
    if (safeProtocols.test(url)) {
        return url;
    }

    // If it starts with www., add https://
    if (/^www\./i.test(url)) {
        return 'https://' + url;
    }

    // For other URLs without protocol, assume https
    if (!/^[/:#]/.test(url) && !safeProtocols.test(url)) {
        return 'https://' + url;
    }

    return url;
}

/**
 * Makes a link element safe by sanitizing its href attribute
 * @param {HTMLAnchorElement} linkElement - The <a> element to make safe
 */
function makeLinkSafe(linkElement) {
    if (!linkElement || linkElement.tagName !== 'A') return;

    const originalHref = linkElement.getAttribute('href');
    const safeHref = sanitizeUrl(originalHref);

    if (originalHref !== safeHref) {
        console.warn('URL sanitized:', originalHref, '->', safeHref);
    }

    linkElement.setAttribute('href', safeHref);

    // Add target="_blank" and rel="noopener noreferrer" for external links
    if (safeHref.startsWith('http') && !safeHref.startsWith(window.location.origin)) {
        linkElement.setAttribute('target', '_blank');
        linkElement.setAttribute('rel', 'noopener noreferrer');
    }
}

// Export for use in other scripts
window.orochiSecurity = window.orochiSecurity || {};
window.orochiSecurity.sanitizeUrl = sanitizeUrl;
window.orochiSecurity.makeLinkSafe = makeLinkSafe;
