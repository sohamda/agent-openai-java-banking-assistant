// Copyright (c) Microsoft. All rights reserved.
package com.microsoft.openai.samples.assistant.controller.content;


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import com.microsoft.openai.samples.assistant.proxy.BlobStorageProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class ContentController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContentController.class);

    private static final Pattern SAFE_FILENAME = Pattern.compile("^[a-zA-Z0-9._-]+$");
    private static final List<String> ALLOWED_EXTENSIONS = List.of("pdf", "jpg", "jpeg", "png");
    private static final List<String> ALLOWED_CONTENT_TYPES = List.of(
            "application/pdf", "image/jpeg", "image/png");
    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024; // 10 MB

    private final BlobStorageProxy blobStorageProxy;

    ContentController(BlobStorageProxy blobStorageProxy) {
        this.blobStorageProxy = blobStorageProxy;
    }

    @GetMapping("/api/content/{fileName}")
    public ResponseEntity<InputStreamResource> getContent(@PathVariable String fileName) {
        LOGGER.info("Received request for content with name [{}]", fileName);

        if (!StringUtils.hasText(fileName)) {
            LOGGER.warn("file name cannot be null");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }

        if (!SAFE_FILENAME.matcher(fileName).matches()) {
            LOGGER.warn("Invalid file name rejected: [{}]", fileName);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }

        String mimeType = URLConnection.guessContentTypeFromName(fileName);

        MediaType contentType = new MediaType(MimeTypeUtils.parseMimeType(mimeType));

        InputStream fileInputStream;

        try {
            fileInputStream = new ByteArrayInputStream(blobStorageProxy.getFileAsBytes(fileName));
        } catch (IOException ex) {
            LOGGER.error("Cannot retrieve file [{}] from blob.{}", fileName, ex.getMessage());
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .header("Content-Disposition", "inline; filename=%s".formatted(fileName))
                .contentType(contentType)
                .body(new InputStreamResource(fileInputStream));
    }

    @PostMapping("/api/content")
    public ResponseEntity<String> uploadContent(@RequestParam("file") MultipartFile file) {
        LOGGER.info("Received request to upload a file [{}]", file.getOriginalFilename());

        if (file.isEmpty()) {
            LOGGER.warn("Uploaded file is empty");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Uploaded file is empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            LOGGER.warn("Uploaded file exceeds maximum allowed size");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("File size exceeds the 10 MB limit");
        }

        String originalFilename = file.getOriginalFilename();
        if (!StringUtils.hasText(originalFilename) || !SAFE_FILENAME.matcher(originalFilename).matches()) {
            LOGGER.warn("Invalid upload file name rejected: [{}]", originalFilename);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid file name");
        }

        String extension = originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase()
                : "";
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            LOGGER.warn("Disallowed file extension rejected: [{}]", extension);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("File type not allowed. Allowed types: pdf, jpg, jpeg, png");
        }

        String contentType = file.getContentType();
        if (!StringUtils.hasText(contentType) || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            LOGGER.warn("Disallowed content type rejected: [{}]", contentType);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("File content type not allowed");
        }

        String safeStorageName = UUID.randomUUID() + "." + extension;

        try {
            byte[] bytes = file.getBytes();
            blobStorageProxy.storeFile(bytes, safeStorageName);
        } catch (IOException ex) {
            LOGGER.error("Cannot store file [{}] to blob.{}", originalFilename, ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error occurred while storing file");
        }

        return ResponseEntity.ok(safeStorageName);
    }

}
