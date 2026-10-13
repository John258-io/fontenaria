package mz.edu.fontanaria.config;

import mz.edu.fontanaria.services.AuthService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Cria o administrador na primeira execução. Só para desenvolvimento:
 * troque a palavra-passe e remova esta classe antes de ir para produção.
 */
@Component
public class AdminInicial implements CommandLineRunner {

    private static final String TELEFONE = "841234567";

    private final AuthService authService;

    public AdminInicial(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public void run(String... args) {
        if (!authService.existeTelefone(TELEFONE)) {
            authService.registar("Ernesto Sitoe", TELEFONE, "admin123", "Administrador");
        }
    }
}
