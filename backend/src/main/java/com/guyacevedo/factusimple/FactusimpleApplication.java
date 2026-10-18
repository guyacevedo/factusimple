package com.guyacevedo.factusimple;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class FactusimpleApplication {

    public static void main(String[] args) {
        // Las entidades usan LocalDateTime y las columnas son TIMESTAMP (sin zona):
        // la hora que se guarda es la de la JVM. Se fija aquí para que no dependa
        // de la zona del servidor (los servidores en la nube suelen estar en UTC).
        // El driver de PostgreSQL pasa esta zona a la sesión, así que los
        // DEFAULT now() de la base quedan en la misma hora que LocalDateTime.now().
        TimeZone.setDefault(TimeZone.getTimeZone("America/Bogota"));
        SpringApplication.run(FactusimpleApplication.class, args);
    }

}
