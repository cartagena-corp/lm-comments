package com.cartagenacorp.lm_comments.controller;

import com.cartagenacorp.lm_comments.dto.CommentDTO;
import com.cartagenacorp.lm_comments.exception.FileStorageException;
import com.cartagenacorp.lm_comments.service.CommentService;
import com.cartagenacorp.lm_comments.util.RequiresPermission;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/api/comments")
public class CommentController {

    private final CommentService commentService;

    @Autowired
    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping("/{issueId}")
    @RequiresPermission({"COMMENT_CRUD", "COMMENT_READ"})
    public ResponseEntity<?> getCommentsByIssue(
            @PathVariable String issueId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        try {
            UUID uuid = UUID.fromString(issueId);
            return ResponseEntity.ok(commentService.getCommentsByIssueId(uuid, pageable));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid uuid");
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred: " + ex.getMessage());
        }
    }

    @DeleteMapping("/{commentId}")
    @RequiresPermission({"COMMENT_CRUD"})
    public ResponseEntity<?> deleteComment(@PathVariable String commentId) {
        try {
            UUID uuid = UUID.fromString(commentId);
            commentService.deleteComment(uuid);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid uuid");
        } catch (ResponseStatusException ex) {
            return ResponseEntity.status(ex.getStatusCode()).body(ex.getReason());
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred: " + ex.getMessage());
        }
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequiresPermission({"COMMENT_CRUD"})
    public ResponseEntity<?> createComment(
            @ModelAttribute CommentDTO commentDTO,
            @RequestPart(value = "files", required = false) MultipartFile[] files) {

        try {
            CommentDTO savedComment = commentService.saveComment(commentDTO, files);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedComment);
        } catch (FileStorageException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error saving files");
        } catch (ResponseStatusException ex) {
            return ResponseEntity.status(ex.getStatusCode()).body(ex.getReason());
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred");
        }
    }
}
