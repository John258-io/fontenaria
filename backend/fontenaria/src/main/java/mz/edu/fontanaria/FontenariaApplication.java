package mz.edu.fontanaria;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * API REST da Fontanária Manhiça.
 *
 * Este backend serve apenas dados (JSON) para o frontend estático em /frontend.
 * A autenticação é feita com tokens JWT (ver security/ e config/SecurityConfig).
 */
@SpringBootApplication
public class FontenariaApplication {

    public static void main(String[] args) {
        SpringApplication.run(FontenariaApplication.class, args);
    }
}
