package com.cartagenacorp.lm_comments.service;

import com.cartagenacorp.lm_comments.dto.IssueDtoResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

@Service
public class IssueExternalService {

    private static final Logger logger = LoggerFactory.getLogger(IssueExternalService.class);

    @Value("${issues.service.url}")
    private String issueServiceUrl;

    private final RestTemplate restTemplate;

    public IssueExternalService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public boolean validateIssueExists(UUID issueId, String token) {
        if (issueId == null) {
            logger.warn("Validación de issue fallida: issueId es null");
            return false;
        }
        logger.debug("Validando la existencia del issue con ID: {}", issueId);
        try {
            String url = issueServiceUrl + "/validate/" + issueId;

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Boolean> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    Boolean.class
            );

            logger.info("Resultado de la validación de existencia del issue con ID {}: {}", issueId, response);
            return Boolean.TRUE.equals(response.getBody());
        } catch (HttpClientErrorException.Unauthorized ex) {
            logger.warn("Token no autorizado para validar la existencia del issue: {}", ex.getMessage());
        } catch (HttpClientErrorException.Forbidden ex) {
            logger.warn("No tiene permisos para  validar la existencia del issue: {}", ex.getMessage());
        } catch (ResourceAccessException ex) {
            logger.warn("El servicio externo no esta disponible: {}",ex.getMessage());
        }  catch (Exception ex) {
            logger.error("Error al validar la existencia del issue: {}", ex.getMessage(), ex);
        }
        return false;
    }

    public boolean validateIssueAccess(UUID issueId, String token) {
        String url = issueServiceUrl + "/" + issueId;

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            HttpEntity<Void> request = new HttpEntity<>(headers);

            restTemplate.exchange(url, HttpMethod.GET, request, IssueDtoResponse.class);
            return true;
        } catch (HttpClientErrorException.Unauthorized ex) {
            logger.warn("Token no autorizado para validar el acceso a la issue: {}", ex.getMessage());
        } catch (HttpClientErrorException.Forbidden ex) {
            logger.warn("No tiene permisos para  validar el acceso a la issue: {}", ex.getMessage());
        } catch (ResourceAccessException ex) {
            logger.warn("El servicio externo no esta disponible: {}",ex.getMessage());
        }  catch (Exception ex) {
            logger.error("Error al validar el acceso a la issue: {}", ex.getMessage(), ex);
        }
        return false;
    }
}
