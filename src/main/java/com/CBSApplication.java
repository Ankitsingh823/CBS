package com;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@SpringBootApplication
@EnableScheduling
@EnableTransactionManagement

@EnableJpaRepositories("com.JPA")
@EnableRedisRepositories("com.Redis")
@EntityScan(basePackages = "com.model")

public class CBSApplication {

    public static void main(String[] args) { SpringApplication.run(CBSApplication.class, args);}
}
