package com.project.file_pipeline_processor.application.service.pipeline;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class FileObjectKeyFactory {

	private final String originalPrefix;
	private final String encryptedPrefix;
	private final String metadataPrefix;

	public FileObjectKeyFactory(
			@Value("${minio.keys.original-prefix:original/}") String originalPrefix,
			@Value("${minio.keys.encrypted-prefix:encrypted/}") String encryptedPrefix,
			@Value("${minio.keys.metadata-prefix:metadata/}") String metadataPrefix
	) {
		this.originalPrefix = normalizePrefix(originalPrefix, "original/");
		this.encryptedPrefix = normalizePrefix(encryptedPrefix, "encrypted/");
		this.metadataPrefix = normalizePrefix(metadataPrefix, "metadata/");
	}

	public String originalKey(String fileId) {
		return originalPrefix + fileId + ".bin";
	}

	public String encryptedKey(String fileId) {
		return encryptedPrefix + fileId + ".enc";
	}

	public String metadataKey(String fileId) {
		return metadataPrefix + fileId + ".json";
	}

	private static String normalizePrefix(String raw, String fallback) {
		String p = (raw == null || raw.isBlank()) ? fallback : raw.trim();
		// Evita prefijos con "/" inicial para mantener llaves limpias.
		while (p.startsWith("/")) {
			p = p.substring(1);
		}
		if (!p.endsWith("/")) {
			p = p + "/";
		}
		return p;
	}
}
