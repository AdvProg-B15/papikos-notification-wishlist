package id.ac.ui.cs.advprog.papikos.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication(scanBasePackages = "id.ac.ui.cs.advprog.papikos.notification")
@EnableAsync
@EnableJpaAuditing
@EnableFeignClients(basePackages = "id.ac.ui.cs.advprog.papikos.notification.client")
public class NotificationApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationApplication.class, args);
    }

}
