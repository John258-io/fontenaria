package mz.edu.fontanaria.config;

import mz.edu.fontanaria.models.Cliente;
import mz.edu.fontanaria.models.Factura;
import mz.edu.fontanaria.models.Leitura;
import mz.edu.fontanaria.models.Pagamento;
import mz.edu.fontanaria.models.Parametro;
import mz.edu.fontanaria.repositories.ClienteRepository;
import mz.edu.fontanaria.repositories.FacturaRepository;
import mz.edu.fontanaria.repositories.LeituraRepository;
import mz.edu.fontanaria.repositories.PagamentoRepository;
import mz.edu.fontanaria.repositories.ParametroRepository;
import mz.edu.fontanaria.services.ClienteService;
import mz.edu.fontanaria.services.FacturaService;
import mz.edu.fontanaria.services.LeituraService;
import mz.edu.fontanaria.services.ParametroService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Preenche as páginas de leituras, facturas e pagamentos com dados de
 * demonstração (5 meses para trás), para o sistema não arrancar vazio.
 *
 * Corre depois dos clientes (ver @Order) e só quando ainda não há leituras.
 * Só para desenvolvimento: remova esta classe antes de produção.
 */
@Component
@Order(3)
public class DadosDemoIniciais implements CommandLineRunner {

    private final ClienteRepository clientes;
    private final LeituraRepository leituras;
    private final FacturaRepository facturas;
    private final PagamentoRepository pagamentos;
    private final ParametroRepository parametros;

    public DadosDemoIniciais(ClienteRepository clientes,
                             LeituraRepository leituras,
                             FacturaRepository facturas,
                             PagamentoRepository pagamentos,
                             ParametroRepository parametros) {
        this.clientes = clientes;
        this.leituras = leituras;
        this.facturas = facturas;
        this.pagamentos = pagamentos;
        this.parametros = parametros;
    }

    @Override
    public void run(String... args) {
        if (leituras.count() > 0 || facturas.count() > 0 || pagamentos.count() > 0) {
            return;
        }

        List<Cliente> comContador = clientes.findAllByOrderByNomeAsc().stream()
                .filter(c -> c.getNumeroContador() != null)
                .toList();

        LocalDate mesActual = LocalDate.now(ClienteService.ZONA).withDayOfMonth(1);
        int indice = 0;

        for (Cliente cliente : comContador) {
            BigDecimal acumulado = new BigDecimal(60 + (indice * 17) % 90);

            // 5 meses de leituras: a mais antiga → a do mês actual (hoje − N meses)
            for (int voltasAtras = 4; voltasAtras >= 0; voltasAtras--) {
                // Dois clientes ainda sem leitura no mês actual (mostra "leituras em falta")
                if (voltasAtras == 0 && (indice == 2 || indice == 6)) {
                    continue;
                }

                LocalDate mes = mesActual.minusMonths(voltasAtras);
                int consumo = 7 + ((indice * 5 + voltasAtras * 3) % 12);
                LocalDate data = mes.withDayOfMonth(Math.min(24, 10 + (indice % 10)));

                Leitura leitura = novaLeitura(cliente, data, acumulado, consumo);
                leituras.save(leitura);
                acumulado = leitura.getLeituraActual();

                // As leituras do mês actual ficam por facturar (para o botão "gerar do mês")
                if (voltasAtras > 0) {
                    Factura factura = novaFactura(leitura, leitura.getDataRegisto());

                    // Pagamentos conforme a antiguidade da factura
                    if (voltasAtras >= 2) {
                        String metodo = indice % 2 == 0 ? "Numerário" : "M-Pesa";
                        novoPagamento(factura, factura.getValor(), metodo, data, "Factura liquidada");
                    } else if (indice % 2 == 0) {
                        novoPagamento(factura, factura.getValor(), "Multicaixa", data, "Factura liquidada");
                    } else if (indice % 3 == 1) {
                        // Pagamento parcial: a factura continua pendente com saldo
                        BigDecimal parcial = factura.getValor()
                                .multiply(new BigDecimal("0.6")).setScale(2, RoundingMode.HALF_UP);
                        novoPagamento(factura, parcial, "e-Mola", data, "Pagamento parcial");
                    }
                }
            }
            indice++;
        }
    }

