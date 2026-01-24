package com.project.file_pipeline_processor.infrastructure.adapter.out.persistence.mongo;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "processed_file_metadata")
public class ProcessedFileMetadataDocument {

    @Id
    private String fileId;

    // ✅ NUEVO: URL pública del archivo en MinIO
    private String urlFile;

    private Long fileTableId;
    private String fileName;
    private String mimeType;
    private long originalSize;
    private String sha256Original;
    private String sha256Decrypted;
    private boolean decryptValidationOk;
    private String encryptionAlgorithm;
    private String ivBase64;
    private String encryptedAesKeyBase64;
    private Instant processedAt;

    public ProcessedFileMetadataDocument() {}

    // ✅ Constructor ORIGINAL (lo dejamos para no romper llamadas existentes)
    public ProcessedFileMetadataDocument(
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
        this.fileId = fileId;
        this.fileTableId = fileTableId;
        this.fileName = fileName;
        this.mimeType = mimeType;
        this.originalSize = originalSize;
        this.sha256Original = sha256Original;
        this.sha256Decrypted = sha256Decrypted;
        this.decryptValidationOk = decryptValidationOk;
        this.encryptionAlgorithm = encryptionAlgorithm;
        this.ivBase64 = ivBase64;
        this.encryptedAesKeyBase64 = encryptedAesKeyBase64;
        this.processedAt = processedAt;
    }

    // ✅ Constructor NUEVO (incluye urlFile)
    public ProcessedFileMetadataDocument(
            String fileId,
            String urlFile,
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
        this.fileId = fileId;
        this.urlFile = urlFile;
        this.fileTableId = fileTableId;
        this.fileName = fileName;
        this.mimeType = mimeType;
        this.originalSize = originalSize;
        this.sha256Original = sha256Original;
        this.sha256Decrypted = sha256Decrypted;
        this.decryptValidationOk = decryptValidationOk;
        this.encryptionAlgorithm = encryptionAlgorithm;
        this.ivBase64 = ivBase64;
        this.encryptedAesKeyBase64 = encryptedAesKeyBase64;
        this.processedAt = processedAt;
    }

    // ✅ Alias opcional (por si en logs usan saved.getId())
    public String getId() { return fileId; }
    public void setId(String id) { this.fileId = id; }

    public String getFileId() { return fileId; }
    public void setFileId(String fileId) { this.fileId = fileId; }

    public String getUrlFile() { return urlFile; }
    public void setUrlFile(String urlFile) { this.urlFile = urlFile; }

    public Long getFileTableId() { return fileTableId; }
    public void setFileTableId(Long fileTableId) { this.fileTableId = fileTableId; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }

    public long getOriginalSize() { return originalSize; }
    public void setOriginalSize(long originalSize) { this.originalSize = originalSize; }

    public String getSha256Original() { return sha256Original; }
    public void setSha256Original(String sha256Original) { this.sha256Original = sha256Original; }

    public String getSha256Decrypted() { return sha256Decrypted; }
    public void setSha256Decrypted(String sha256Decrypted) { this.sha256Decrypted = sha256Decrypted; }

    public boolean isDecryptValidationOk() { return decryptValidationOk; }
    public void setDecryptValidationOk(boolean decryptValidationOk) { this.decryptValidationOk = decryptValidationOk; }

    public String getEncryptionAlgorithm() { return encryptionAlgorithm; }
    public void setEncryptionAlgorithm(String encryptionAlgorithm) { this.encryptionAlgorithm = encryptionAlgorithm; }

    public String getIvBase64() { return ivBase64; }
    public void setIvBase64(String ivBase64) { this.ivBase64 = ivBase64; }

    public String getEncryptedAesKeyBase64() { return encryptedAesKeyBase64; }
    public void setEncryptedAesKeyBase64(String encryptedAesKeyBase64) { this.encryptedAesKeyBase64 = encryptedAesKeyBase64; }

    public Instant getProcessedAt() { return processedAt; }
    public void setProcessedAt(Instant processedAt) { this.processedAt = processedAt; }
}
