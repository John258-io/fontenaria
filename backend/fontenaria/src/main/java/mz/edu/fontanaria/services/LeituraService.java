package mz.edu.fontanaria.services;

import mz.edu.fontanaria.dto.LeituraListaResposta;
import mz.edu.fontanaria.dto.LeituraPedido;
import mz.edu.fontanaria.dto.LeituraResposta;
import mz.edu.fontanaria.dto.ResumoLeituras;
import mz.edu.fontanaria.errors.NaoEncontrado;
import mz.edu.fontanaria.errors.RegraDeNegocio;
import mz.edu.fontanaria.models.Cliente;
import mz.edu.fontanaria.models.Leitura;
import mz.edu.fontanaria.models.UsuarioSessao;
import mz.edu.fontanaria.repositories.LeituraRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Regras das leituras de contadores: uma leitura por cliente e por mês,
 * consumo calculado em relação à leitura anterior e pesquisa por cliente.
 */
@Service
public class LeituraService {

    private final LeituraRepository repositorio;
    private final ClienteService clientes;
    private final FacturaService facturas;

    public LeituraService(LeituraRepository repositorio,
                          ClienteService clientes,
                          FacturaService facturas) {
        this.repositorio = repositorio;
        this.clientes = clientes;
        this.facturas = facturas;
    }

    /** Lista as leituras, filtradas por pesquisa e/ou período ("AAAA-MM"). */
    @Transactional(readOnly = true)
    public LeituraListaResposta listar(String busca, String periodo) {
        String termo = busca == null ? "" : busca.trim().toLowerCase(Locale.ROOT);
        String mes = normalizarPeriodo(periodo);

        List<LeituraResposta> resposta = repositorio.filtrar(termo, mes).stream()
                .map(leitura -> LeituraResposta.de(leitura, facturas.jaFacturada(leitura.getId())))
                .toList();

        return new LeituraListaResposta(resumo(), resposta);
    }

    /** Contagens para os cartões de indicadores da página. */
    @Transactional(readOnly = true)
    public ResumoLeituras resumo() {
        String periodoActual = periodoActual();
        return new ResumoLeituras(
                repositorio.count(),
                repositorio.countByPeriodo(periodoActual),
                consumoDoPeriodo(periodoActual),
                repositorio.contarClientesSemLeitura(periodoActual));
    }

    @Transactional(readOnly = true)
    public Leitura porId(Long id) {
        return repositorio.findById(id)
                .orElseThrow(() -> new NaoEncontrado("Leitura " + id + " não encontrada."));
    }

    @Transactional
    public Leitura criar(LeituraPedido pedido, UsuarioSessao usuario) {
        Leitura leitura = new Leitura();
        aplicar(leitura, pedido, usuario, null);
        leitura.setDataRegisto(LocalDateTime.now(ClienteService.ZONA));
        return repositorio.save(leitura);
    }

    @Transactional
    public Leitura actualizar(Long id, LeituraPedido pedido, UsuarioSessao usuario) {
        Leitura leitura = porId(id);
        aplicar(leitura, pedido, usuario, id);
        return repositorio.save(leitura);
    }

    @Transactional
    public void remover(Long id) {
        Leitura leitura = porId(id);
        if (facturas.jaFacturada(id)) {
            throw new RegraDeNegocio(
                    "A leitura de " + leitura.getCliente().getNome() + " já originou uma factura; "
                            + "elimine a factura antes de apagar a leitura.");
        }
        repositorio.delete(leitura);
    }

    // --- Consultas usadas pelo painel ---

    /** Soma do consumo (m³) de um período. */
    @Transactional(readOnly = true)
    public BigDecimal consumoDoPeriodo(String periodo) {
        BigDecimal consumo = repositorio.consumoDoPeriodo(periodo);
        return consumo == null ? BigDecimal.ZERO : consumo;
    }

    /** Clientes activos com contador que ainda não leram no período. */
    @Transactional(readOnly = true)
    public long clientesSemLeitura(String periodo) {
        return repositorio.contarClientesSemLeitura(periodo);
    }

    /** Leituras mais recentes (actividade recente do painel). */
    @Transactional(readOnly = true)
    public List<Leitura> ultimas() {
        return repositorio.findTop5ByOrderByDataRegistoDesc();
    }

    /** Leituras de um período, para gerar as facturas do mês. */
    @Transactional(readOnly = true)
    public List<Leitura> doPeriodo(String periodo) {
        return repositorio.findByPeriodoOrderByDataLeituraAsc(periodo);
    }

    // --- Regras de validação ---

