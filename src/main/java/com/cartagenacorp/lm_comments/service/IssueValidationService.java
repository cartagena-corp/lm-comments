package com.cartagenacorp.lm_comments.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

@Service
public class IssueValidationService {

    private static final Logger logger = LoggerFactory.getLogger(IssueValidationService.class);

    @Value("${issues.service.url}")
    private String issueServiceUrl;

    private final RestTemplate restTemplate;

    @Autowired
    public IssueValidationService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public boolean validateIssueExists(UUID issueId) {
        if (issueId == null) {
            logger.warn("Validación de issue fallida: issueId es null");
            return false;
        }
        try {
            String url = issueServiceUrl + "/validate/" + issueId;
            logger.debug("Validando existencia del issue con ID: {}", issueId);

            ResponseEntity<Boolean> response = restTemplate.getForEntity(url, Boolean.class);
            boolean exists = Boolean.TRUE.equals(response.getBody());

            logger.info("Resultado de validación del issue {}: {}", issueId, exists);
            return exists;
        } catch (HttpClientErrorException.NotFound ex) {
            logger.warn("Issue no encontrado con ID: {}", issueId);
            return false;
        } catch (Exception ex) {
            logger.error("Error al validar existencia del issue con ID {}: {}", issueId, ex.getMessage(), ex);
            return false;
        }
    }
}
