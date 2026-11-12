package com.sso;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.json.JSONObject;

/**
 * Servlet-API-independent cookie parsing shared by {@link SSOCookieReader} (javax)
 * and {@link JakartaSSOCookieReader} (jakarta).
 */
final class SSOCookieParser {

    /** Cookie names in lookup priority order. */
    enum CookieNames {
        sso_token, _hs_sso, tmsso, engsso
    }

    private SSOCookieParser() {
    }

    /*
    The new cookie is Base64 encoded
    This is the json structure after decode
    {"userId":"","userMail":"","ticketId":"","firstName":"","lastName":"","emailValidity":"valid/invalid",
     "userType":"paying/registered","antiAbuseToken":"","r":"","p":"}
     */
    static SSOCookie parse(String name, String value, boolean paying) {
        if (name.equals(CookieNames.sso_token.name())) {
            return parseNewFormat(value);
        }
        return parseOldFormat(value, paying);
    }

    static boolean isPayingCookieName(String name) {
        return name.endsWith("Pusr");
    }

    private static SSOCookie parseOldFormat(String cookieValue, boolean paying) {

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

        ssoCookie.setUserType(paying ? SSOUserType.paying : SSOUserType.registered);
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