    private void aplicar(Leitura leitura, LeituraPedido pedido, UsuarioSessao usuario, Long idActual) {
        if (pedido == null) {
            throw new RegraDeNegocio("Os dados da leitura são obrigatórios.");
        }
        if (pedido.clienteId() == null) {
            throw new RegraDeNegocio("O cliente da leitura é obrigatório.");
        }

        Cliente cliente = clientes.porId(pedido.clienteId());
        LocalDate data = exigirData(pedido.dataLeitura());

        if (data.isAfter(LocalDate.now(ClienteService.ZONA))) {
            throw new RegraDeNegocio("A data da leitura não pode estar no futuro.");
        }

        BigDecimal actual = exigirValor(pedido.leituraActual());
        String periodo = periodoDe(data);
        BigDecimal anterior = leituraAnterior(cliente, data, idActual);
        BigDecimal consumo = actual.subtract(anterior).setScale(2, RoundingMode.HALF_UP);

        if (consumo.signum() < 0) {
            throw new RegraDeNegocio("A leitura actual (" + escala(actual)
                    + " m³) é inferior à leitura anterior (" + escala(anterior) + " m³).");
        }
        exigirPeriodoLivre(cliente, periodo, idActual);

        leitura.setCliente(cliente);
        leitura.setDataLeitura(data);
        leitura.setPeriodo(periodo);
        leitura.setLeituraAnterior(anterior);
        leitura.setLeituraActual(actual);
        leitura.setConsumo(consumo);
        leitura.setObservacao(textoOpcional(pedido.observacao()));
        leitura.setRegistadoPor(usuario == null ? "—" : usuario.getNome());
    }

    /** Último valor lido do contador antes da data indicada (ZERO se não houver). */
    private BigDecimal leituraAnterior(Cliente cliente, LocalDate data, Long idActual) {
        Optional<Leitura> anterior = repositorio
                .findTopByClienteIdAndDataLeituraBeforeOrderByDataLeituraDesc(cliente.getId(), data)
                .filter(l -> !l.getId().equals(idActual));
        return anterior.map(Leitura::getLeituraActual).orElse(BigDecimal.ZERO);
    }

    /** Cada cliente só pode ter uma leitura por mês. */
    private void exigirPeriodoLivre(Cliente cliente, String periodo, Long idActual) {
        boolean ocupado = idActual == null
                ? repositorio.existsByClienteIdAndPeriodo(cliente.getId(), periodo)
                : repositorio.existsByClienteIdAndPeriodoAndIdNot(cliente.getId(), periodo, idActual);
        if (ocupado) {
            throw new RegraDeNegocio(
                    cliente.getNome() + " já tem leitura registada para o período " + periodo + ".");
        }
    }

    private LocalDate exigirData(String valor) {
        String texto = textoOpcional(valor);
        if (texto == null) {
            throw new RegraDeNegocio("A data da leitura é obrigatória (formato AAAA-MM-DD).");
        }
        try {
            return LocalDate.parse(texto);
        } catch (DateTimeParseException e) {
            throw new RegraDeNegocio("Data da leitura inválida: " + texto + " (use AAAA-MM-DD).");
        }
    }

    private BigDecimal exigirValor(BigDecimal valor) {
        if (valor == null) {
            throw new RegraDeNegocio("A leitura do contador é obrigatória.");
        }
        if (valor.signum() < 0) {
            throw new RegraDeNegocio("A leitura do contador não pode ser negativa.");
        }
        return valor.setScale(2, RoundingMode.HALF_UP);
    }

    private String textoOpcional(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return valor.trim();
    }

    // --- Datas e períodos ("AAAA-MM") ---

    /** 2026-10-24 -> "2026-10" */
    public static String periodoDe(LocalDate data) {
        return data.getYear() + "-" + String.format("%02d", data.getMonthValue());
    }

    public static String periodoActual() {
        return periodoDe(LocalDate.now(ClienteService.ZONA));
    }

    /** "2026-2" ou lixo -> "2026-02"; vazio -> "" (sem filtro). */
    private String normalizarPeriodo(String periodo) {
        String texto = textoOpcional(periodo);
        if (texto == null) {
            return "";
        }
        try {
            return periodoDe(YearMonth.parse(texto));
        } catch (DateTimeException e) {
            throw new RegraDeNegocio("Período inválido: " + texto + " (use AAAA-MM).");
        }
    }

    /** "2026-10" -> "2026-10" */
    public static String periodoDe(YearMonth mes) {
        return mes.getYear() + "-" + String.format("%02d", mes.getMonthValue());
    }

    /** 2 m³ -> "2,0" (evita os "2.00" do BigDecimal na mensagem de erro). */
    private String escala(BigDecimal valor) {
        return valor.stripTrailingZeros().toPlainString();
    }
}
