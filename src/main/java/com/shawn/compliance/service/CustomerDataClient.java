package com.shawn.compliance.service;

import com.shawn.compliance.dto.CustomerProfileResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class CustomerDataClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    @Autowired
    public CustomerDataClient(@Value("${services.customer-data.base-url}") String baseUrl) {
        this(new RestTemplate(), baseUrl);
    }

    CustomerDataClient(RestTemplate restTemplate, String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    public CustomerProfileResponse getCustomer(String customerId) {
        return restTemplate.getForObject(
                baseUrl + "/customers/" + customerId,
                CustomerProfileResponse.class
        );
    }
}
