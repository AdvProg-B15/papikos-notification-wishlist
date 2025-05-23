package id.ac.ui.cs.advprog.papikos.notification.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NotificationRabbitMQConfig {
    public static final String TOPIC_EXCHANGE_NAME = "rental.topic";
    public static final String ROUTING_KEY_RENTAL_CREATED = "rental.created";
    public static final String NOTIFICATION_QUEUE_NAME = "notification.rental.created.queue";

    @Bean TopicExchange rentalTopicExchange() { return new TopicExchange(TOPIC_EXCHANGE_NAME); }
    @Bean MessageConverter jsonMessageConverter() { return new Jackson2JsonMessageConverter(); }
    @Bean RabbitTemplate rabbitTemplate(ConnectionFactory cf) { /* ... as above ... */
        final RabbitTemplate rabbitTemplate = new RabbitTemplate(cf);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }
    @Bean Queue notificationQueue() { return new Queue(NOTIFICATION_QUEUE_NAME, true); }
    @Bean Binding notificationBinding(Queue notificationQueue, TopicExchange exchange) {
        return BindingBuilder.bind(notificationQueue).to(exchange).with(ROUTING_KEY_RENTAL_CREATED);
    }
}