package mz.edu.fontanaria.services;

import mz.edu.fontanaria.dto.FacturaListaResposta;
import mz.edu.fontanaria.dto.FacturaResposta;
import mz.edu.fontanaria.dto.ResumoFacturas;
import mz.edu.fontanaria.errors.NaoEncontrado;
import mz.edu.fontanaria.errors.RegraDeNegocio;
import mz.edu.fontanaria.models.Cliente;
import mz.edu.fontanaria.models.Factura;
import mz.edu.fontanaria.models.Leitura;
import mz.edu.fontanaria.repositories.FacturaRepository;
import mz.edu.fontanaria.repositories.LeituraRepository;
import mz.edu.fontanaria.repositories.PagamentoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Regras da facturação: a factura nasce de uma leitura (consumo × tarifa da
 * categoria), recebe os pagamentos e muda de estado quando fica paga.
 */
@Service
public class FacturaService {

    public static final String PENDENTE = "Pendente";
    public static final String PAGA = "Paga";

    private final FacturaRepository repositorio;
    private final LeituraRepository leituras;
    private final PagamentoRepository pagamentos;
    private final ParametroService parametros;

    public FacturaService(FacturaRepository repositorio,
                          LeituraRepository leituras,
                          PagamentoRepository pagamentos,
                          ParametroService parametros) {
        this.repositorio = repositorio;
        this.leituras = leituras;
        this.pagamentos = pagamentos;
        this.parametros = parametros;
    }

    /** Lista as facturas, filtradas por pesquisa, estado e/ou período. */
    @Transactional(readOnly = true)
    public FacturaListaResposta listar(String busca, String estado, String periodo) {
        String termo = busca == null ? "" : busca.trim().toLowerCase(Locale.ROOT);
        String situacao = estado == null ? "" : estado.trim();
        String mes = periodo == null ? "" : periodo.trim();

        List<FacturaResposta> resposta = repositorio.filtrar(termo, situacao, mes).stream()
                .map(f -> FacturaResposta.de(f, emAtraso(f)))
                .toList();

        return new FacturaListaResposta(resumo(), resposta);
    }

