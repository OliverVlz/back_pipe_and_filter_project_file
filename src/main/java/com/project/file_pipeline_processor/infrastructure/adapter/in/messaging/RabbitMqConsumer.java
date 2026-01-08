package com.project.file_pipeline_processor.infrastructure.adapter.in.messaging;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.project.file_pipeline_processor.domain.model.FileMessage;
import com.project.file_pipeline_processor.domain.port.in.ProcessFileMessageUseCase;

@Component
public class RabbitMqConsumer {

	private static final Logger log = LoggerFactory.getLogger(RabbitMqConsumer.class);

	private final ProcessFileMessageUseCase processFileMessageUseCase;

	public RabbitMqConsumer(ProcessFileMessageUseCase processFileMessageUseCase) {
		this.processFileMessageUseCase = processFileMessageUseCase;
	}

	@RabbitListener(queues = "${file-service.rabbitmq.queue:file.processing.queue}")
	public void onMessage(Message message) {
		String body = message.getBody() == null
				? ""
				: new String(message.getBody(), StandardCharsets.UTF_8);

		log.info("[RabbitMQ] Mensaje recibido. queue={}, bytes={}, contentType={}",
				message.getMessageProperties().getConsumerQueue(),
				message.getBody() == null ? 0 : message.getBody().length,
				message.getMessageProperties().getContentType());

		processFileMessageUseCase.process(new FileMessage(body, Instant.now()));
	}
}
