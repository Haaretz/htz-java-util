package com.sso;

public class SSOCookie {

    private String userId;
    private SSOUserType userType;
    private String userMail;
    private SSOEmailValidity emailValidity;
    private String firstName;
    private String lastName;
    private String antiAbuseToken;
    private String ticketId;

    public SSOCookie() {
    }

    public SSOCookie(String userId, SSOUserType userType, String userMail,
                     SSOEmailValidity emailValidity, String firstName, String lastName,
                     String antiAbuseToken, String ticketId) {

        this.userId = userId;
        this.userType = userType;
        this.userMail = userMail;
        this.emailValidity = emailValidity;
        this.firstName = firstName;
        this.lastName = lastName;
        this.antiAbuseToken = antiAbuseToken;
        this.ticketId = ticketId;
    }

    public String getUserId() {
        return userId;
    }

    public SSOCookie setUserId(String userId) {
        this.userId = userId;
        return this;
    }

    public SSOUserType getUserType() {
        return userType;
    }

    public SSOCookie setUserType(SSOUserType userType) {
        this.userType = userType;
        return this;
    }

    public String getUserMail() {
        return userMail;
    }

    public SSOCookie setUserMail(String userMail) {
        this.userMail = userMail;
        return this;
    }

    public SSOEmailValidity getEmailValidity() {
        return emailValidity;
    }

    public SSOCookie setEmailValidity(SSOEmailValidity emailValidity) {
        this.emailValidity = emailValidity;
        return this;
    }

    public String getFirstName() {
        return firstName;
    }

    public SSOCookie setFirstName(String firstName) {
        this.firstName = firstName;
        return this;
    }

    public String getLastName() {
        return lastName;
    }

    public SSOCookie setLastName(String lastName) {
        this.lastName = lastName;
        return this;
    }

    public String getAntiAbuseToken() {
        return antiAbuseToken;
    }

    public SSOCookie setAntiAbuseToken(String antiAbuseToken) {
        this.antiAbuseToken = antiAbuseToken;
        return this;
    }

    public String getTicketId() {
        return ticketId;
    }

    public SSOCookie setTicketId(String ticketId) {
        this.ticketId = ticketId;
        return this;
    }
}
