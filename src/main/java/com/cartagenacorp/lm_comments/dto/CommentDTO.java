package com.cartagenacorp.lm_comments.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommentDTO {
    private UUID id;
    private UUID issueId;
    private UUID userId;
    private String text;
    private LocalDateTime createdAt;
    private List<FileAttachmentDTO> attachments;
    private UserBasicDataDto user;
    private Integer responsesCount;
    private UUID organizationId;
}
