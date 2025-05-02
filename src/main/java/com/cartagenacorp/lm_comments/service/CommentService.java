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
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final FileAttachmentService fileAttachmentService;
    private final CommentMapper commentMapper;
    private final CommentResponsesRepository commentResponsesRepository;
    private final CommentResponsesMapper commentResponsesMapper;
    private final UserValidationService userValidationService;
    private final IssueValidationService issueValidationService;

    @Autowired
    public CommentService(CommentRepository commentRepository, FileAttachmentService fileAttachmentService,
                          CommentMapper commentMapper, CommentResponsesRepository commentResponsesRepository,
                          CommentResponsesMapper commentResponsesMapper, UserValidationService userValidationService,
                          IssueValidationService issueValidationService) {
        this.commentRepository = commentRepository;
        this.fileAttachmentService = fileAttachmentService;
        this.commentMapper = commentMapper;
        this.commentResponsesRepository = commentResponsesRepository;
        this.commentResponsesMapper = commentResponsesMapper;
        this.userValidationService = userValidationService;
        this.issueValidationService = issueValidationService;
    }

    @Transactional(readOnly = true)
    public PageResponseDTO<CommentDTO> getCommentsByIssueId(UUID issueId, Pageable pageable) {
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

        userValidationService.getUsersData(JwtContextHolder.getToken(),
                        userIds.stream().map(UUID::toString).toList())
                .ifPresent(users -> {
                    Map<UUID, UserBasicDataDto> userMap = users.stream()
                            .collect(Collectors.toMap(UserBasicDataDto::getId, Function.identity()));

                    dtoPage.forEach(dto -> {
                        UserBasicDataDto user = userMap.get(dto.getUserId());
                        dto.setUser(user);
                        dto.setResponsesCount(responseCounts.getOrDefault(dto.getId(), 0L).intValue());
                    });
                });

        return new PageResponseDTO<>(dtoPage);
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
        CommentDTO savedDto = commentMapper.commentToCommentDTO(savedComment);

        userValidationService.getUsersData(JwtContextHolder.getToken(), List.of(userId.toString()))
                .ifPresent(users -> {
                    if (!users.isEmpty()) {
                        savedDto.setUser(users.get(0));
                    }
                });

        return savedDto;
    }

    @Transactional(readOnly = true)
    public List<CommentResponsesDto> getResponsesByCommentId(UUID commentId) {

        List<CommentResponses> responses = commentResponsesRepository.findByCommentId(commentId);
        List<CommentResponsesDto> responsesDtos = responses.stream()
                .map(commentResponsesMapper::toDto)
                .collect(Collectors.toList());

        List<String> userIds = responsesDtos.stream()
                .map(dto -> dto.getUserId().toString())
                .distinct()
                .collect(Collectors.toList());

        Optional<List<UserBasicDataDto>> usersOpt = userValidationService.getUsersData(JwtContextHolder.getToken(), userIds);

        if (usersOpt.isPresent()) {
            Map<UUID, UserBasicDataDto> userMap = usersOpt.get().stream()
                    .collect(Collectors.toMap(UserBasicDataDto::getId, Function.identity()));

            responsesDtos.forEach(dto -> dto.setUser(userMap.get(dto.getUserId())));
        }

        return responsesDtos;
    }

    @Transactional
    public void deleteResponse(UUID responseId) {
        CommentResponses response = commentResponsesRepository.findById(responseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Response not found"));

        commentResponsesRepository.delete(response);
    }

    @Transactional
    public CommentResponsesDto saveResponse(CommentResponsesDto responseDto) {
        UUID userId = JwtContextHolder.getUserId();

        Comment comment = commentRepository.findById(responseDto.getCommentId())
                .orElseThrow(() -> new EntityNotFoundException("Comment not found"));

        responseDto.setUserId(userId);
        responseDto.setCreatedAt(LocalDateTime.now());
        CommentResponses response = commentResponsesMapper.toEntity(responseDto);
        response.setComment(comment);
        commentResponsesRepository.save(response);

        CommentResponsesDto responsesDto = commentResponsesMapper.toDto(response);

        userValidationService.getUsersData(JwtContextHolder.getToken(), List.of(userId.toString()))
                .ifPresent(users -> {
                    if (!users.isEmpty()) {
                        responsesDto.setUser(users.get(0));
                    }
                });

        return responsesDto;
    }
}
