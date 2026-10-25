package mz.edu.fontanaria.config;

import mz.edu.fontanaria.models.Parametro;
import mz.edu.fontanaria.repositories.ParametroRepository;
import mz.edu.fontanaria.services.ParametroService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Cria os parâmetros do sistema na primeira execução (tarifas por m³ e prazo
 * de vencimento). Só para desenvolvimento: remova esta classe antes de
 * produção.
 */
@Component
@Order(1)
public class ParametrosIniciais implements CommandLineRunner {

    private final ParametroRepository repositorio;

    public ParametrosIniciais(ParametroRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public void run(String... args) {
        guardar("tarifa.domestica", "45", ParametroService.CONHECIDOS.get("tarifa.domestica"));
        guardar("tarifa.comercial", "85", ParametroService.CONHECIDOS.get("tarifa.comercial"));
        guardar("tarifa.institucional", "120", ParametroService.CONHECIDOS.get("tarifa.institucional"));
        guardar("vencimento.dias", "15", ParametroService.CONHECIDOS.get("vencimento.dias"));
    }

    private void guardar(String chave, String valor, String descricao) {
        if (!repositorio.existsByChave(chave)) {
            repositorio.save(new Parametro(chave, valor, descricao));
        }
    }
}
