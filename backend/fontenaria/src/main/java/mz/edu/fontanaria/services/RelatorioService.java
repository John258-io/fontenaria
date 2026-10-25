package mz.edu.fontanaria.services;

import mz.edu.fontanaria.dto.RelatorioResposta;
import mz.edu.fontanaria.dto.RelatorioResposta.Categoria;
import mz.edu.fontanaria.dto.RelatorioResposta.Indicador;
import mz.edu.fontanaria.dto.RelatorioResposta.Mes;
import mz.edu.fontanaria.errors.RegraDeNegocio;
import mz.edu.fontanaria.models.Cliente;
import mz.edu.fontanaria.models.Factura;
import mz.edu.fontanaria.models.Leitura;
import mz.edu.fontanaria.models.Pagamento;
import mz.edu.fontanaria.repositories.ClienteRepository;
import mz.edu.fontanaria.repositories.FacturaRepository;
import mz.edu.fontanaria.repositories.LeituraRepository;
import mz.edu.fontanaria.repositories.PagamentoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Relatórios: consumo, facturação e recebimentos de um período, por categoria
 * e por mês. O período por omissão são os últimos 6 meses.
 */
@Service
public class RelatorioService {

    private static final String[] ABREVIATURAS = {
            "Jan", "Fev", "Mar", "Abr", "Mai", "Jun",
            "Jul", "Ago", "Set", "Out", "Nov", "Dez"
    };

    private final LeituraRepository leituras;
    private final FacturaRepository facturas;
    private final PagamentoRepository pagamentos;
    private final ClienteRepository clientes;

    public RelatorioService(LeituraRepository leituras,
                            FacturaRepository facturas,
                            PagamentoRepository pagamentos,
                            ClienteRepository clientes) {
        this.leituras = leituras;
        this.facturas = facturas;
        this.pagamentos = pagamentos;
        this.clientes = clientes;
    }

    /** Gera o relatório do período pedido (datas em AAAA-MM-DD). */
    @Transactional(readOnly = true)
    public RelatorioResposta gerar(String inicioPedido, String fimPedido) {
        LocalDate hoje = LocalDate.now(ClienteService.ZONA);
        LocalDate inicio = inicioPedido == null || inicioPedido.isBlank()
                ? hoje.withDayOfMonth(1).minusMonths(5)
                : exigirData(inicioPedido, "Data inicial");
        LocalDate fim = fimPedido == null || fimPedido.isBlank()
                ? hoje
                : exigirData(fimPedido, "Data final");

        if (fim.isBefore(inicio)) {
            throw new RegraDeNegocio("A data final é anterior à data inicial.");
        }

        List<Leitura> leiturasPeriodo = leituras
                .findAllByDataLeituraBetweenOrderByDataLeituraAsc(inicio, fim);
        List<Factura> facturasPeriodo = facturasDe(facturas.findAll(), inicio, fim);
        List<Pagamento> pagamentosPeriodo = pagamentosDe(pagamentos.findAll(), inicio, fim);

        BigDecimal consumo = soma(leiturasPeriodo.stream().map(Leitura::getConsumo).toList());
        BigDecimal faturado = soma(facturasPeriodo.stream().map(Factura::getValor).toList());
        BigDecimal recebido = soma(pagamentosPeriodo.stream().map(Pagamento::getValor).toList());
        BigDecimal porReceber = facturas.saldoPorReceber();
        long pendentes = facturas.countByEstado(FacturaService.PENDENTE);

        List<Indicador> indicadores = List.of(
                new Indicador("Consumo medido", numero(consumo, 1) + " m³",
                        leiturasPeriodo.size() + " leitura(s) no período", "tone-teal", "i-drop"),
                new Indicador("Facturado", moeda(faturado),
                        facturasPeriodo.size() + " factura(s) emitida(s)", "tone-blue", "i-file"),
                new Indicador("Recebido", moeda(recebido),
                        pagamentosPeriodo.size() + " pagamento(s) no período", "tone-green", "i-cash"),
                new Indicador("Por receber", moeda(porReceber),
                        pendentes + " factura(s) pendente(s)", "tone-orange", "i-alert"));

        return new RelatorioResposta(
                inicio.toString(),
                fim.toString(),
                clientes.countByEstado("Activo"),
                indicadores,
                porCategoria(leiturasPeriodo, facturasPeriodo),
                receitaMensal());
    }