    /** Contagens para os cartões de indicadores da página. */
    @Transactional(readOnly = true)
    public ResumoFacturas resumo() {
        BigDecimal faturado = repositorio.findAll().stream()
                .map(Factura::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new ResumoFacturas(
                repositorio.count(),
                repositorio.countByEstado(PENDENTE),
                repositorio.countByEstado(PAGA),
                faturado,
                saldoPorReceber());
    }

    @Transactional(readOnly = true)
    public Factura porId(Long id) {
        return repositorio.findById(id)
                .orElseThrow(() -> new NaoEncontrado("Factura " + id + " não encontrada."));
    }

    /** Emite a factura de uma leitura ainda não facturada. */
    @Transactional
    public Factura gerar(Long leituraId) {
        Leitura leitura = leituras.findById(leituraId)
                .orElseThrow(() -> new NaoEncontrado("Leitura " + leituraId + " não encontrada."));

        if (repositorio.existsByLeituraId(leituraId)) {
            throw new RegraDeNegocio("A leitura de " + leitura.getCliente().getNome()
                    + " para o período " + leitura.getPeriodo() + " já foi facturada.");
        }
        if (leitura.getConsumo().signum() <= 0) {
            throw new RegraDeNegocio("A leitura de " + leitura.getCliente().getNome()
                    + " não tem consumo para facturar.");
        }

        Cliente cliente = leitura.getCliente();
        BigDecimal tarifa = parametros.tarifaDe(cliente.getCategoria());
        BigDecimal valor = leitura.getConsumo().multiply(tarifa).setScale(2, RoundingMode.HALF_UP);

        LocalDateTime emissao = LocalDateTime.now(ClienteService.ZONA);

        Factura factura = new Factura();
        factura.setCodigo(proximoCodigo());
        factura.setCliente(cliente);
        factura.setLeitura(leitura);
        factura.setPeriodo(leitura.getPeriodo());
        factura.setConsumo(leitura.getConsumo());
        factura.setTarifa(tarifa);
        factura.setValor(valor);
        factura.setValorPago(BigDecimal.ZERO);
        factura.setDataEmissao(emissao);
        factura.setVencimento(emissao.toLocalDate().plusDays(parametros.diasVencimento()));
        factura.setEstado(PENDENTE);
        return repositorio.save(factura);
    }

    /** Gera todas as facturas em falta de um mês ("AAAA-MM"). */
    @Transactional
    public int gerarMes(String periodo) {
        if (periodo == null || periodo.isBlank()) {
            throw new RegraDeNegocio("O período é obrigatório para gerar as facturas do mês.");
        }
        int criadas = 0;
        for (Leitura leitura : leituras.findByPeriodoOrderByDataLeituraAsc(periodo.trim())) {
            if (repositorio.existsByLeituraId(leitura.getId()) || leitura.getConsumo().signum() <= 0) {
                continue;
            }
            gerar(leitura.getId());
            criadas++;
        }
        return criadas;
    }

    /** Apaga uma factura que ainda não tem nenhum pagamento. */
    @Transactional
    public void remover(Long id) {
        Factura factura = porId(id);
        if (pagamentos.countByFacturaId(id) > 0) {
            throw new RegraDeNegocio("A factura " + factura.getCodigo()
                    + " já tem pagamentos; elimine-os primeiro.");
        }
        repositorio.delete(factura);
    }

    // --- Ligar/desligar pagamentos (chamado pelo PagamentoService) ---

    @Transactional
    public void aplicarPagamento(Factura factura, BigDecimal valor) {
        factura.setValorPago(factura.getValorPago().add(valor));
        actualizarEstado(factura);
        repositorio.save(factura);
    }

    @Transactional
    public void anularPagamento(Factura factura, BigDecimal valor) {
        BigDecimal novo = factura.getValorPago().subtract(valor);
        factura.setValorPago(novo.signum() < 0 ? BigDecimal.ZERO : novo);
        actualizarEstado(factura);
        repositorio.save(factura);
    }

    private void actualizarEstado(Factura factura) {
        boolean paga = factura.getValorPago().compareTo(factura.getValor()) >= 0;
        factura.setEstado(paga ? PAGA : PENDENTE);
    }

    // --- Consultas usadas pelas outras páginas e pelo painel ---

    @Transactional(readOnly = true)
    public boolean jaFacturada(Long leituraId) {
        return repositorio.existsByLeituraId(leituraId);
    }

    /** Soma ainda em dívida nas facturas pendentes. */
    @Transactional(readOnly = true)
    public BigDecimal saldoPorReceber() {
        BigDecimal saldo = repositorio.saldoPorReceber();
        return saldo == null ? BigDecimal.ZERO : saldo;
    }

    /** Facturas pendentes, da mais vencida para a mais recente. */
    @Transactional(readOnly = true)
    public List<Factura> pendentes() {
        return repositorio.findAllByEstadoOrderByVencimentoAsc(PENDENTE);
    }

    /** Facturas pendentes que já passaram do vencimento. */
    @Transactional(readOnly = true)
    public long emAtraso() {
        return repositorio.countByEstadoAndVencimentoBefore(PENDENTE, LocalDate.now(ClienteService.ZONA));
    }

    /** Facturação de um período ("AAAA-MM"). */
    @Transactional(readOnly = true)
    public BigDecimal faturadoDoPeriodo(String periodo) {
        BigDecimal total = repositorio.faturadoDoPeriodo(periodo);
        return total == null ? BigDecimal.ZERO : total;
    }

    /** Facturação dos últimos meses, do mais antigo para o mais recente. */
    @Transactional(readOnly = true)
    public List<TotalMes> receitaMensal(int meses) {
        Map<String, BigDecimal> porPeriodo = new HashMap<>();
        for (Factura factura : repositorio.findAll()) {
            porPeriodo.merge(factura.getPeriodo(), factura.getValor(), BigDecimal::add);
        }

        List<TotalMes> resposta = new ArrayList<>();
        LocalDate mes = LocalDate.now(ClienteService.ZONA).withDayOfMonth(1);
        for (int i = meses - 1; i >= 0; i--) {
            String periodo = LeituraService.periodoDe(mes.minusMonths(i));
            resposta.add(new TotalMes(periodo, porPeriodo.getOrDefault(periodo, BigDecimal.ZERO)));
        }
        return resposta;
    }

    /** Facturas mais recentes (actividade recente do painel). */
    @Transactional(readOnly = true)
    public List<Factura> ultimas() {
        return repositorio.findTop5ByOrderByDataEmissaoDesc();
    }

    /** Pagamentos já registados numa factura (detalhe da factura). */
    @Transactional(readOnly = true)
    public long pagamentosDe(Long facturaId) {
        return pagamentos.countByFacturaId(facturaId);
    }

    public boolean emAtraso(Factura factura) {
        return PENDENTE.equals(factura.getEstado())
                && factura.getVencimento().isBefore(LocalDate.now(ClienteService.ZONA));
    }

    /** Facturação de um mês (usado pelo painel e pelos relatórios). */
    public record TotalMes(String periodo, BigDecimal valor) {
    }

    // --- Regras internas ---

    /** "FAT-2026-0001": sequência anual simples, a partir do número de facturas do ano. */
    private String proximoCodigo() {
        int ano = LocalDate.now(ClienteService.ZONA).getYear();
        String prefixo = "FAT-" + ano + "-";
        long numero = repositorio.countByCodigoStartingWith(prefixo) + 1;
        return prefixo + String.format("%04d", numero);
    }
}
