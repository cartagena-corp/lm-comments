package com.cartagenacorp.lm_comments.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class IssueDtoResponse implements Serializable {
    UUID id;
    String title;
    UUID organizationId;
}