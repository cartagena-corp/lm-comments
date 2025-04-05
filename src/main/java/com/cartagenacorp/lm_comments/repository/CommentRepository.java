package com.cartagenacorp.lm_comments.repository;

import com.cartagenacorp.lm_comments.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CommentRepository extends JpaRepository<Comment, UUID> {
    List<Comment> findByIssueId(UUID issueId);
}
