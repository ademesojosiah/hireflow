package com.hireflow.hireflow.service;

public interface EmailService {
    void sendOtp(String to, String otp);

    void sendCompanyWelcome(String to, String firstName, String companyName);
}
