package com.cartagenacorp.lm_comments.service;

import com.cartagenacorp.lm_comments.dto.CommentDTO;
import com.cartagenacorp.lm_comments.dto.PageResponseDTO;
import com.cartagenacorp.lm_comments.dto.UserBasicDataDto;
import com.cartagenacorp.lm_comments.entity.Comment;
import com.cartagenacorp.lm_comments.entity.CommentResponses;
import com.cartagenacorp.lm_comments.dto.CommentResponsesDto;
import com.cartagenacorp.lm_comments.entity.FileAttachment;
import com.cartagenacorp.lm_comments.mapper.CommentMapper;
import com.cartagenacorp.lm_comments.mapper.CommentResponsesMapper;
import com.cartagenacorp.lm_comments.repository.CommentRepository;
import com.cartagenacorp.lm_comments.repository.CommentResponsesRepository;
import com.cartagenacorp.lm_comments.util.JwtContextHolder;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CommentService {

    private static final Logger logger = LoggerFactory.getLogger(CommentService.class);

    @Value("${file.upload-dir}")
    private String uploadDir;

    private final CommentRepository commentRepository;
    private final FileAttachmentService fileAttachmentService;
    private final CommentMapper commentMapper;
    private final CommentResponsesRepository commentResponsesRepository;
    private final CommentResponsesMapper commentResponsesMapper;
    private final UserExternalService userExternalService;
    private final IssueExternalService issueExternalService;

    @Autowired
    public CommentService(CommentRepository commentRepository, FileAttachmentService fileAttachmentService,
                          CommentMapper commentMapper, CommentResponsesRepository commentResponsesRepository,
                          CommentResponsesMapper commentResponsesMapper, UserExternalService userExternalService,
                          IssueExternalService issueExternalService) {
        this.commentRepository = commentRepository;
        this.fileAttachmentService = fileAttachmentService;
        this.commentMapper = commentMapper;
        this.commentResponsesRepository = commentResponsesRepository;
        this.commentResponsesMapper = commentResponsesMapper;
        this.userExternalService = userExternalService;
        this.issueExternalService = issueExternalService;
    }

    @Transactional
    public CommentDTO saveComment(CommentDTO commentDTO, MultipartFile[] files) {
        UUID userId = JwtContextHolder.getUserId();
        String token = JwtContextHolder.getToken();
        UUID organizationId = JwtContextHolder.getOrganizationId();

        if (!issueExternalService.validateIssueExists(commentDTO.getIssueId(), token)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "The issue ID provided is not valid");
        }

        if (!issueExternalService.validateIssueAccess(commentDTO.getIssueId(), JwtContextHolder.getToken())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to access the content of this issue");
        }

        commentDTO.setUserId(userId);
        commentDTO.setOrganizationId(organizationId);
        commentDTO.setCreatedAt(LocalDateTime.now());
        Comment comment = commentMapper.commentDTOToComment(commentDTO);
        commentRepository.save(comment);

        if (files != null) {
            List<FileAttachment> attachments = fileAttachmentService.saveFiles(comment, files);
            comment.setAttachments(attachments);
        }
        Comment savedComment = commentRepository.save(comment);
        CommentDTO savedDto = commentMapper.commentToCommentDTO(savedComment);

        List<UserBasicDataDto> users = userExternalService.getUsersData(JwtContextHolder.getToken(), List.of(userId.toString()));
        if (!users.isEmpty()) {
            savedDto.setUser(users.get(0));
        }

        return savedDto;
    }

    @Transactional(readOnly = true)
    public PageResponseDTO<CommentDTO> getCommentsByIssueId(UUID issueId, Pageable pageable) {
        if (!issueExternalService.validateIssueAccess(issueId, JwtContextHolder.getToken())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to access the content of this issue");
        }

        Page<Comment> commentPage = commentRepository.findByIssueId(issueId, pageable);
        Page<CommentDTO> dtoPage = commentPage.map(commentMapper::commentToCommentDTO);

        List<UUID> commentIds = dtoPage.getContent().stream()
                .map(CommentDTO::getId)
                .toList();

        Map<UUID, Long> responseCounts = commentResponsesRepository.countByCommentIds(commentIds).stream()
                .collect(Collectors.toMap(
                        tuple -> (UUID) tuple[0],
                        tuple -> (Long) tuple[1]
                ));

        List<UUID> userIds = dtoPage.getContent().stream()
                .map(CommentDTO::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        List<UserBasicDataDto> users = userExternalService.getUsersData(
                JwtContextHolder.getToken(),
                userIds.stream().map(UUID::toString).toList()
        );

        if (!users.isEmpty()) {
            Map<UUID, UserBasicDataDto> userMap = users.stream()
                    .collect(Collectors.toMap(UserBasicDataDto::getId, Function.identity()));

            dtoPage.forEach(dto -> {
                UserBasicDataDto user = userMap.get(dto.getUserId());
                dto.setUser(user);
                dto.setResponsesCount(responseCounts.getOrDefault(dto.getId(), 0L).intValue());
            });
        }

        return new PageResponseDTO<>(dtoPage);
    }

    @Transactional
    public void deleteComment(UUID commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found"));

        if (!issueExternalService.validateIssueAccess(comment.getIssueId(), JwtContextHolder.getToken())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to access the content of this issue");
        }

        List<FileAttachment> attachments = comment.getAttachments();
        if (attachments != null) {
            for (FileAttachment attachment : attachments) {
                try {
                    Path filePath = Paths.get(uploadDir, attachment.getFileName());
                    Files.deleteIfExists(filePath);
                } catch (IOException e) {
                    logger.warn("The file could not be deleted: {}", attachment.getFileUrl(), e);
                }
            }
        }

        commentRepository.delete(comment);
    }

    @Transactional
    public CommentResponsesDto saveResponse(CommentResponsesDto responseDto) {
        UUID userId = JwtContextHolder.getUserId();

        Comment comment = commentRepository.findById(responseDto.getCommentId())
                .orElseThrow(() -> new EntityNotFoundException("Comment not found"));

        if (!issueExternalService.validateIssueAccess(comment.getIssueId(), JwtContextHolder.getToken())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to access the content of this issue");
        }

        responseDto.setUserId(userId);
        responseDto.setCreatedAt(LocalDateTime.now());
        CommentResponses response = commentResponsesMapper.toEntity(responseDto);
        response.setComment(comment);
        commentResponsesRepository.save(response);

        CommentResponsesDto responsesDto = commentResponsesMapper.toDto(response);

        List<UserBasicDataDto> users = userExternalService.getUsersData(JwtContextHolder.getToken(), List.of(userId.toString()));
        if (!users.isEmpty()) {
            responsesDto.setUser(users.get(0));
        }

        return responsesDto;
    }

    @Transactional(readOnly = true)
    public List<CommentResponsesDto> getResponsesByCommentId(UUID commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found"));

        if (!issueExternalService.validateIssueAccess(comment.getIssueId(), JwtContextHolder.getToken())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to access the content of this issue");
        }

        List<CommentResponses> responses = commentResponsesRepository.findByCommentId(commentId);
        List<CommentResponsesDto> responsesDtos = responses.stream()
                .map(commentResponsesMapper::toDto)
                .collect(Collectors.toList());

        List<String> userIds = responsesDtos.stream()
                .map(dto -> dto.getUserId().toString())
                .distinct()
                .collect(Collectors.toList());

        List<UserBasicDataDto> users = userExternalService.getUsersData(JwtContextHolder.getToken(), userIds);

        if (!users.isEmpty()) {
            Map<UUID, UserBasicDataDto> userMap = users.stream()
                    .collect(Collectors.toMap(UserBasicDataDto::getId, Function.identity()));

            responsesDtos.forEach(dto -> dto.setUser(userMap.get(dto.getUserId())));
        }

        return responsesDtos;
    }

    @Transactional
    public void deleteResponse(UUID responseId) {
        CommentResponses response = commentResponsesRepository.findById(responseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Response not found"));

        Comment comment = commentRepository.findById(response.getComment().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found"));

        if (!issueExternalService.validateIssueAccess(comment.getIssueId(), JwtContextHolder.getToken())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to access the content of this issue");
        }

        commentResponsesRepository.delete(response);
    }

    @Transactional
    public void deleteCommentsByIssueId(UUID issueId) {
        if (!issueExternalService.validateIssueAccess(issueId, JwtContextHolder.getToken())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You do not have permission to access the content of this issue");
        }

        List<Comment> comments = commentRepository.findByIssueId(issueId);

        for (Comment comment : comments) {
            if (comment.getAttachments() != null) {
                for (FileAttachment attachment : comment.getAttachments()) {
                    try {
                        Path filePath = Paths.get(uploadDir, attachment.getFileName());
                        Files.deleteIfExists(filePath);
                    } catch (IOException e) {
                        logger.warn("The file could not be deleted: {}", attachment.getFileUrl(), e);
                    }
                }
            }

            commentRepository.delete(comment);
        }
    }

}
