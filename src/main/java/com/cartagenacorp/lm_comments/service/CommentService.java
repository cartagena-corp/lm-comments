package com.cartagenacorp.lm_comments.service;

import com.cartagenacorp.lm_comments.dto.CommentDTO;
import com.cartagenacorp.lm_comments.dto.PageResponseDTO;
import com.cartagenacorp.lm_comments.entity.Comment;
import com.cartagenacorp.lm_comments.entity.FileAttachment;
import com.cartagenacorp.lm_comments.mapper.CommentMapper;
import com.cartagenacorp.lm_comments.repository.CommentRepository;
import com.cartagenacorp.lm_comments.util.JwtContextHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final FileAttachmentService fileAttachmentService;
    private final CommentMapper commentMapper;
    private final UserValidationService userValidationService;
    private final IssueValidationService issueValidationService;

    @Autowired
    public CommentService(CommentRepository commentRepository, FileAttachmentService fileAttachmentService,
                          CommentMapper commentMapper, UserValidationService userValidationService, IssueValidationService issueValidationService) {
        this.commentRepository = commentRepository;
        this.fileAttachmentService = fileAttachmentService;
        this.commentMapper = commentMapper;
        this.userValidationService = userValidationService;
        this.issueValidationService = issueValidationService;
    }

    @Transactional(readOnly = true)
    public PageResponseDTO<CommentDTO> getCommentsByIssueId(UUID issueId, Pageable pageable) {
        Page<Comment> commentPage = commentRepository.findByIssueId(issueId, pageable);
        return new PageResponseDTO<>(commentPage.map(comment -> commentMapper.commentToCommentDTO(comment)));
    }

    @Transactional
    public void deleteComment(UUID commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found"));

        List<FileAttachment> attachments = comment.getAttachments();
        if (attachments != null) {
            for (FileAttachment attachment : attachments) {
                try {
                    Files.deleteIfExists(Paths.get(attachment.getFileUrl()));
                } catch (IOException e) {
                    System.err.println("The file could not be deleted: " + attachment.getFileUrl());
                    e.printStackTrace();
                }
            }
        }

        commentRepository.delete(comment);
    }

    @Transactional
    public CommentDTO saveComment(CommentDTO commentDTO, MultipartFile[] files) {
        UUID userId = JwtContextHolder.getUserId();

        if (!issueValidationService.validateIssueExists(commentDTO.getIssueId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "The issue ID provided is not valid");
        }

        commentDTO.setUserId(userId);
        commentDTO.setCreatedAt(LocalDateTime.now());
        Comment comment = commentMapper.commentDTOToComment(commentDTO);
        commentRepository.save(comment);

        if (files != null) {
            List<FileAttachment> attachments = fileAttachmentService.saveFiles(comment, files);
            comment.setAttachments(attachments);
        }
        Comment savedComment = commentRepository.save(comment);

        return commentMapper.commentToCommentDTO(savedComment);
    }
}
