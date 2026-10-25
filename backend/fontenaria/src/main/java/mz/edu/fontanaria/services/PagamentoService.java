package mz.edu.fontanaria.services;

import mz.edu.fontanaria.dto.FacturaResposta;
import mz.edu.fontanaria.dto.MensagemContagem;
import mz.edu.fontanaria.dto.PagamentoListaResposta;
import mz.edu.fontanaria.dto.PagamentoPedido;
import mz.edu.fontanaria.dto.PagamentoResposta;
import mz.edu.fontanaria.dto.ResumoPagamentos;
import mz.edu.fontanaria.errors.NaoEncontrado;
import mz.edu.fontanaria.errors.RegraDeNegocio;
import mz.edu.fontanaria.models.Factura;
import mz.edu.fontanaria.models.Pagamento;
import mz.edu.fontanaria.models.UsuarioSessao;
import mz.edu.fontanaria.repositories.PagamentoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Regras dos pagamentos: registar recebimentos (totais ou parciais) contra uma
 * factura pendente e anular recibos, recalculando o estado da factura.
 */
@Service
public class PagamentoService {

    public static final Set<String> METODOS = Set.of(
            "Numerário", "M-Pesa", "e-Mola", "Transferência", "Multicaixa");

    private final PagamentoRepository repositorio;
    private final FacturaService facturas;

    public PagamentoService(PagamentoRepository repositorio, FacturaService facturas) {
        this.repositorio = repositorio;
        this.facturas = facturas;
    }

    /** Lista os pagamentos e as facturas pendentes (para o formulário). */
    @Transactional(readOnly = true)
    public PagamentoListaResposta listar(String busca, String metodo) {
        String termo = busca == null ? "" : busca.trim().toLowerCase(Locale.ROOT);
        String forma = metodo == null ? "" : metodo.trim();

        List<PagamentoResposta> resposta = repositorio.filtrar(termo, forma).stream()
                .map(PagamentoResposta::de)
                .toList();

        List<FacturaResposta> pendentes = facturas.pendentes().stream()
                .map(f -> FacturaResposta.de(f, facturas.emAtraso(f)))
                .toList();

        return new PagamentoListaResposta(resumo(), resposta, pendentes);
    }

    /** Soma do dinheiro recebido neste mês. */
    @Transactional(readOnly = true)
    public BigDecimal recebidoEsteMes() {
        LocalDateTime inicio = LocalDateTime.now(ClienteService.ZONA)
                .withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        return nullSafe(repositorio.totalEntre(inicio, LocalDateTime.now(ClienteService.ZONA).plusNanos(1)));
    }

    /** Contagens para os cartões de indicadores da página. */
    @Transactional(readOnly = true)
    public ResumoPagamentos resumo() {
        LocalDateTime inicioDoMes = LocalDateTime.now(ClienteService.ZONA)
                .withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime limite = LocalDateTime.now(ClienteService.ZONA).plusNanos(1);

        BigDecimal recebidoEsteMes = repositorio.totalEntre(inicioDoMes, limite);
        BigDecimal recebidoTotal = repositorio.totalGeral();

        return new ResumoPagamentos(
                repositorio.count(),
                repositorio.countByDataPagamentoAfter(inicioDoMes),
                nullSafe(recebidoEsteMes),
                nullSafe(recebidoTotal),
                facturas.saldoPorReceber());
    }

    /** Regista um pagamento contra uma factura pendente. */
    @Transactional
    public Pagamento registar(PagamentoPedido pedido, UsuarioSessao usuario) {
        if (pedido == null || pedido.facturaId() == null) {
            throw new RegraDeNegocio("A factura a pagar é obrigatória.");
        }

        Factura factura = facturas.porId(pedido.facturaId());

        if (!FacturaService.PENDENTE.equals(factura.getEstado())) {
            throw new RegraDeNegocio("A factura " + factura.getCodigo() + " já está paga.");
        }

        BigDecimal valor = exigirValor(pedido.valor());
        BigDecimal saldo = factura.getValor().subtract(factura.getValorPago());

        if (valor.compareTo(saldo) > 0) {
            throw new RegraDeNegocio("O valor excede o saldo em dívida da factura "
                    + factura.getCodigo() + " (" + saldo.toPlainString() + " MZN).");
        }

        Pagamento pagamento = new Pagamento();
        pagamento.setCodigo(proximoCodigo());
        pagamento.setFactura(factura);
        pagamento.setValor(valor);
        pagamento.setDataPagamento(LocalDateTime.now(ClienteService.ZONA));
        pagamento.setMetodo(escolherMetodo(pedido.metodo()));
        pagamento.setRecebidoPor(usuario == null ? "—" : usuario.getNome());
        pagamento.setObservacao(textoOpcional(pedido.observacao()));

        Pagamento guardado = repositorio.save(pagamento);
        facturas.aplicarPagamento(factura, valor);
        return guardado;
    }

    /** Anula um recibo (estorno) e devolve o saldo à factura. */
    @Transactional
    public void remover(Long id) {
        Pagamento pagamento = porId(id);
        facturas.anularPagamento(pagamento.getFactura(), pagamento.getValor());
        repositorio.delete(pagamento);
    }

    @Transactional(readOnly = true)
    public Pagamento porId(Long id) {
        return repositorio.findById(id)
                .orElseThrow(() -> new NaoEncontrado("Pagamento " + id + " não encontrado."));
    }

    /** Pagamentos mais recentes (actividade recente do painel). */
    @Transactional(readOnly = true)
    public List<Pagamento> ultimas() {
        return repositorio.findTop5ByOrderByDataPagamentoDesc();
    }

    /** Pagamentos já registados numa factura (detalhe da factura). */
    @Transactional(readOnly = true)
    public List<PagamentoResposta> daFactura(Long facturaId) {
        return repositorio.findByFacturaIdOrderByDataPagamentoDesc(facturaId).stream()
                .map(PagamentoResposta::de)
                .toList();
    }

    // --- Regras de validação ---

    private BigDecimal exigirValor(BigDecimal valor) {
        if (valor == null || valor.signum() <= 0) {
            throw new RegraDeNegocio("O valor do pagamento é obrigatório e deve ser positivo.");
        }
        return valor.setScale(2, RoundingMode.HALF_UP);
    }

    private String escolherMetodo(String valor) {
        if (valor == null || valor.isBlank()) {
            return "Numerário";
        }
        String texto = valor.trim();
        if (!METODOS.contains(texto)) {
            throw new RegraDeNegocio("Método de pagamento inválido: " + texto + ".");
        }
        return texto;
    }

    private String textoOpcional(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return valor.trim();
    }

    private BigDecimal nullSafe(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }

    /** "REC-2026-0001": sequência anual simples. */
    private String proximoCodigo() {
        int ano = LocalDateTime.now(ClienteService.ZONA).getYear();
        String prefixo = "REC-" + ano + "-";
        long numero = repositorio.countByCodigoStartingWith(prefixo) + 1;
        return prefixo + String.format("%04d", numero);
    }
}
