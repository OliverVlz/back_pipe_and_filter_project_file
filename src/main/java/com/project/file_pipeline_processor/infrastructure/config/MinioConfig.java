package com.project.file_pipeline_processor.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.minio.MinioClient;

@Configuration
public class MinioConfig {

	@Bean
	public MinioClient minioClient(
			@Value("${minio.url:http://localhost:9000}") String url,
			@Value("${minio.access-key:minioadmin}") String accessKey,
			@Value("${minio.secret-key:minioadmin}") String secretKey
	) {
		return MinioClient.builder()
				.endpoint(url)
				.credentials(accessKey, secretKey)
				.build();
	}
}
