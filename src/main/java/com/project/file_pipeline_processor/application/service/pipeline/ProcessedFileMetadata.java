package com.project.file_pipeline_processor.application.service.pipeline;

import java.time.Instant;

public record ProcessedFileMetadata(
		String fileId,
		Long fileTableId,
		String fileName,
		String mimeType,
		long originalSize,
		String sha256Original,
		String sha256Decrypted,
		boolean decryptValidationOk,
		String encryptionAlgorithm,
		String ivBase64,
		String encryptedAesKeyBase64,
		Instant processedAt
) {
}
