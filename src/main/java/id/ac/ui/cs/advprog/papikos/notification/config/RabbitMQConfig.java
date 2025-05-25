package id.ac.ui.cs.advprog.papikos.notification.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String TOPIC_EXCHANGE_NAME = "rental.topic";
    public static final String ROUTING_KEY_RENTAL_CREATED = "rental.created";
    public static final String KOS_QUEUE_NAME = "kos.rental.created.queue";

    @Bean
    public TopicExchange rentalTopicExchange() {
        return new TopicExchange(TOPIC_EXCHANGE_NAME);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // Not strictly needed if only consuming, but good practice
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }

    @Bean
    Queue kosQueue() {
        // durable: true, exclusive: false, autoDelete: false
        return new Queue(KOS_QUEUE_NAME, true, false, false);
    }

    @Bean
    Binding kosBinding(Queue kosQueue, TopicExchange exchange) {
        return BindingBuilder.bind(kosQueue).to(exchange).with(ROUTING_KEY_RENTAL_CREATED);
    }
}