package com.cartagenacorp.lm_comments.service;

import com.cartagenacorp.lm_comments.entity.Comment;
import com.cartagenacorp.lm_comments.entity.FileAttachment;
import com.cartagenacorp.lm_comments.exception.FileStorageException;
import com.cartagenacorp.lm_comments.repository.FileAttachmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class FileAttachmentService {

    private final FileAttachmentRepository fileAttachmentRepository;

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Value("${app.upload-access-url}")
    private String uploadAccessUrl;

    @Autowired
    public FileAttachmentService(FileAttachmentRepository fileAttachmentRepository) {
        this.fileAttachmentRepository = fileAttachmentRepository;
    }

    public List<FileAttachment> saveFiles(Comment comment, MultipartFile[] files) {
        List<FileAttachment> attachments = new ArrayList<>();

        for (MultipartFile file : files) {
            String fileUrl = saveFileToStorage(file);

            FileAttachment attachment = new FileAttachment();
            attachment.setComment(comment);
            attachment.setFileName(file.getOriginalFilename());
            attachment.setFileUrl(fileUrl);

            attachments.add(fileAttachmentRepository.save(attachment));
        }

        return attachments;
    }

    private String saveFileToStorage(MultipartFile file) {
        try {
            Files.createDirectories(Paths.get(uploadDir));

            String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Path filePath = Paths.get(uploadDir, fileName);

            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            return uploadAccessUrl + fileName;
        } catch (IOException e){
            throw new FileStorageException("Error saving file", e);
        }
    }
}