    /** Consumo e facturação agrupados pela categoria do cliente. */
    private List<Categoria> porCategoria(List<Leitura> leiturasPeriodo, List<Factura> facturasPeriodo) {
        Map<String, DadosCategoria> porNome = new LinkedHashMap<>();
        for (String categoria : List.of("Doméstica", "Comercial", "Institucional")) {
            porNome.put(categoria, new DadosCategoria());
        }

        for (Leitura leitura : leiturasPeriodo) {
            DadosCategoria dados = porNome.computeIfAbsent(leitura.getCliente().getCategoria(),
                    chave -> new DadosCategoria());
            dados.leituras++;
            dados.consumo = dados.consumo.add(leitura.getConsumo());
        }

        for (Factura factura : facturasPeriodo) {
            DadosCategoria dados = porNome.computeIfAbsent(factura.getCliente().getCategoria(),
                    chave -> new DadosCategoria());
            dados.facturado = dados.facturado.add(factura.getValor());
        }

        List<Categoria> resposta = new ArrayList<>();
        porNome.forEach((nome, dados) -> {
            if (dados.leituras > 0 || dados.facturado.signum() > 0) {
                resposta.add(new Categoria(nome, dados.leituras, dados.consumo, dados.facturado));
            }
        });
        return resposta;
    }

    /** Facturação dos últimos 6 meses (para o gráfico de barras). */
    private List<Mes> receitaMensal() {
        Map<String, BigDecimal> porPeriodo = new LinkedHashMap<>();
        for (Factura factura : facturas.findAll()) {
            porPeriodo.merge(factura.getPeriodo(), factura.getValor(), BigDecimal::add);
        }

        List<Mes> resposta = new ArrayList<>();
        LocalDate mes = LocalDate.now(ClienteService.ZONA).withDayOfMonth(1);
        for (int i = 5; i >= 0; i--) {
            LocalDate referencia = mes.minusMonths(i);
            String periodo = LeituraService.periodoDe(referencia);
            resposta.add(new Mes(ABREVIATURAS[referencia.getMonthValue() - 1],
                    porPeriodo.getOrDefault(periodo, BigDecimal.ZERO)));
        }
        return resposta;
    }

    // --- Filtros por data (as entidades guardam instantes, não só datas) ---

    private List<Factura> facturasDe(List<Factura> todas, LocalDate inicio, LocalDate fim) {
        return todas.stream()
                .filter(f -> !f.getDataEmissao().toLocalDate().isBefore(inicio)
                        && !f.getDataEmissao().toLocalDate().isAfter(fim))
                .toList();
    }

    private List<Pagamento> pagamentosDe(List<Pagamento> todos, LocalDate inicio, LocalDate fim) {
        return todos.stream()
                .filter(p -> !p.getDataPagamento().toLocalDate().isBefore(inicio)
                        && !p.getDataPagamento().toLocalDate().isAfter(fim))
                .toList();
    }

    // --- Utilidades ---

    private BigDecimal soma(List<BigDecimal> valores) {
        return valores.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private LocalDate exigirData(String valor, String rotulo) {
        try {
            return LocalDate.parse(valor.trim());
        } catch (DateTimeException e) {
            throw new RegraDeNegocio(rotulo + " inválida: " + valor + " (use AAAA-MM-DD).");
        }
    }

    private String numero(BigDecimal valor, int casas) {
        NumberFormat formato = NumberFormat.getNumberInstance(new Locale("pt", "PT"));
        formato.setMaximumFractionDigits(casas);
        formato.setMinimumFractionDigits(0);
        return formato.format(valor);
    }

    private String moeda(BigDecimal valor) {
        return numero(valor, 2) + " MZN";
    }

    /** Estado agregado de uma categoria, usado no agrupamento. */
    private static class DadosCategoria {
        private long leituras;
        private BigDecimal consumo = BigDecimal.ZERO;
        private BigDecimal facturado = BigDecimal.ZERO;
    }
}
