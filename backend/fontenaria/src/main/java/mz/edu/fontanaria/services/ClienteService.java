package mz.edu.fontanaria.services;

import mz.edu.fontanaria.dto.ClienteListaResposta;
import mz.edu.fontanaria.dto.ClientePedido;
import mz.edu.fontanaria.dto.ClienteResposta;
import mz.edu.fontanaria.dto.ResumoClientes;
import mz.edu.fontanaria.errors.NaoEncontrado;
import mz.edu.fontanaria.errors.RegraDeNegocio;
import mz.edu.fontanaria.models.Cliente;
import mz.edu.fontanaria.repositories.ClienteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Regras dos clientes: validação dos dados, pesquisa e CRUD.
 */
@Service
public class ClienteService {

    /** Fuso horário da operação (usado também pelo painel). */
    public static final ZoneId ZONA = ZoneId.of("Africa/Maputo");

    public static final Set<String> CATEGORIAS = Set.of("Doméstica", "Comercial", "Institucional");
    public static final Set<String> ESTADOS = Set.of("Activo", "Suspenso");

    private final ClienteRepository repositorio;
    private final AuthService authService;

    public ClienteService(ClienteRepository repositorio, AuthService authService) {
        this.repositorio = repositorio;
        this.authService = authService;
    }

    /** Lista todos os clientes, ou só os que correspondem à pesquisa. */
    @Transactional(readOnly = true)
    public ClienteListaResposta listar(String busca) {
        String termo = busca == null ? "" : busca.trim().toLowerCase(Locale.ROOT);

        List<Cliente> encontrados = termo.isEmpty()
                ? repositorio.findAllByOrderByNomeAsc()
                : repositorio.procurar(termo);

        return new ClienteListaResposta(resumo(),
                encontrados.stream().map(ClienteResposta::de).toList());
    }

    /** Contagens para os cartões de indicadores. */
    @Transactional(readOnly = true)
    public ResumoClientes resumo() {
        long total = repositorio.count();
        long activos = repositorio.countByEstado("Activo");
        long suspensos = repositorio.countByEstado("Suspenso");
        long incompletos = repositorio.countByNumeroContadorIsNull();
        long novosEsteMes = repositorio.countByDataRegistoAfter(inicioDoMes());
        return new ResumoClientes(total, activos, suspensos, incompletos, novosEsteMes);
    }

    @Transactional(readOnly = true)
    public Cliente porId(Long id) {
        return repositorio.findById(id)
                .orElseThrow(() -> new NaoEncontrado("Cliente " + id + " não encontrado."));
    }

    @Transactional
    public Cliente criar(ClientePedido pedido) {
        Cliente cliente = new Cliente();
        aplicar(cliente, pedido, null);

        cliente.setDataRegisto(LocalDateTime.now(ZONA));
        return repositorio.save(cliente);
    }

    @Transactional
    public Cliente actualizar(Long id, ClientePedido pedido) {
        Cliente cliente = porId(id);
        aplicar(cliente, pedido, id);
        return repositorio.save(cliente);
    }

    @Transactional
    public void remover(Long id) {
        if (!repositorio.existsById(id)) {
            throw new NaoEncontrado("Cliente " + id + " não encontrado.");
        }
        repositorio.deleteById(id);
    }

    // --- Regras de validação ---

    private void aplicar(Cliente cliente, ClientePedido pedido, Long idActual) {
        if (pedido == null) {
            throw new RegraDeNegocio("Os dados do cliente são obrigatórios.");
        }

        cliente.setNome(exigirTexto(pedido.nome(), "O nome do cliente é obrigatório."));
        cliente.setTelefone(normalizarTelefone(pedido.telefone()));
        cliente.setBairro(exigirTexto(pedido.bairro(), "O bairro é obrigatório."));
        cliente.setCategoria(escolher(pedido.categoria(), CATEGORIAS, "Doméstica", "categoria"));
        cliente.setEstado(escolher(pedido.estado(), ESTADOS, "Activo", "estado"));
        cliente.setNumeroContador(textoOpcional(pedido.numeroContador()));
        validarContadorUnico(cliente.getNumeroContador(), idActual);
    }

    /** Reutiliza a normalização do login ("+258 84 123 4567" -> "841234567"). */
    private String normalizarTelefone(String valor) {
        String digitos = authService.normalizarTelefone(valor);
        if (!digitos.matches("8\\d{8}")) {
            throw new RegraDeNegocio(
                    "O telefone deve ter 9 dígitos e começar por 8 (ex.: 84 123 4567).");
        }
        return digitos;
    }

    private void validarContadorUnico(String numeroContador, Long idActual) {
        if (numeroContador == null) {
            return;
        }
        Optional<Cliente> existente = repositorio.findByNumeroContador(numeroContador);
        if (existente.isPresent() && !existente.get().getId().equals(idActual)) {
            throw new RegraDeNegocio("Já existe um cliente com o contador " + numeroContador + ".");
        }
    }

    private String exigirTexto(String valor, String mensagem) {
        String texto = textoOpcional(valor);
        if (texto == null) {
            throw new RegraDeNegocio(mensagem);
        }
        return texto;
    }

    private String textoOpcional(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return valor.trim();
    }

    private String escolher(String valor, Set<String> opcoes, String padrao, String rotulo) {
        String texto = textoOpcional(valor);
        if (texto == null) {
            return padrao;
        }
        if (!opcoes.contains(texto)) {
            throw new RegraDeNegocio("Opção de " + rotulo + " inválida: " + texto + ".");
        }
        return texto;
    }

    private LocalDateTime inicioDoMes() {
        return LocalDateTime.now(ZONA).withDayOfMonth(1).withHour(0)
                .withMinute(0).withSecond(0).withNano(0);
    }
}
