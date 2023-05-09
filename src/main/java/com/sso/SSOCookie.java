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

    public String getUserId() {
        return userId;
    }

    void setUserId(String userId) {
        this.userId = userId;
    }

    public SSOUserType getUserType() {
        return userType;
    }

    void setUserType(SSOUserType userType) {
        this.userType = userType;
    }

    public String getUserMail() {
        return userMail;
    }

    void setUserMail(String userMail) {
        this.userMail = userMail;
    }

    public SSOEmailValidity getEmailValidity() {
        return emailValidity;
    }

    void setEmailValidity(SSOEmailValidity emailValidity) {
        this.emailValidity = emailValidity;
    }

    public String getFirstName() {
        return firstName;
    }

    void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getAntiAbuseToken() {
        return antiAbuseToken;
    }

    void setAntiAbuseToken(String antiAbuseToken) {
        this.antiAbuseToken = antiAbuseToken;
    }

    public String getTicketId() {
        return ticketId;
    }

    void setTicketId(String ticketId) {
        this.ticketId = ticketId;
    }
}
