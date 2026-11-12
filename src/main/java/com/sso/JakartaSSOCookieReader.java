package com.sso;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads the SSO cookie from a jakarta.servlet request. For javax.servlet use {@link SSOCookieReader}.
 *
 * Do NOT add overloads for the other servlet API (javax/jakarta) to this class. Containers that
 * run javax->jakarta bytecode migration (Tomcat 10+ / Eclipse Transformer, e.g. Brightspot) rewrite
 * them into duplicate methods and the class fails to load with ClassFormatError.
 */
public class JakartaSSOCookieReader {

    private static final Logger LOGGER = LoggerFactory.getLogger(JakartaSSOCookieReader.class);

    public static SSOCookie read(HttpServletRequest request) {
        try {
            return read(request.getCookies());
        } catch (Exception e) {
            LOGGER.error("failed to read sso cookie", e);
        }
        return null;
    }

    public static SSOCookie read(Cookie[] cookies) {
        try {
            Cookie cookie = getCookie(cookies);
            if (cookie != null && cookie.getValue() != null && !cookie.getValue().isEmpty()) {
                return SSOCookieParser.parse(cookie.getName(), cookie.getValue(), isPaying(cookies));
            }
        } catch (Exception e) {
            LOGGER.error("failed to read sso cookie");
        }
        return null;
    }

    private static Cookie getCookie(Cookie[] cookies) {
        if (cookies != null) {
            for (SSOCookieParser.CookieNames cookieName : SSOCookieParser.CookieNames.values()) {
                for (Cookie cookie : cookies) {
                    if (cookie.getName().equalsIgnoreCase(cookieName.name())) {
                        return cookie;
                    }
                }
            }
        }
        return null;
    }

    private static boolean isPaying(Cookie[] cookies) {
        for (Cookie cookie : cookies) {
            if (SSOCookieParser.isPayingCookieName(cookie.getName())) {
                return true;
            }
        }
        return false;
    }
}
