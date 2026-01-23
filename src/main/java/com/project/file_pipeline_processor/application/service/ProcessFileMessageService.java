package com.project.file_pipeline_processor.application.service;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.file_pipeline_processor.application.dto.FileEventDto;
import com.project.file_pipeline_processor.application.service.crypto.CryptoEnvelope;
import com.project.file_pipeline_processor.application.service.crypto.CryptoService;
import com.project.file_pipeline_processor.application.service.crypto.HashUtil;
import com.project.file_pipeline_processor.application.service.pipeline.ProcessedFileMetadata;

import com.project.file_pipeline_processor.domain.model.FileMessage;
import com.project.file_pipeline_processor.domain.port.in.ProcessFileMessageUseCase;
import com.project.file_pipeline_processor.infrastructure.adapter.out.persistence.FilesDbFileContentReader;
import com.project.file_pipeline_processor.infrastructure.adapter.out.persistence.FileDocumentsJdbcRepository;
import com.project.file_pipeline_processor.infrastructure.adapter.out.storage.MinioObjectStorage;
import com.project.file_pipeline_processor.infrastructure.adapter.out.persistence.mongo.ProcessedFileMetadataDocument;
import com.project.file_pipeline_processor.infrastructure.adapter.out.persistence.mongo.ProcessedFileMetadataMongoRepository;

@Service
public class ProcessFileMessageService implements ProcessFileMessageUseCase {

    private static final Logger log = LoggerFactory.getLogger(ProcessFileMessageService.class);

    private final FilesDbFileContentReader filesDbFileContentReader;
    private final FileDocumentsJdbcRepository fileDocumentsJdbcRepository;
    private final CryptoService cryptoService;
    private final MinioObjectStorage minioObjectStorage;
    private final ObjectMapper objectMapper;
    private final ProcessedFileMetadataMongoRepository metadataMongoRepository;

    // ✅ Para armar la URL pública que guardas en Mongo
    private final String minioPublicUrl;
    private final String minioBucket;

    // ✅ Prefijos (igual que en tu yaml)
    private final String originalPrefix;
    private final String encryptedPrefix;
    private final String metadataPrefix;

    public ProcessFileMessageService(
            FilesDbFileContentReader filesDbFileContentReader,
            FileDocumentsJdbcRepository fileDocumentsJdbcRepository,
            CryptoService cryptoService,
            MinioObjectStorage minioObjectStorage,
            ObjectMapper objectMapper,
            ProcessedFileMetadataMongoRepository metadataMongoRepository,
            @Value("${minio.public-url:${MINIO_PUBLIC_URL:https://minio.local}}") String minioPublicUrl,
            @Value("${minio.bucket:${MINIO_BUCKET:bucket}}") String minioBucket,
            @Value("${minio.keys.original-prefix:original/}") String originalPrefix,
            @Value("${minio.keys.encrypted-prefix:encrypted/}") String encryptedPrefix,
            @Value("${minio.keys.metadata-prefix:metadata/}") String metadataPrefix
    ) {
        this.filesDbFileContentReader = filesDbFileContentReader;
        this.fileDocumentsJdbcRepository = fileDocumentsJdbcRepository;
        this.cryptoService = cryptoService;
        this.minioObjectStorage = minioObjectStorage;
        this.objectMapper = objectMapper;
        this.metadataMongoRepository = metadataMongoRepository;

        this.minioPublicUrl = minioPublicUrl;
        this.minioBucket = minioBucket;

        this.originalPrefix = normalizePrefix(originalPrefix);
        this.encryptedPrefix = normalizePrefix(encryptedPrefix);
        this.metadataPrefix = normalizePrefix(metadataPrefix);
    }

