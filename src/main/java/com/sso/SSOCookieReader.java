package com.sso;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
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

    /*
    The new cookie is Base64 encoded
    This is the json structure after decode
    {"userId":"","userMail":"","ticketId":"","firstName":"","lastName":"","emailValidity":"valid/invalid",
     "userType":"paying/registered","antiAbuseToken":"","r":"","p":"}
     */

    public static SSOCookie read(HttpServletRequest request) {
        try {
            return read(request.getCookies());
        } catch (Exception e) {
            LOGGER.error("failed to read sso cookie", e);
        }
        return null;
    }

    public static SSOCookie read(jakarta.servlet.http.HttpServletRequest request) {
        return read(
                Arrays.stream(request.getCookies())
                        .map(jakartaCookie ->
                                new Cookie(jakartaCookie.getName(), jakartaCookie.getValue()))
                        .toArray(Cookie[]::new)
        );
    }

    public static SSOCookie read(Cookie[] cookies) {

        try {
            Cookie cookie = getCookie(cookies);
            if (cookie != null && cookie.getValue() != null && !cookie.getValue().isEmpty()) {
                if (cookie.getName().equals(sso_token.name())) {
                    return parseNewFormat(cookie.getValue());
                } else {
                    return parseOldFormat(cookie.getValue(), cookies);
                }
            }

        } catch (Exception e) {
            LOGGER.error("failed to read sso cookie");
            //LOGGER.debug("failed to read sso cookie", e);
        }
        return null;
    }

    private static Cookie getCookie(Cookie[] cookies) {
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

    private static SSOCookie parseOldFormat(String cookieValue, Cookie[] cookies) {

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
        for (Cookie cookie : cookies) {
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
        return json.has(key) && !json.isNull(key) ? json.getString(key) : null;
    }

}
