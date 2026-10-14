package mz.edu.fontanaria.config;

import mz.edu.fontanaria.services.AuthService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Cria o utilizador padrão da aplicação (operador) na primeira execução.
 * Só para desenvolvimento: troque a palavra-passe e remova esta classe
 * antes de ir para produção.
 */
@Component
public class UtilizadorPadraoInicial implements CommandLineRunner {

    private static final String TELEFONE = "841234568";
    private static final String SENHA = "user123";

    private final AuthService authService;

    public UtilizadorPadraoInicial(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public void run(String... args) {
        if (!authService.existeTelefone(TELEFONE)) {
            authService.registar("Utilizador Padrão", TELEFONE, SENHA, "Operador");
        }
    }
}
