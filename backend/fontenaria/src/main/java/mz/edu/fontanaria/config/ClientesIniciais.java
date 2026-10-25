package mz.edu.fontanaria.config;

import mz.edu.fontanaria.models.Cliente;
import mz.edu.fontanaria.repositories.ClienteRepository;
import mz.edu.fontanaria.services.ClienteService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Cria clientes de demonstração na primeira execução, para que a página de
 * clientes e os indicadores do painel não venham vazios. Só para desenvolvimento:
 * remova esta classe antes de ir para produção.
 */
@Component
@Order(2)
public class ClientesIniciais implements CommandLineRunner {

    private final ClienteRepository repositorio;

    public ClientesIniciais(ClienteRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public void run(String... args) {
        if (repositorio.count() > 0) {
            return;
        }

        guardar("Ana Mucavele", "841234571", "Mahelane", "Doméstica", "CT-1042", "Activo", 22);
        guardar("Beatriz Mondlane", "843398214", "Tanga", "Doméstica", "CT-1043", "Activo", 18);
        guardar("Carlos Nhaca", "847720015", "Vila Manhiça", "Comercial", "CT-1044", "Activo", 14);
        guardar("Emília Chongo", "829910043", "Namati", "Doméstica", "CT-1045", "Activo", 11);
        guardar("Fátima Sitoe", "845561230", "Palane", "Doméstica", "CT-1046", "Suspenso", 9);
        guardar("João Cossa", "841234567", "Mahelane", "Doméstica", "CT-1047", "Activo", 7);
        guardar("Nelson Mabjaia", "863340087", "Tanga", "Institucional", "CT-1048", "Activo", 5);
        guardar("Osvaldo Chirindza", "848872231", "Vila Manhiça", "Comercial", "CT-1049", "Activo", 4);
        guardar("Rosa Simango", "824451190", "Namati", "Doméstica", "CT-1050", "Activo", 3);
        guardar("Salomão Macuácua", "875560014", "Mahelane", "Doméstica", "CT-1051", "Activo", 2);
        guardar("Teresa Macuene", "842208876", "Palane", "Doméstica", null, "Activo", 1);
        guardar("Zulmira Nhantumbo", "846619902", "Tanga", "Doméstica", null, "Suspenso", 1);
        guardar("Escola Primária de Namati", "843370055", "Namati", "Institucional", "CT-1052", "Activo", 0);
        guardar("Cantina Mahelane", "829904471", "Mahelane", "Comercial", null, "Activo", 0);
    }

    /** Regista um cliente directamente (com data de registo retroactiva). */
    private void guardar(String nome, String telefone, String bairro, String categoria,
                         String contador, String estado, int mesesAtras) {
        Cliente cliente = new Cliente();
        cliente.setNome(nome);
        cliente.setTelefone(telefone);
        cliente.setBairro(bairro);
        cliente.setCategoria(categoria);
        cliente.setNumeroContador(contador);
        cliente.setEstado(estado);
        cliente.setDataRegisto(LocalDateTime.now(ClienteService.ZONA)
                .minusMonths(mesesAtras).minusDays(mesesAtras == 0 ? 3 : 0));
        repositorio.save(cliente);
    }
}
