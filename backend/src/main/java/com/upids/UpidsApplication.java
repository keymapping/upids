package com.upids;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * UPIDS - Urban Pipeline Inspection and Detection System
 */
@SpringBootApplication
@EnableAsync
@MapperScan("com.upids.mapper")
public class UpidsApplication {

    public static void main(String[] args) {
        SpringApplication.run(UpidsApplication.class, args);
    }
}