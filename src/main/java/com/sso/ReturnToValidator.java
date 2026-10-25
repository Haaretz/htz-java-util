package com.sso;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Protects against open-redirect / phishing via user supplied redirect targets ("returnTo", "redirectUrl").
 * <p>
 * Allowed values: a site-relative path ("/foo"), or an absolute URL whose host is allowed:
 * <ul>
 *     <li>when {@code allowedHosts} is not empty - only those exact hosts</li>
 *     <li>otherwise - any of {@code allowedDomains} or a subdomain of them</li>
 * </ul>
 * Plain Java on purpose (no Spring): every service builds it from its own configuration.
 */
public class ReturnToValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReturnToValidator.class);

    public static final List<String> DEFAULT_DOMAINS =
            Collections.unmodifiableList(Arrays.asList("haaretz.co.il", "haaretz.com", "themarker.com"));

    private static final int MAX_DECODE_ROUNDS = 3;
    private static final int MAX_LOGGED_LENGTH = 200;

    private final boolean requireHttps;
    private final Set<String> allowedHosts;
    private final Set<String> allowedDomains;

    /** https only, any subdomain of {@link #DEFAULT_DOMAINS} */
    public ReturnToValidator() {
        this(true, Collections.emptyList(), DEFAULT_DOMAINS);
    }

    /**
     * @param requireHttps   reject http:// URLs (keep true in production)
     * @param allowedHosts   exact hosts to allow; when empty, {@code allowedDomains} is used
     * @param allowedDomains domains whose host or subdomains are allowed; when null or empty, {@link #DEFAULT_DOMAINS}
     */
    public ReturnToValidator(boolean requireHttps, List<String> allowedHosts, List<String> allowedDomains) {
        this.requireHttps = requireHttps;
        this.allowedHosts = normalize(allowedHosts);
        Set<String> domains = normalize(allowedDomains);
        this.allowedDomains = domains.isEmpty() ? normalize(DEFAULT_DOMAINS) : domains;
    }

    /**
     * @return true when returnTo is missing (caller uses its default) or points to an allowed destination
     */
    public boolean isValid(String returnTo) {
        if (returnTo == null || returnTo.trim().isEmpty() || isAllowed(returnTo.trim())) {
            return true;
        }
        LOGGER.warn("Rejected returnTo with non-allowed destination: {}", forLog(returnTo));
        return false;
    }

    /**
     * @return the trimmed returnTo when it is allowed, otherwise {@code fallback}
     */
    public String sanitize(String returnTo, String fallback) {
        return returnTo != null && !returnTo.trim().isEmpty() && isValid(returnTo) ? returnTo.trim() : fallback;
    }

    /**
     * Strict check: null/blank values are not allowed.
     */
    public boolean isAllowed(String returnTo) {
        if (returnTo == null || returnTo.isEmpty()) {
            return false;
        }
        // check the raw value and every percent-decoded form, in case a downstream service decodes it again
        String current = returnTo;
        for (int i = 0; i <= MAX_DECODE_ROUNDS; i++) {
            if (!isAllowedSingle(current)) {
                return false;
            }
            String decoded;
            try {
                decoded = URLDecoder.decode(current, StandardCharsets.UTF_8);
            } catch (IllegalArgumentException e) {
                return false;
            }
            if (decoded.equals(current)) {
                return true;
            }
            current = decoded;
        }
        return false;
    }

    private boolean isAllowedSingle(String returnTo) {
        // backslashes and control chars can be normalized by browsers into a different host
        if (returnTo.indexOf('\\') >= 0 || returnTo.chars().anyMatch(c -> c < 0x20 || c == 0x7f)) {
            return false;
        }
        // site-relative path, but not protocol-relative ("//evil.com")
        if (returnTo.startsWith("/")) {
            return !returnTo.startsWith("//");
        }
        try {
            URI uri = new URI(returnTo);
            String scheme = uri.getScheme();
            boolean schemeAllowed = "https".equalsIgnoreCase(scheme) || (!requireHttps && "http".equalsIgnoreCase(scheme));
            if (!schemeAllowed) {
                return false;
            }
            // reject "https://haaretz.co.il@evil.com"
            if (uri.getRawUserInfo() != null || uri.getHost() == null) {
                return false;
            }
            // reject "https://haaretz.co.il//evil.com": a consumer redirecting to just the path gets "//evil.com"
            String rawPath = uri.getRawPath();
            if (rawPath != null && rawPath.startsWith("//")) {
                return false;
            }
            return isAllowedHost(uri.getHost());
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isAllowedHost(String host) {
        String normalizedHost = host.toLowerCase(Locale.ROOT);
        if (normalizedHost.endsWith(".")) {
            normalizedHost = normalizedHost.substring(0, normalizedHost.length() - 1);
        }
        if (!allowedHosts.isEmpty()) {
            return allowedHosts.contains(normalizedHost);
        }
        for (String domain : allowedDomains) {
            if (normalizedHost.equals(domain) || normalizedHost.endsWith("." + domain)) {
                return true;
            }
        }
        return false;
    }

    private static Set<String> normalize(List<String> values) {
        if (values == null) {
            return Collections.emptySet();
        }
        return values.stream()
                .filter(v -> v != null && !v.trim().isEmpty())
                .map(v -> v.trim().toLowerCase(Locale.ROOT).replaceFirst("^\\.", ""))
                .collect(Collectors.toSet());
    }

    // the rejected value is attacker-controlled: strip control chars so it can't forge log lines
    private static String forLog(String value) {
        String cleaned = value.replaceAll("\\p{Cntrl}", "?");
        return cleaned.length() > MAX_LOGGED_LENGTH ? cleaned.substring(0, MAX_LOGGED_LENGTH) + "..." : cleaned;
    }
}
