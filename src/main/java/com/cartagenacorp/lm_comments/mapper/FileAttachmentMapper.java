package com.cartagenacorp.lm_comments.mapper;

import com.cartagenacorp.lm_comments.dto.FileAttachmentDTO;
import com.cartagenacorp.lm_comments.entity.FileAttachment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface FileAttachmentMapper {
    @Mapping(target = "commentId", source = "comment.id")
    FileAttachmentDTO fileToFileDTO(FileAttachment fileAttachment);

    @Mapping(target = "comment.id", source = "commentId")
    FileAttachment fileDTOToFile(FileAttachmentDTO fileAttachmentDTO);

    List<FileAttachmentDTO> filesToFileDTOs(List<FileAttachment> fileAttachments);

    List<FileAttachment> fileDTOsToFiles(List<FileAttachmentDTO> fileAttachmentDTOS);
}
