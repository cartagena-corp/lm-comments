package com.cartagenacorp.lm_comments.repository;

import com.cartagenacorp.lm_comments.entity.CommentResponses;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface CommentResponsesRepository extends JpaRepository<CommentResponses, UUID> {
    List<CommentResponses> findByCommentId(UUID commentId);

    @Query("SELECT cr.comment.id, COUNT(cr) FROM CommentResponses cr WHERE cr.comment.id IN :commentIds GROUP BY cr.comment.id")
    List<Object[]> countByCommentIds(@Param("commentIds") List<UUID> commentIds);

}