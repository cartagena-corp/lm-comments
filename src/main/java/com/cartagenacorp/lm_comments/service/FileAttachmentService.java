package com.cartagenacorp.lm_comments.service;

import com.cartagenacorp.lm_comments.entity.Comment;
import com.cartagenacorp.lm_comments.entity.FileAttachment;
import com.cartagenacorp.lm_comments.exception.FileStorageException;
import com.cartagenacorp.lm_comments.repository.FileAttachmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(FileAttachmentService.class);

    private final FileAttachmentRepository fileAttachmentRepository;

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Value("${app.upload-access-url}")
    private String uploadAccessUrl;

    public FileAttachmentService(FileAttachmentRepository fileAttachmentRepository) {
        this.fileAttachmentRepository = fileAttachmentRepository;
    }

    public List<FileAttachment> saveFiles(Comment comment, MultipartFile[] files) {
        logger.info("Iniciando guardado de archivos adjuntos para el comentario con ID: {}", comment.getId());
        List<FileAttachment> attachments = new ArrayList<>();

        for (MultipartFile file : files) {
            logger.info("Procesando archivo: {}", file.getOriginalFilename());
            String fileName = saveFileToStorage(file);
            String fileUrl = uploadAccessUrl + fileName;

            FileAttachment attachment = new FileAttachment();
            attachment.setComment(comment);
            attachment.setFileName(fileName);
            attachment.setFileUrl(fileUrl);

            FileAttachment saved = fileAttachmentRepository.save(attachment);
            attachments.add(saved);
            logger.info("Archivo guardado en base de datos con ID: {}, URL: {}", saved.getId(), fileUrl);
        }

        logger.info("Todos los archivos han sido procesados correctamente.");
        return attachments;
    }

    private String saveFileToStorage(MultipartFile file) {
        try {
            Path directory = Paths.get(uploadDir);
            if (!Files.exists(directory)) {
                logger.info("Directorio de subida no existe. Creando: {}", directory.toAbsolutePath());
                Files.createDirectories(directory);
            }

            String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Path filePath = directory.resolve(fileName);

            logger.info("Guardando archivo físicamente en: {}", filePath.toAbsolutePath());
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);


            String fileAccessUrl = uploadAccessUrl + fileName;
            logger.info("Archivo guardado correctamente. URL de acceso: {}", fileAccessUrl);
            return fileName;
        } catch (IOException e){
            logger.error("Error al guardar archivo: {}", file.getOriginalFilename(), e);
            throw new FileStorageException("Error guardando el archivo: " + file.getOriginalFilename(), e);
        }
    }
}
