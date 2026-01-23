package com.project.file_pipeline_processor.application.service.pipeline;

import java.text.Normalizer;

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

	// ✅ ORIGINAL: usa nombre real del archivo
	public String originalKey(String fileName) {
		String safe = safeFileName(fileName, "file.bin");
		return originalPrefix + safe;
	}

	// ✅ ENCRYPTED: usa el mismo nombre + .enc
	public String encryptedKey(String fileName) {
		String safe = safeFileName(fileName, "file.bin");
		return encryptedPrefix + safe + ".enc";
	}

	// ✅ METADATA: usa el mismo nombre + .json
	public String metadataKey(String fileName) {
		String safe = safeFileName(fileName, "file.bin");
		return metadataPrefix + safe + ".json";
	}

	private static String safeFileName(String raw, String fallback) {
		if (raw == null || raw.isBlank()) return fallback;

		// Quitar path si llega "C:\...\report.pdf" o "/tmp/report.pdf"
		String name = raw.replace("\\", "/");
		int idx = name.lastIndexOf('/');
		if (idx >= 0) name = name.substring(idx + 1);

		// Normalizar: quitar tildes y caracteres raros
		name = Normalizer.normalize(name, Normalizer.Form.NFD)
				.replaceAll("\\p{M}", "");

		// Reemplazar caracteres peligrosos
		name = name.replaceAll("[^a-zA-Z0-9._-]", "_");

		// Evitar nombres vacíos
		if (name.isBlank()) return fallback;

		return name;
	}

	private static String normalizePrefix(String raw, String fallback) {
		String p = (raw == null || raw.isBlank()) ? fallback : raw.trim();
		while (p.startsWith("/")) {
			p = p.substring(1);
		}
		if (!p.endsWith("/")) {
			p = p + "/";
		}
		return p;
	}
}
