package com.cartagenacorp.lm_comments.service;

import com.cartagenacorp.lm_comments.dto.UserBasicDataDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserValidationService {

    private static final Logger logger = LoggerFactory.getLogger(UserValidationService.class);

    @Value("${auth.service.url}")
    private String authServiceUrl;

    private final RestTemplate restTemplate;

    @Autowired
    public UserValidationService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public boolean userExists(UUID userId, String token) {
        try {
            logger.debug("Validando existencia del usuario con ID: {}", userId);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + token);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Boolean> response = restTemplate.exchange(
                    authServiceUrl + "/validate/" + userId,
                    HttpMethod.GET,
                    entity,
                    Boolean.class
            );
            boolean exists = Boolean.TRUE.equals(response.getBody());
            logger.info("Resultado de validación del usuario {}: {}", userId, exists);
            return exists;
        } catch (Exception e) {
            logger.error("Error al validar el usuario con ID {}: {}", userId, e.getMessage(), e);
            return false;
        }
    }

    public UUID getUserIdFromToken(String token) {
        try {
            logger.debug("Obteniendo userId desde token...");

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + token);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    authServiceUrl + "/token",
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            if (response.getBody() != null) {
                UUID userId = UUID.fromString(response.getBody());
                logger.info("Token válido. ID de usuario extraído: {}", userId);
                return userId;
            }
            logger.warn("No se pudo extraer el ID de usuario desde el token");
            return null;
        } catch (Exception e) {
            logger.error("Error al obtener el userId desde el token: {}", e.getMessage(), e);
            return null;
        }
    }

    public Optional<List<UserBasicDataDto>> getUsersData(String token, List<String> ids) {
        try {
            logger.debug("Solicitando datos básicos de los usuarios: {}", ids);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + token);
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<List<String>> entity = new HttpEntity<>(ids, headers);

            ResponseEntity<List<UserBasicDataDto>> response = restTemplate.exchange(
                    authServiceUrl + "/users/batch",
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<>() {}
            );
            logger.info("Datos de usuarios obtenidos exitosamente. Cantidad: {}",
                    response.getBody() != null ? response.getBody().size() : 0);
            return Optional.ofNullable(response.getBody());
        } catch (Exception e) {
            logger.error("Error al obtener datos de usuarios: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }
}
