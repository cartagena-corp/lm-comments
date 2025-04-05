package com.cartagenacorp.lm_comments.dto;

import com.cartagenacorp.lm_comments.entity.Comment;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FileAttachmentDTO {
    private UUID id;
    private String fileName;
    private String fileUrl;
    private UUID commentId;
}
