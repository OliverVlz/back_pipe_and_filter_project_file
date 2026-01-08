package com.project.file_pipeline_processor.domain.model;

import java.time.Instant;

public record FileMessage(
		String payload,
		Instant receivedAt
) {
}
