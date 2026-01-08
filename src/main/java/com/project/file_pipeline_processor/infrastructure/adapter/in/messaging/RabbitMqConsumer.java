package com.project.file_pipeline_processor.infrastructure.adapter.in.messaging;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.validation.annotation.Validated;
import org.springframework.stereotype.Component;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.file_pipeline_processor.application.dto.FileEventDto;
import com.project.file_pipeline_processor.domain.model.FileMessage;
import com.project.file_pipeline_processor.domain.port.in.ProcessFileMessageUseCase;

@Component
@Validated
public class RabbitMqConsumer {

	private static final Logger log = LoggerFactory.getLogger(RabbitMqConsumer.class);

	private final ProcessFileMessageUseCase processFileMessageUseCase;
	private final ObjectMapper objectMapper;
	private final Validator validator;

	public RabbitMqConsumer(ProcessFileMessageUseCase processFileMessageUseCase, ObjectMapper objectMapper, Validator validator) {
		this.processFileMessageUseCase = processFileMessageUseCase;
		this.objectMapper = objectMapper;
		this.validator = validator;
	}

	@RabbitListener(queues = "${file-service.rabbitmq.queue:file.processing.queue}")
	public void onMessage(Message message) {
		log.info("[RabbitMQ] Mensaje recibido. queue={}, bytes={}, contentType={}",
				message.getMessageProperties().getConsumerQueue(),
				message.getBody() == null ? 0 : message.getBody().length,
				message.getMessageProperties().getContentType());

		try {
			if (message.getBody() == null || message.getBody().length == 0) {
				throw new IllegalArgumentException("Mensaje vacío");
			}
			JsonNode root = objectMapper.readTree(message.getBody());
			FileEventDto event = toEvent(root);
			var violations = validator.validate(event);
			if (!violations.isEmpty()) {
				String first = violations.stream().map(ConstraintViolation::getMessage).findFirst().orElse("Evento inválido");
				throw new IllegalArgumentException("Evento inválido: " + first);
			}
			processFileMessageUseCase.process(new FileMessage(event, Instant.now()));
		} catch (Exception ex) {
			log.error("[RabbitMQ] Error parseando mensaje JSON. messageId={}, error={}",
					message.getMessageProperties().getMessageId(),
					ex.getMessage(),
					ex);
		}
	}

	private FileEventDto toEvent(JsonNode root) {
		String fileUuid = text(root, "fileUuid");
		Long fileId = longValue(root, "fileId");
		String fileName = text(root, "fileName");
		String contentType = text(root, "contentType");

		// Fallback para formato alternativo (si alguien envía {"fileId":"UUID", "mimeType":"..."})
		if ((fileUuid == null || fileUuid.isBlank()) && root.hasNonNull("fileId") && root.get("fileId").isTextual()) {
			fileUuid = root.get("fileId").asText();
		}
		if ((contentType == null || contentType.isBlank()) && root.hasNonNull("mimeType")) {
			contentType = root.get("mimeType").asText();
		}

		return new FileEventDto(fileUuid == null ? "" : fileUuid, fileId, fileName == null ? "" : fileName,
				contentType == null ? "" : contentType);
	}

	private static String text(JsonNode root, String field) {
		JsonNode n = root.get(field);
		return n != null && !n.isNull() ? n.asText() : null;
	}

	private static Long longValue(JsonNode root, String field) {
		JsonNode n = root.get(field);
		if (n == null || n.isNull()) {
			return null;
		}
		if (n.isNumber()) {
			return n.asLong();
		}
		if (n.isTextual()) {
			try {
				return Long.parseLong(n.asText());
			} catch (NumberFormatException ignored) {
				return null;
			}
		}
		return null;
	}
}
