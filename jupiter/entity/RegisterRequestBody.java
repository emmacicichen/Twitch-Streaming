package com.laioffer.jupiter.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class RegisterRequestBody {
    private final String userId;
    private final String password;
    private final String firstName;
    private final String lastName;

    public String getUserId() {
        return userId;
    }

    public String getPassword() {
        return password;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    @JsonCreator
    public RegisterRequestBody(@JsonProperty("user_id") String userId, @JsonProperty("password") String password,
                               @JsonProperty("first_name") String firstName, @JsonProperty("last_name") String lastName) {
        this.userId = userId;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
    }
}
