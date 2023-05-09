package com.sso;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sso.SSOCookieReader.CookieNames.sso_token;
import static com.sso.SSOUserType.paying;
import static com.sso.SSOUserType.registered;

public class SSOCookieReader {

    private static final Logger LOGGER = LoggerFactory.getLogger(SSOCookieReader.class);

    enum CookieNames {
        sso_token, _hs_sso, tmsso, engsso
    }

    public static SSOCookie read(HttpServletRequest request) {

        try {
            Cookie cookie = getCookie(request);
            if (cookie != null && cookie.getValue() != null && !cookie.getValue().isEmpty()) {
                if (cookie.getName().equals(sso_token.name())) {
                    return parseNewFormat(cookie.getValue());
                } else {
                    return parseOldFormat(cookie.getValue(), request);
                }
            }

        } catch (Exception e) {
            LOGGER.error("failed to read sso cookie", e);
        }
        return null;
    }

    private static Cookie getCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (CookieNames cookieName : CookieNames.values()) {
                for (Cookie cookie : cookies) {
                    if (cookie.getName().equalsIgnoreCase(cookieName.name())) {
                        return cookie;
                    }
                }
            }
        }
        return null;
    }

    private static SSOCookie parseOldFormat(String cookieValue, HttpServletRequest request) {

        cookieValue = URLDecoder.decode(cookieValue, StandardCharsets.UTF_8);
        SSOCookie ssoCookie = new SSOCookie();

        String[] fields = cookieValue.split(":");
        for (String field : fields) {
            String key = field.split("=")[0];
            String value = field.split("=")[1];
            switch (key) {
                case "userId":
                    ssoCookie.setUserId(value);
                    break;
                case "userName":
                    ssoCookie.setUserMail(value);
                    break;
                case "ticketId":
                    ssoCookie.setTicketId(value);
                    break;
                case "firstName":
                    ssoCookie.setFirstName(value);
                    break;
                case "lastName":
                    ssoCookie.setLastName(value);
                    break;
                case "emailValidity":
                    ssoCookie.setEmailValidity(SSOEmailValidity.valueOf(value));
                    break;
                case "antiAbuseToken":
                    ssoCookie.setAntiAbuseToken(value);
                    break;
            }
        }

        ssoCookie.setUserType(registered);
        for (Cookie cookie : request.getCookies()) {
            if (cookie.getName().endsWith("Pusr")) {
                ssoCookie.setUserType(paying);
                break;
            }
        }

        return ssoCookie;
    }

    private static SSOCookie parseNewFormat(String cookieValue) {

        byte[] bytes = Base64.getDecoder().decode(cookieValue);
        cookieValue = new String(bytes, StandardCharsets.UTF_8);
        JSONObject json = new JSONObject(cookieValue);

        SSOCookie ssoCookie = new SSOCookie();
        ssoCookie.setUserId(getString(json, "userId"));
        ssoCookie.setUserMail(getString(json, "userMail"));
        ssoCookie.setTicketId(getString(json, "ticketId"));
        ssoCookie.setFirstName(getString(json, "firstName"));
        ssoCookie.setLastName(getString(json, "lastName"));
        ssoCookie.setAntiAbuseToken(getString(json, "antiAbuseToken"));

        String emailValidity = getString(json, "emailValidity");
        if (emailValidity != null) {
            ssoCookie.setEmailValidity(SSOEmailValidity.valueOf(emailValidity));
        }
        String userType = getString(json, "userType");
        if (userType != null) {
            ssoCookie.setUserType(SSOUserType.valueOf(userType));
        }

        return ssoCookie;
    }

    private static String getString(JSONObject json, String key) {
        return json.has(key) ? json.getString(key) : null;
    }

}
