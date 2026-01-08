package com.project.file_pipeline_processor.infrastructure.adapter.out.storage;

import java.io.ByteArrayInputStream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;

@Service
public class MinioObjectStorage {

	private final MinioClient minioClient;
	private final String bucket;

	public MinioObjectStorage(MinioClient minioClient, @Value("${minio.bucket:file-pipeline}") String bucket) {
		this.minioClient = minioClient;
		this.bucket = bucket;
	}

	public void putBytes(String objectKey, byte[] data, String contentType) {
		try {
			ensureBucketExists();
			minioClient.putObject(
					PutObjectArgs.builder()
							.bucket(bucket)
							.object(objectKey)
							.stream(new ByteArrayInputStream(data), data.length, -1)
							.contentType(contentType)
							.build()
			);
		} catch (Exception ex) {
			throw new IllegalStateException("Error uploading to MinIO: bucket=" + bucket + ", key=" + objectKey, ex);
		}
	}

	private void ensureBucketExists() throws Exception {
		boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
		if (!exists) {
			minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
		}
	}
}
