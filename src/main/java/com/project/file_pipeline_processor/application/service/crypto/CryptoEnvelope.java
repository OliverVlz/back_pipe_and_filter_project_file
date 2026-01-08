package com.project.file_pipeline_processor.application.service.crypto;

public record CryptoEnvelope(
		String algorithm,
		byte[] encryptedData,
		byte[] encryptedAesKey,
		byte[] iv
) {
}
