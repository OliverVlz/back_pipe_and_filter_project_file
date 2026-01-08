package com.project.file_pipeline_processor.domain.model;

import java.time.Instant;

import com.project.file_pipeline_processor.application.dto.FileEventDto;

public record FileMessage(
		FileEventDto event,
		Instant receivedAt
) {
}
