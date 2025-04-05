package com.cartagenacorp.lm_comments.config;

import com.cartagenacorp.lm_comments.mapper.CommentMapper;
import com.cartagenacorp.lm_comments.mapper.FileAttachmentMapper;
import org.mapstruct.factory.Mappers;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MapperConfig {
    @Bean
    public CommentMapper commentMapper() {
        return Mappers.getMapper(CommentMapper.class);
    }
    @Bean
    public FileAttachmentMapper fileAttachmentMapper() {
        return Mappers.getMapper(FileAttachmentMapper.class);
    }
}
