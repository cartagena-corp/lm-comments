package com.cartagenacorp.lm_comments.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

@Service
public class IssueValidationService {

    @Value("${issues.service.url}")
    private String issueServiceUrl;

    private final RestTemplate restTemplate;

    @Autowired
    public IssueValidationService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public boolean validateIssueExists(UUID issueId) {
        if (issueId == null) {
            return false;
        }
        try {
            String url = issueServiceUrl + "/validate/" + issueId;
            ResponseEntity<Boolean> response = restTemplate.getForEntity(url, Boolean.class);
            return Boolean.TRUE.equals(response.getBody());
        } catch (HttpClientErrorException.NotFound ex) {
            return false;
        } catch (Exception ex) {
            System.out.println("Error validating issue: " + ex.getMessage());
            return false;
        }
    }
}
