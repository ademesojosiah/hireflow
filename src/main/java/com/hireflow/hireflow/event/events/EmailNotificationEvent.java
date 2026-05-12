package com.hireflow.hireflow.event.events;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmailNotificationEvent {

    public static final String OTP_VERIFICATION = "OTP_VERIFICATION";
    public static final String COMPANY_WELCOME = "COMPANY_WELCOME";

    private String type;
    private String to;
    private String otp;
    private String firstName;
    private String companyName;
}