    @Override
    public void process(FileMessage message) {
        FileEventDto event = message.event();
        if (event == null || event.fileUuid() == null || event.fileUuid().isBlank()) {
            log.warn("[Pipeline] Evento inválido. receivedAt={}", message.receivedAt());
            return;
        }

        String fileUuid = event.fileUuid();
        String fileName = (event.fileName() == null || event.fileName().isBlank()) ? (fileUuid + ".bin") : event.fileName();

        log.info("[Pipeline] Iniciando. fileUuid={}, fileName={}, contentType={}",
                fileUuid, fileName, event.contentType());

        try {
            byte[] originalBytes = filesDbFileContentReader.findFileDataByUuid(fileUuid)
                    .orElseThrow(() -> new IllegalArgumentException("Archivo no encontrado en mysql-files. uuid=" + fileUuid));

            CryptoEnvelope envelope = cryptoService.encrypt(originalBytes);
            byte[] decryptedBytes = cryptoService.decrypt(envelope);

            String shaOriginal = HashUtil.sha256Hex(originalBytes);
            String shaDecrypted = HashUtil.sha256Hex(decryptedBytes);
            boolean ok = shaOriginal.equalsIgnoreCase(shaDecrypted);
            if (!ok) {
                throw new IllegalStateException("Validación de descifrado falló. shaOriginal != shaDecrypted");
            }

            // ✅ Actualizar hash en MySQL
            fileDocumentsJdbcRepository.updateHashByUuid(fileUuid, shaOriginal);

            ProcessedFileMetadata metadata = new ProcessedFileMetadata(
                    fileUuid,
                    event.fileId(),
                    fileName,
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

            // ✅ Keys en MinIO: ÚNICAS, pero conservan el nombre real
            String safeName = sanitizeFileName(fileName);
            String originalKey = originalPrefix + fileUuid + "/" + safeName;
            String encryptedKey = encryptedPrefix + fileUuid + ".enc";
            String metaKey = metadataPrefix + fileUuid + ".json";

            // ✅ Subir a MinIO
            String contentType = (event.contentType() == null || event.contentType().isBlank())
                    ? "application/octet-stream"
                    : event.contentType();

            minioObjectStorage.putBytes(originalKey, originalBytes, contentType);
            minioObjectStorage.putBytes(encryptedKey, envelope.encryptedData(), "application/octet-stream");
            minioObjectStorage.putBytes(metaKey, metadataJson, "application/json");

            // ✅ URL pública (apunta al ORIGINAL con nombre real)
            String urlFile = buildMinioUrl(minioPublicUrl, minioBucket, originalKey);

            // ✅ Persistir metadata en Mongo (best-effort)
            try {
                ProcessedFileMetadataDocument doc = new ProcessedFileMetadataDocument(
                        metadata.fileId(),   // _id = uuid
                        urlFile,
                        metadata.fileTableId(),
                        metadata.fileName(),
                        metadata.mimeType(),
                        metadata.originalSize(),
                        metadata.sha256Original(),
                        metadata.sha256Decrypted(),
                        metadata.decryptValidationOk(),
                        metadata.encryptionAlgorithm(),
                        metadata.ivBase64(),
                        metadata.encryptedAesKeyBase64(),
                        metadata.processedAt()
                );

                metadataMongoRepository.save(doc);
                log.info("[Pipeline] Metadata persisted to MongoDB. fileUuid={}, urlFile={}", fileUuid, urlFile);

            } catch (Exception ex) {
                log.warn("[Pipeline] No se pudo persistir metadata en MongoDB: {}", ex.getMessage());
            }

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

    private static String buildMinioUrl(String baseUrl, String bucket, String objectKey) {
        String b = (baseUrl == null) ? "" : baseUrl.trim();
        while (b.endsWith("/")) b = b.substring(0, b.length() - 1);

        String k = (objectKey == null) ? "" : objectKey.trim();
        while (k.startsWith("/")) k = k.substring(1);

        return b + "/" + bucket + "/" + k;
    }

    private static String normalizePrefix(String p) {
        String x = (p == null || p.isBlank()) ? "" : p.trim();
        while (x.startsWith("/")) x = x.substring(1);
        if (!x.isEmpty() && !x.endsWith("/")) x = x + "/";
        return x;
    }

    // ✅ Evita caracteres raros en keys/URLs. (Espacios -> _)
    private static String sanitizeFileName(String name) {
        String n = name.trim().replace("\\", "/");
        int lastSlash = n.lastIndexOf('/');
        if (lastSlash >= 0) n = n.substring(lastSlash + 1);
        n = n.replaceAll("\\s+", "_");
        n = n.replaceAll("[^a-zA-Z0-9._-]", "");
        if (n.isBlank()) n = "file.bin";
        return n;
    }
}
