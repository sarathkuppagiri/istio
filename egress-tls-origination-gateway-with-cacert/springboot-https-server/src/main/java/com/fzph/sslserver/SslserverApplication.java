package com.fzph.sslserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SslserverApplication {

    public static void main(String[] args) {
    System.setProperty("https.protocols", "TLSv1 TLSv1.1 TLSv1.2 TLSv1.3");
        SpringApplication.run(SslserverApplication.class, args);
    }

}
