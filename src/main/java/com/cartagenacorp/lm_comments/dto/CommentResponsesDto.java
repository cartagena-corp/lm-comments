package com.cartagenacorp.lm_comments.dto;

import com.cartagenacorp.lm_comments.entity.CommentResponses;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for {@link CommentResponses}
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommentResponsesDto implements Serializable {
    private UUID id;
    private UUID commentId;
    private UUID userId;
    private String text;
    private LocalDateTime createdAt;
    private UserBasicDataDto user;
}