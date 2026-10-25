package mz.edu.fontanaria.controllers;

import mz.edu.fontanaria.dto.DashboardResposta;
import mz.edu.fontanaria.dto.DashboardResposta.Actividade;
import mz.edu.fontanaria.dto.DashboardResposta.Atencoes;
import mz.edu.fontanaria.dto.DashboardResposta.Indicador;
import mz.edu.fontanaria.dto.DashboardResposta.Item;
import mz.edu.fontanaria.dto.DashboardResposta.Mes;
import mz.edu.fontanaria.dto.DashboardResposta.Receita;
import mz.edu.fontanaria.dto.ResumoClientes;
import mz.edu.fontanaria.dto.ResumoLeituras;
import mz.edu.fontanaria.models.Factura;
import mz.edu.fontanaria.models.Leitura;
import mz.edu.fontanaria.models.Pagamento;
import mz.edu.fontanaria.models.UsuarioSessao;
import mz.edu.fontanaria.services.ClienteService;
import mz.edu.fontanaria.services.FacturaService;
import mz.edu.fontanaria.services.LeituraService;
import mz.edu.fontanaria.services.PagamentoService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Painel "Visão geral": indicadores, receita e actividade recente, agora
 * calculados a partir da base de dados (clientes, leituras, facturas e
 * pagamentos).
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private static final ZoneId ZONA = ZoneId.of("Africa/Maputo");

    private static final String[] MESES = {
            "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
            "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"
    };

    private static final String[] ABREVIATURAS = {
            "Jan", "Fev", "Mar", "Abr", "Mai", "Jun",
            "Jul", "Ago", "Set", "Out", "Nov", "Dez"
    };

    private final ClienteService clientes;
    private final LeituraService leituras;
    private final FacturaService facturas;
    private final PagamentoService pagamentos;

    public DashboardController(ClienteService clientes,
                               LeituraService leituras,
                               FacturaService facturas,
                               PagamentoService pagamentos) {
        this.clientes = clientes;
        this.leituras = leituras;
        this.facturas = facturas;
        this.pagamentos = pagamentos;
    }

    @GetMapping
    public DashboardResposta dashboard(@AuthenticationPrincipal UsuarioSessao usuario) {
        return new DashboardResposta(
                usuario,
                saudacao(),
                dataActual(),
                indicadores(),
                receita(),
                atencoes(),
                actividade());
    }

    private String saudacao() {
        int hora = LocalTime.now(ZONA).getHour();
        if (hora < 12) return "Bom dia";
        if (hora < 19) return "Boa tarde";
        return "Boa noite";
    }

    private String dataActual() {
        LocalDate hoje = LocalDate.now(ZONA);
        return hoje.getDayOfMonth() + " de " + MESES[hoje.getMonthValue() - 1] + " de " + hoje.getYear();
    }

    // --- Cartões de indicadores (dados reais do mês corrente) ---

    private List<Indicador> indicadores() {
        String periodo = LeituraService.periodoActual();
        ResumoClientes resumoClientes = clientes.resumo();
        ResumoLeituras resumoLeituras = leituras.resumo();

        BigDecimal consumo = leituras.consumoDoPeriodo(periodo);
        BigDecimal recebido = pagamentos.recebidoEsteMes();
        BigDecimal faturado = facturas.faturadoDoPeriodo(periodo);
        BigDecimal saldo = facturas.saldoPorReceber();
        long pendentes = facturas.resumo().pendentes();

        String notaConsumo = resumoLeituras.esteMes() > 0
                ? resumoLeituras.esteMes() + " leitura(s) registada(s)"
                : "Ainda sem leituras";

        String notaRecebido = faturado.signum() > 0
                ? percentagem(recebido, faturado) + "% do facturado"
                : "Nada facturado este mês";

        return List.of(
                new Indicador("Clientes registados", numero(resumoClientes.total()), resumoClientes.total(),
                        "+" + resumoClientes.novosEsteMes() + " este mês", false,
                        "tone-blue", "i-users", "clientes.html"),
                new Indicador("Consumo do mês", numero(consumo) + " m³", consumo.longValue(),
                        notaConsumo, false, "tone-teal", "i-drop", "leituras.html"),
                new Indicador("Pagamentos recebidos", moeda(recebido), recebido.longValue(),
                        notaRecebido, false, "tone-green", "i-cash", "pagamentos.html"),
                new Indicador("Saldo por receber", moeda(saldo), saldo.longValue(),
                        plural(pendentes, "factura pendente", "facturas pendentes"), saldo.signum() > 0,
                        "tone-orange", "i-alert", "facturas.html"));
    }

    // --- Receita mensal (últimos 6 meses de facturação) ---

    private Receita receita() {
        List<FacturaService.TotalMes> meses = facturas.receitaMensal(6);

        BigDecimal total = meses.stream()
                .map(FacturaService.TotalMes::valor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Mes> barras = meses.stream()
                .map(m -> new Mes(abreviatura(m.periodo()), rotulo(m.valor()), m.valor().longValue()))
                .toList();

        return new Receita(moeda(total), variacao(meses), abreviaturaActual(), barras);
    }

    /** "+8,4% vs. mês anterior" (ou o total dos 6 meses, ainda sem comparação). */
    private String variacao(List<FacturaService.TotalMes> meses) {
        if (meses.size() < 2) {
            return "—";
        }
        BigDecimal anterior = meses.get(meses.size() - 2).valor();
        BigDecimal actual = meses.get(meses.size() - 1).valor();
        if (anterior.signum() <= 0) {
            return "Primeiros meses de operação";
        }
        BigDecimal percentagem = actual.subtract(anterior)
                .multiply(new BigDecimal("100"))
                .divide(anterior, 1, RoundingMode.HALF_UP);
        String sinal = percentagem.signum() < 0 ? "" : "+";
        return sinal + percentagem.toPlainString().replace(".", ",") + "% vs. mês anterior";
    }

    // --- Atenção necessária ---

    private Atencoes atencoes() {
        String periodo = LeituraService.periodoActual();
        long atrasos = facturas.emAtraso();
        long faltas = leituras.clientesSemLeitura(periodo);
        long incompletos = clientes.resumo().incompletos();

        List<Item> itens = new ArrayList<>();

        if (atrasos > 0) {
            itens.add(new Item(plural(atrasos, "factura em atraso", "facturas em atraso"),
                    "Vencidas sem pagamento", "tone-orange", "i-alert", "facturas.html"));
        }
        if (faltas > 0) {
            itens.add(new Item(plural(faltas, "leitura em falta", "leituras em falta"),
                    "Referentes ao mês de " + MESES[mesActual() - 1],
                    "tone-gray", "i-file", "leituras.html"));
        }
        if (incompletos > 0) {
            itens.add(new Item(plural(incompletos, "cadastro incompleto", "cadastros incompletos"),
                    "Falta informação do contador",
                    "tone-gray", "i-users", "clientes.html"));
        }
        if (itens.isEmpty()) {
            itens.add(new Item("Tudo em dia", "Nenhuma pendência para resolver",
                    "tone-green", "i-info", "#"));
        }

        return new Atencoes(itens.size(), itens);
    }

    // --- Actividade recente (pagamentos, facturas e leituras mais recentes) ---

    private List<Actividade> actividade() {
        List<Momento> momentos = new ArrayList<>();

        for (Pagamento pagamento : pagamentos.ultimas()) {
            momentos.add(new Momento(pagamento.getDataPagamento(), new Actividade(
                    pagamento.getFactura().getCliente().getNome(),
                    pagamento.getCodigo(),
                    "Pagamento",
                    formatarMomento(pagamento.getDataPagamento()),
                    pagamento.getMetodo(),
                    "+ " + moedaPreciso(pagamento.getValor()),
                    "Pago", "pill-green", true)));
        }

        for (Factura factura : facturas.ultimas()) {
            boolean paga = FacturaService.PAGA.equals(factura.getEstado());
            momentos.add(new Momento(factura.getDataEmissao(), new Actividade(
                    factura.getCliente().getNome(),
                    factura.getCodigo(),
                    "Factura",
                    formatarMomento(factura.getDataEmissao()),
                    factura.getConsumo().stripTrailingZeros().toPlainString() + " m³",
                    moedaPreciso(factura.getValor()),
                    factura.getEstado(), paga ? "pill-green" : "pill-orange", false)));
        }

        for (Leitura leitura : leituras.ultimas()) {
            String contador = leitura.getCliente().getNumeroContador();
            momentos.add(new Momento(leitura.getDataRegisto(), new Actividade(
                    leitura.getCliente().getNome(),
                    contador == null ? "—" : contador,
                    "Leitura",
                    formatarMomento(leitura.getDataRegisto()),
                    leitura.getConsumo().stripTrailingZeros().toPlainString() + " m³",
                    "—", "Registada", "pill-green", false)));
        }

        return momentos.stream()
                .sorted(Comparator.comparing(Momento::quando).reversed())
                .limit(8)
                .map(Momento::linha)
                .toList();
    }

    /** "Hoje, 10:42" / "Ontem, 16:05" / "12 Out, 09:18" */
    private String formatarMomento(LocalDateTime momento) {
        LocalDate hoje = LocalDate.now(ZONA);
        LocalDate data = momento.toLocalDate();
        String hora = String.format("%02d:%02d", momento.getHour(), momento.getMinute());

        if (data.equals(hoje)) {
            return "Hoje, " + hora;
        }
        if (data.equals(hoje.minusDays(1))) {
            return "Ontem, " + hora;
        }
        return data.getDayOfMonth() + " " + ABREVIATURAS[data.getMonthValue() - 1] + ", " + hora;
    }

    // --- Utilidades de formatação ---

    /** 184560 -> "184 560" (separador de milhares do português de Moçambique). */
    private String numero(long valor) {
        return NumberFormat.getIntegerInstance(new Locale("pt", "PT")).format(valor);
    }

    private String numero(BigDecimal valor) {
        return numero(valor.longValue());
    }

    /** 184560 -> "184 560 MZN" */
    private String moeda(BigDecimal valor) {
        return numero(valor) + " MZN";
    }

    /** 860 -> "860,00 MZN" */
    private String moedaPreciso(BigDecimal valor) {
        NumberFormat formato = NumberFormat.getNumberInstance(new Locale("pt", "PT"));
        formato.setMinimumFractionDigits(2);
        formato.setMaximumFractionDigits(2);
        return formato.format(valor) + " MZN";
    }

    private String percentagem(BigDecimal parte, BigDecimal todo) {
        if (todo.signum() <= 0) {
            return "0";
        }
        return parte.multiply(new BigDecimal("100"))
                .divide(todo, 0, RoundingMode.HALF_UP)
                .toPlainString();
    }

    /** 96000 -> "96k"; 860 -> "860" */
    private String rotulo(BigDecimal valor) {
        long inteiro = valor.longValue();
        return inteiro >= 1000 ? (inteiro / 1000) + "k" : String.valueOf(inteiro);
    }

    /** "2026-10" -> "Out" */
    private String abreviatura(String periodo) {
        int mes = Integer.parseInt(periodo.substring(5));
        return ABREVIATURAS[mes - 1];
    }

    private String abreviaturaActual() {
        return ABREVIATURAS[mesActual() - 1];
    }

    private int mesActual() {
        return LocalDate.now(ZONA).getMonthValue();
    }

    /** 1 -> "1 cadastro incompleto"; 3 -> "3 cadastros incompletos". */
    private String plural(long quantidade, String singular, String plural) {
        return quantidade + " " + (quantidade == 1 ? singular : plural);
    }

    /** Par (momento, linha) para ordenar a actividade recente. */
    private record Momento(LocalDateTime quando, Actividade linha) {
    }
}
