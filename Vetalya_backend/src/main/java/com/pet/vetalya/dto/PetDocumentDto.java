package com.pet.vetalya.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.time.LocalDate;

public class PetDocumentDto {

    public record UploadRequest(
            @NotBlank(message = "Category is required")
            String category,

            @NotBlank(message = "File name is required")
            String fileName,

            @NotBlank(message = "File type is required")
            String fileType,

            @NotNull(message = "File size is required")
            Long fileSizeBytes,

            @NotBlank(message = "Storage path or URL is required")
            String storagePath,

            String description,

            LocalDate documentDate
    ) {}

    public record Response(
            String documentId,
            String petId,
            String category,
            String fileName,
            String fileType,
            Long fileSizeBytes,
            String storagePath,
            String description,
            LocalDate documentDate,
            String verificationStatus,
            String verifiedByVetId,
            Instant createdAt
    ) {}
}
