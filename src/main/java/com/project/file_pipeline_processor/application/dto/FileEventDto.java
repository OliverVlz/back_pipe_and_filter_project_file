package com.project.file_pipeline_processor.application.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO del evento consumido desde RabbitMQ.
 *
 * Compatible con el mensaje publicado por el Backend 1 (FileUploadMessage):
 * - fileUuid (UUID público)
 * - fileId (ID numérico interno en MySQL)
 * - fileName
 * - contentType
 */
public record FileEventDto(
		@NotBlank String fileUuid,
		Long fileId,
		@NotBlank String fileName,
		@NotBlank String contentType
) {
}
