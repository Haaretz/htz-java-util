package com.sso;

public class SSOCookie {

    private String userId;
    private SSOUserType userType;
    private String userMail;
    private SSOEmailValidity emailValidated;
    private String firstName;
    private String lastName;
    private String antiAbuseToken;
    private String ticketId;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public SSOUserType getUserType() {
        return userType;
    }

    public void setUserType(SSOUserType userType) {
        this.userType = userType;
    }

    public String getUserMail() {
        return userMail;
    }

    public void setUserMail(String userMail) {
        this.userMail = userMail;
    }

    public SSOEmailValidity getEmailValidated() {
        return emailValidated;
    }

    public void setEmailValidated(SSOEmailValidity emailValidated) {
        this.emailValidated = emailValidated;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getAntiAbuseToken() {
        return antiAbuseToken;
    }

    public void setAntiAbuseToken(String antiAbuseToken) {
        this.antiAbuseToken = antiAbuseToken;
    }

    public String getTicketId() {
        return ticketId;
    }

    public void setTicketId(String ticketId) {
        this.ticketId = ticketId;
    }
}
