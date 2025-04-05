package com.cartagenacorp.lm_comments.mapper;

import com.cartagenacorp.lm_comments.dto.CommentDTO;
import com.cartagenacorp.lm_comments.entity.Comment;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring", uses = {FileAttachmentMapper.class})
public interface CommentMapper {

    Comment commentDTOToComment(CommentDTO commentDTO);

    CommentDTO commentToCommentDTO(Comment comment);

    List<CommentDTO> commentsToCommentDTOs(List<Comment> comments);

    List<Comment> commentDTOsToComments(List<CommentDTO> commentDTOS);

}
