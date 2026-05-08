package com.aizeronote;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class AiZeroNoteApplication {
    public static void main(String[] args) {
        SpringApplication.run(AiZeroNoteApplication.class, args);
    }
}
