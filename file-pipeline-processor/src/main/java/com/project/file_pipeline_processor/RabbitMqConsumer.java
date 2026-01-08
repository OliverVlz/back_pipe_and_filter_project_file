package com.project.file_pipeline_processor;

import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class RabbitMqConsumer {

	private static final Logger log = LoggerFactory.getLogger(RabbitMqConsumer.class);

	@RabbitListener(queues = "${file-service.rabbitmq.queue:file.processing.queue}")
	public void onMessage(Message message) {
		String body = message.getBody() == null
				? ""
				: new String(message.getBody(), StandardCharsets.UTF_8);

		log.info("[RabbitMQ] Mensaje recibido. queue={}, bytes={}, contentType={}, body={}",
				message.getMessageProperties().getConsumerQueue(),
				message.getBody() == null ? 0 : message.getBody().length,
				message.getMessageProperties().getContentType(),
				body);
	}
}
