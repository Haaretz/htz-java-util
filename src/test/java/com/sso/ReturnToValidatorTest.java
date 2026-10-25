package com.sso;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ReturnToValidatorTest {

    // stage-like: any subdomain of the default domains, http allowed
    private final ReturnToValidator anySubdomain = new ReturnToValidator(false, Collections.emptyList(), null);
    // production-like: https only, explicit hosts
    private final ReturnToValidator explicitHosts =
            new ReturnToValidator(true, Arrays.asList("www.haaretz.co.il", "login.themarker.com"), null);

    private static final String[] ALLOWED = {
            "/",
            "/promotions-page",
            "/a?next=%2Fb",
            "https://www.haaretz.co.il/news",
            "https://haaretz.co.il",
            "https://www.haaretz.com/israel-news?x=1",
            "https://www.themarker.com/",
            "http://login-dev1.k8s-stage.themarker.com/path",
            "HTTPS://WWW.HAARETZ.CO.IL/"
    };

    private static final String[] MALICIOUS = {
            "https://evil.com",
            "https://haaretz.co.il.evil.com",
            "https://evilhaaretz.co.il",
            "https://notthemarker.com",
            "https://www.haaretz.co.il@evil.com",
            "//evil.com",
            "/\\evil.com",
            "\\\\evil.com",
            "https:\\\\evil.com",
            "javascript:alert(1)",
            "data:text/html,hi",
            "evil.com",
            "https://evil.com/?r=haaretz.co.il",
            "https://evil.com#haaretz.com",
            "/\t/evil.com",
            "/%2F%2Fevil.com",
            "/%2f%2fevil.com",
            "/%5Cevil.com",
            "/%252F%252Fevil.com",
            "/%0d%0afake-log-line",
            "https://evil.com%2F.haaretz.co.il",
            "https://login.haaretz.co.il//google.com",
            "https://www.haaretz.co.il///evil.com/x",
            "https://www.haaretz.co.il/%2Fevil.com",
            "https://www.haaretz.co.il/%252Fevil.com"
    };

    @Test
    public void keepsAllowedValues() {
        for (String returnTo : ALLOWED) {
            assertEquals(returnTo, returnTo, anySubdomain.sanitize(returnTo, "/"));
        }
    }

    @Test
    public void rejectsMaliciousValues() {
        for (String returnTo : MALICIOUS) {
            assertEquals(returnTo, "/", anySubdomain.sanitize(returnTo, "/"));
            assertEquals(returnTo, "/", explicitHosts.sanitize(returnTo, "/"));
            assertFalse(returnTo, anySubdomain.isValid(returnTo));
        }
    }

    @Test
    public void missingValueIsValidAndFallsBack() {
        assertTrue(anySubdomain.isValid(null));
        assertTrue(anySubdomain.isValid("  "));
        assertFalse(anySubdomain.isAllowed(null));
        assertEquals("https://fallback.haaretz.co.il", anySubdomain.sanitize(null, "https://fallback.haaretz.co.il"));
    }

    @Test
    public void explicitHostsOnlyAllowsListedHttpsHosts() {
        assertTrue(explicitHosts.isAllowed("https://www.haaretz.co.il/x"));
        assertTrue(explicitHosts.isAllowed("https://LOGIN.themarker.com/"));
        assertTrue(explicitHosts.isAllowed("/relative"));
        assertFalse(explicitHosts.isAllowed("http://www.haaretz.co.il/x"));
        assertFalse(explicitHosts.isAllowed("https://old-campaign.haaretz.co.il/"));
        assertFalse(explicitHosts.isAllowed("https://www.haaretz.com/"));
    }

    @Test
    public void customDomains() {
        ReturnToValidator validator = new ReturnToValidator(true, null, Arrays.asList(".mouse.co.il"));
        assertTrue(validator.isAllowed("https://www.mouse.co.il/"));
        assertFalse(validator.isAllowed("https://www.haaretz.co.il/"));
    }

    @Test
    public void defaultConstructorIsHttpsWithDefaultDomains() {
        ReturnToValidator validator = new ReturnToValidator();
        assertTrue(validator.isAllowed("https://react-stage.haaretz.com/"));
        assertFalse(validator.isAllowed("http://www.haaretz.com/"));
    }
}
