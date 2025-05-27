package id.ac.ui.cs.advprog.papikos.notification;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
class NotificationApplicationTests {
    @MockBean
    private RabbitTemplate rabbitTemplate;

    @MockBean
    private ConnectionFactory rabbitConnectionFactory;
    @Test
    void contextLoads() {
    }

}
