package com.project.file_pipeline_processor.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.project.file_pipeline_processor.domain.model.FileMessage;
import com.project.file_pipeline_processor.domain.port.in.ProcessFileMessageUseCase;

@Service
public class ProcessFileMessageService implements ProcessFileMessageUseCase {

	private static final Logger log = LoggerFactory.getLogger(ProcessFileMessageService.class);

	@Override
	public void process(FileMessage message) {
		log.info("[UseCase] Procesando mensaje. receivedAt={}, payload={}", message.receivedAt(), message.payload());
	}
}
