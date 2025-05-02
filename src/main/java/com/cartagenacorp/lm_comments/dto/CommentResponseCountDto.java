package com.cartagenacorp.lm_comments.dto;

import java.util.UUID;

public record CommentResponseCountDto(UUID commentId, Long count) {}

