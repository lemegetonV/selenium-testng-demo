package com.demowebshop.models;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BillingAddress {

    private final String firstName;
    private final String lastName;
    private final String email;
    private final String country;
    private final String state;
    private final String city;
    private final String address1;
    private final String zip;
    private final String phone;
}
