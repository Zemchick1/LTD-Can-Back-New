package com.LTD.ltdWorksAPI;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableJpaRepositories("com.LTD.ltdWorksAPI.repository")
@EnableScheduling
public class LtdWorksApi {

    public static void main(String[] args) {
        SpringApplication.run(LtdWorksApi.class, args);
    }
}
