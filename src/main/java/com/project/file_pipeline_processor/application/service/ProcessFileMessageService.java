package com.project.file_pipeline_processor.application.service;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.file_pipeline_processor.application.dto.FileEventDto;
import com.project.file_pipeline_processor.application.service.crypto.CryptoEnvelope;
import com.project.file_pipeline_processor.application.service.crypto.CryptoService;
import com.project.file_pipeline_processor.application.service.pipeline.FileObjectKeyFactory;
import com.project.file_pipeline_processor.application.service.pipeline.ProcessedFileMetadata;

import com.project.file_pipeline_processor.domain.model.FileMessage;
import com.project.file_pipeline_processor.domain.port.in.ProcessFileMessageUseCase;
import com.project.file_pipeline_processor.infrastructure.adapter.out.persistence.FilesDbFileContentReader;
import com.project.file_pipeline_processor.infrastructure.adapter.out.storage.MinioObjectStorage;

@Service
public class ProcessFileMessageService implements ProcessFileMessageUseCase {

	private static final Logger log = LoggerFactory.getLogger(ProcessFileMessageService.class);

	private final FilesDbFileContentReader filesDbFileContentReader;
	private final CryptoService cryptoService;
	private final MinioObjectStorage minioObjectStorage;
	private final ObjectMapper objectMapper;
	private final FileObjectKeyFactory keyFactory;

	public ProcessFileMessageService(
			FilesDbFileContentReader filesDbFileContentReader,
			CryptoService cryptoService,
			MinioObjectStorage minioObjectStorage,
			ObjectMapper objectMapper,
			FileObjectKeyFactory keyFactory
	) {
		this.filesDbFileContentReader = filesDbFileContentReader;
		this.cryptoService = cryptoService;
		this.minioObjectStorage = minioObjectStorage;
		this.objectMapper = objectMapper;
		this.keyFactory = keyFactory;
	}

	@Override
	public void process(FileMessage message) {
		FileEventDto event = message.event();
		if (event == null || event.fileUuid() == null || event.fileUuid().isBlank()) {
			log.warn("[Pipeline] Evento inválido. receivedAt={}", message.receivedAt());
			return;
		}

		String fileUuid = event.fileUuid();
		log.info("[Pipeline] Iniciando. fileUuid={}, fileName={}, contentType={}", fileUuid, event.fileName(), event.contentType());

		try {
			byte[] originalBytes = filesDbFileContentReader.findFileDataByUuid(fileUuid)
					.orElseThrow(() -> new IllegalArgumentException("Archivo no encontrado en mysql-files. uuid=" + fileUuid));

			CryptoEnvelope envelope = cryptoService.encrypt(originalBytes);
			byte[] decryptedBytes = cryptoService.decrypt(envelope);

			String shaOriginal = cryptoService.sha256Hex(originalBytes);
			String shaDecrypted = cryptoService.sha256Hex(decryptedBytes);
			boolean ok = shaOriginal.equalsIgnoreCase(shaDecrypted);
			if (!ok) {
				throw new IllegalStateException("Validación de descifrado falló. shaOriginal != shaDecrypted");
			}

			ProcessedFileMetadata metadata = new ProcessedFileMetadata(
					fileUuid,
					event.fileId(),
					event.fileName(),
					event.contentType(),
					originalBytes.length,
					shaOriginal,
					shaDecrypted,
					ok,
					envelope.algorithm(),
					java.util.Base64.getEncoder().encodeToString(envelope.iv()),
					java.util.Base64.getEncoder().encodeToString(envelope.encryptedAesKey()),
					Instant.now()
			);
			byte[] metadataJson = toJsonBytes(metadata);

			minioObjectStorage.putBytes(keyFactory.originalKey(fileUuid), originalBytes,
					event.contentType() == null || event.contentType().isBlank() ? "application/octet-stream" : event.contentType());
			minioObjectStorage.putBytes(keyFactory.encryptedKey(fileUuid), envelope.encryptedData(), "application/octet-stream");
			minioObjectStorage.putBytes(keyFactory.metadataKey(fileUuid), metadataJson, "application/json");

			log.info("[Pipeline] OK. fileUuid={}, bytes={}", fileUuid, originalBytes.length);
		} catch (Exception ex) {
			log.error("[Pipeline] ERROR procesando fileUuid={}. error={}", fileUuid, ex.getMessage(), ex);
		}
	}

	private byte[] toJsonBytes(Object obj) {
		try {
			return objectMapper.writeValueAsBytes(obj);
		} catch (JsonProcessingException ex) {
			throw new IllegalStateException("No se pudo serializar metadata a JSON", ex);
		}
	}
}
