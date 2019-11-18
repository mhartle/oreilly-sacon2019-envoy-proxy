package com.hartle_klug.mhartle.sacon2019.echo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {
        public static void main(String[] args) throws InterruptedException {
                SpringApplication.run(Application.class, args);
                Thread.currentThread().join();
        }
}
