package com.project.file_pipeline_processor.infrastructure.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declara la topología (exchange/queue/binding) requerida por el consumidor.
 *
 * Nota: Si el broker arranca vacío (sin definitions), sin estos beans Spring AMQP
 * intentará una "passive declare" y fallará con NOT_FOUND.
 */
@Configuration
public class RabbitMqTopologyConfig {

	@Value("${file-service.rabbitmq.exchange:file.exchange}")
	private String exchangeName;

	@Value("${file-service.rabbitmq.queue:file.processing.queue}")
	private String queueName;

	@Value("${file-service.rabbitmq.routing-key:file.uploaded}")
	private String routingKey;

	@Value("${file-service.rabbitmq.queue-ttl-ms:86400000}")
	private long queueTtlMs;

	@Bean
	public TopicExchange fileExchange() {
		return ExchangeBuilder.topicExchange(exchangeName).durable(true).build();
	}

	@Bean
	public Queue fileProcessingQueue() {
		return QueueBuilder
				.durable(queueName)
				.withArgument("x-message-ttl", queueTtlMs)
				.build();
	}

	@Bean
	public Binding fileProcessingBinding(Queue fileProcessingQueue, TopicExchange fileExchange) {
		return BindingBuilder.bind(fileProcessingQueue).to(fileExchange).with(routingKey);
	}
}
