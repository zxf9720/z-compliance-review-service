package com.shawn.compliance.service;

import com.shawn.compliance.dto.CustomerProfileResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class CustomerDataClientTest {

    @Test
    void getCustomerFetchesProfileFromConfiguredService() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(once(), requestTo("http://customer-service/customers/customer-1"))
                .andRespond(withSuccess("""
                    {
                      "customerId": "customer-1",
                      "name": "Alex Chen",
                      "age": 36,
                      "annualIncome": 85000,
                      "riskLevel": "LOW",
                      "investmentObjective": "GROWTH",
                      "kycStatus": "VERIFIED"
                    }
                    """, MediaType.APPLICATION_JSON));
        CustomerDataClient client = new CustomerDataClient(restTemplate, "http://customer-service");

        CustomerProfileResponse response = client.getCustomer("customer-1");

        assertThat(response.customerId()).isEqualTo("customer-1");
        assertThat(response.name()).isEqualTo("Alex Chen");
        assertThat(response.annualIncome()).isEqualTo(85000);
        assertThat(response.kycStatus()).isEqualTo("VERIFIED");
        server.verify();
    }
}
