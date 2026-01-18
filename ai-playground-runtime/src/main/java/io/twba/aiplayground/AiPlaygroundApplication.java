package io.twba.aiplayground;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableRetry
public class AiPlaygroundApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiPlaygroundApplication.class, args);
    }

}
