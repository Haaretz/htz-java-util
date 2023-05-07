package com.sso;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.json.JSONObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sso.SSOCookieReader.CookieNames.sso_token;


public class SSOCookieReader {

    private static final Logger LOGGER = LoggerFactory.getLogger(SSOCookieReader.class);

    enum CookieNames {
        sso_token, _hs_sso, tmsso, engsso
    }

    enum UserTypes {
        paying, registered
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
            String[] parts = field.split("=");
            if (parts.length == 2) {
                switch (parts[0]) {
                    case "userId":
                        ssoCookie.setUserId(parts[1]);
                        break;
                    case "userName":
                        ssoCookie.setUserMail(parts[1]);
                        break;
                    case "ticketId":
                        ssoCookie.setTicketId(parts[1]);
                        break;
                    case "firstName":
                        ssoCookie.setFirstName(parts[1]);
                        break;
                    case "lastName":
                        ssoCookie.setLastName(parts[1]);
                        break;
                    case "emailValidity":
                        ssoCookie.setEmailValidated(parts[1].equalsIgnoreCase("valid"));
                        break;
                    case "antiAbuseToken":
                        ssoCookie.setAntiAbuseToken(parts[1]);
                        break;
                }
            }
        }

        ssoCookie.setUserType(UserTypes.registered.name());
        for (Cookie cookie : request.getCookies()) {
            if (cookie.getName().endsWith("Pusr")) {
                ssoCookie.setUserType(UserTypes.paying.name());
            }
        }


        return ssoCookie;
    }

    private static SSOCookie parseNewFormat(String cookieValue) throws UnsupportedEncodingException {

        byte[] bytes = Base64.getDecoder().decode(cookieValue);
        cookieValue = new String(bytes, StandardCharsets.UTF_8);
        JSONObject json = new JSONObject(cookieValue);
        SSOCookie ssoCookie = new SSOCookie();

        ssoCookie.setUserId(json.getString("userId"));
        ssoCookie.setUserMail(json.getString("userName"));
        ssoCookie.setTicketId(json.getString("ticketId"));
        ssoCookie.setFirstName(json.getString("firstName"));
        ssoCookie.setLastName(json.getString("lastName"));
        ssoCookie.setEmailValidated("valid".equalsIgnoreCase(json.getString("emailValidity")));
        ssoCookie.setAntiAbuseToken(json.getString("antiAbuseToken"));
        ssoCookie.setUserType(json.getString("userType"));

        return ssoCookie;
    }
}