    private Leitura novaLeitura(Cliente cliente, LocalDate data, BigDecimal anterior, int consumo) {
        Leitura leitura = new Leitura();
        leitura.setCliente(cliente);
        leitura.setDataLeitura(data);
        leitura.setPeriodo(LeituraService.periodoDe(data));
        leitura.setLeituraAnterior(anterior);
        leitura.setLeituraActual(anterior.add(BigDecimal.valueOf(consumo)));
        leitura.setConsumo(new BigDecimal(consumo));
        leitura.setDataRegisto(data.atTime(10, 15));
        leitura.setObservacao(null);
        leitura.setRegistadoPor("Ernesto Sitoe");
        return leitura;
    }

    private Factura novaFactura(Leitura leitura, LocalDateTime emissao) {
        BigDecimal tarifa = tarifaDe(leitura.getCliente().getCategoria());
        int dias = diasVencimento();

        Factura factura = new Factura();
        factura.setCodigo(proximoCodigo());
        factura.setCliente(leitura.getCliente());
        factura.setLeitura(leitura);
        factura.setPeriodo(leitura.getPeriodo());
        factura.setConsumo(leitura.getConsumo());
        factura.setTarifa(tarifa);
        factura.setValor(leitura.getConsumo().multiply(tarifa).setScale(2, RoundingMode.HALF_UP));
        factura.setValorPago(BigDecimal.ZERO);
        factura.setDataEmissao(emissao);
        factura.setVencimento(emissao.toLocalDate().plusDays(dias));
        factura.setEstado(FacturaService.PENDENTE);
        return facturas.save(factura);
    }

    private void novoPagamento(Factura factura, BigDecimal valor, String metodo,
                               LocalDate data, String observacao) {
        Pagamento pagamento = new Pagamento();
        pagamento.setCodigo(proximoCodigoPagamento());
        pagamento.setFactura(factura);
        pagamento.setValor(valor);
        pagamento.setDataPagamento(data.atTime(15, 30));
        pagamento.setMetodo(metodo);
        pagamento.setRecebidoPor("Ernesto Sitoe");
        pagamento.setObservacao(observacao);
        pagamentos.save(pagamento);

        BigDecimal pago = factura.getValorPago().add(valor);
        factura.setValorPago(pago);
        factura.setEstado(pago.compareTo(factura.getValor()) >= 0
                ? FacturaService.PAGA : FacturaService.PENDENTE);
        facturas.save(factura);
    }

    private BigDecimal tarifaDe(String categoria) {
        return parametros.findByChave(ParametroService.chaveTarifa(categoria))
                .map(Parametro::getValor)
                .map(BigDecimal::new)
                .orElse(new BigDecimal("45"));
    }

    private int diasVencimento() {
        return parametros.findByChave("vencimento.dias")
                .map(Parametro::getValor)
                .map(Integer::parseInt)
                .orElse(15);
    }

    /** "FAT-2026-0001", continuando a sequência já existente. */
    private String proximoCodigo() {
        int ano = LocalDate.now(ClienteService.ZONA).getYear();
        String prefixo = "FAT-" + ano + "-";
        return prefixo + String.format("%04d", facturas.countByCodigoStartingWith(prefixo) + 1);
    }

    /** "REC-2026-0001", continuando a sequência já existente. */
    private String proximoCodigoPagamento() {
        int ano = LocalDate.now(ClienteService.ZONA).getYear();
        String prefixo = "REC-" + ano + "-";
        return prefixo + String.format("%04d", pagamentos.countByCodigoStartingWith(prefixo) + 1);
    }
}
