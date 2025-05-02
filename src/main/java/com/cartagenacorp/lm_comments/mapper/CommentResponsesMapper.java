package com.cartagenacorp.lm_comments.mapper;

import com.cartagenacorp.lm_comments.entity.CommentResponses;
import com.cartagenacorp.lm_comments.dto.CommentResponsesDto;
import org.mapstruct.*;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface CommentResponsesMapper {


    @Mapping(source = "commentId", target = "comment.id")
    CommentResponses toEntity(CommentResponsesDto commentResponsesDto);

    @Mapping(source = "comment.id", target = "commentId")
    CommentResponsesDto toDto(CommentResponses commentResponses);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    CommentResponses partialUpdate(CommentResponsesDto commentResponsesDto, @MappingTarget CommentResponses commentResponses);
}