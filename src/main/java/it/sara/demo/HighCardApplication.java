package it.sara.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point. Boots the Spring application context and embedded Tomcat.
 */
@SpringBootApplication
public class HighCardApplication {

    public static void main(String[] args) {
        SpringApplication.run(HighCardApplication.class, args);
    }

}
