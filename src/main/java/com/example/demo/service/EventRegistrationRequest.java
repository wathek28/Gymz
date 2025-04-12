package com.example.demo.service;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EventRegistrationRequest {
    private String firstName;
    private String email;
    private String phoneNumber;

    public EventRegistrationRequest() {
    }

    public EventRegistrationRequest(String firstName, String email, String phoneNumber) {
        this.firstName = firstName;
        this.email = email;
        this.phoneNumber = phoneNumber;
    }
}