package com.cartagenacorp.lm_comments.service;

import com.cartagenacorp.lm_comments.dto.UserBasicDataDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;

@Service
public class UserExternalService {

    private static final Logger logger = LoggerFactory.getLogger(UserExternalService.class);

    @Value("${auth.service.url}")
    private String authServiceUrl;

    private final RestTemplate restTemplate;

    public UserExternalService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public List<UserBasicDataDto> getUsersData(String token, List<String> ids) {
        logger.debug("Obteniendo información de los usuarios con IDs: {}", ids);
        try {
            String url = authServiceUrl + "/users/batch";

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<List<String>> entity = new HttpEntity<>(ids, headers);

            ResponseEntity<List<UserBasicDataDto>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<List<UserBasicDataDto>>() {}
            );
            logger.info("Resultado de la obtención de información de los usuarios con IDs {} : {}", ids, response);

            List<UserBasicDataDto> result = response.getBody();
            return result != null ? result : Collections.emptyList();
        } catch (HttpClientErrorException.Unauthorized ex) {
            logger.warn("Token no autorizado para obtener la información de los usuarios con IDs {}: {}", ids, ex.getMessage());
        } catch (HttpClientErrorException.Forbidden ex) {
            logger.warn("No tiene permisos para  obtener la información de los usuarios con IDs {}: {}", ids, ex.getMessage());
        } catch (ResourceAccessException ex) {
            logger.warn("El servicio externo no esta disponible: {}",ex.getMessage());
        }  catch (Exception ex) {
            logger.error("Error al obtener información de los usuarios con IDs {}: {}", ids, ex.getMessage(), ex);
        }
        return Collections.emptyList();
    }
}
