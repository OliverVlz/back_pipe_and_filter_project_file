package com.project.file_pipeline_processor.domain.port.in;

import com.project.file_pipeline_processor.domain.model.FileMessage;

public interface ProcessFileMessageUseCase {
	void process(FileMessage message);
}
