package mz.edu.fontanaria.models;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Factura emitida a partir de uma leitura: consumo × tarifa da categoria do
 * cliente.
 *
 * Cada leitura origina no MÁXIMO uma factura. O estado é "Pendente" ou "Paga"
 * e é recalculado sempre que entra ou sai um pagamento (valorPago ≥ valor).
 */
@Entity
@Table(name = "facturas")
public class Factura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Referência mostrada ao cliente, ex.: FAT-2026-0001
    @Column(nullable = false, unique = true, length = 20)
    private String codigo;

    // EAGER: o DTO da factura mostra sempre o cliente e a leitura de origem
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "leitura_id", nullable = false, unique = true)
    private Leitura leitura;

    // Período facturado, no formato "AAAA-MM" (o período da leitura)
    @Column(nullable = false, length = 7)
    private String periodo;

    // Consumo facturado, em m³
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal consumo = BigDecimal.ZERO;

    // Tarifa aplicada, em MZN por m³ (fica guardada mesmo que a tarifa mude)
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal tarifa = BigDecimal.ZERO;

    // Valor total da factura, em MZN (consumo × tarifa)
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valor = BigDecimal.ZERO;

    // Soma dos pagamentos já registados nesta factura
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valorPago = BigDecimal.ZERO;

    @Column(nullable = false)
    private LocalDateTime dataEmissao = LocalDateTime.now();

    // Último dia para pagar sem juros de atraso
    @Column(nullable = false)
    private LocalDate vencimento;

    @Column(nullable = false, length = 20)
    private String estado = "Pendente";

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }

    public Leitura getLeitura() { return leitura; }
    public void setLeitura(Leitura leitura) { this.leitura = leitura; }

    public String getPeriodo() { return periodo; }
    public void setPeriodo(String periodo) { this.periodo = periodo; }

    public BigDecimal getConsumo() { return consumo; }
    public void setConsumo(BigDecimal consumo) { this.consumo = consumo; }

    public BigDecimal getTarifa() { return tarifa; }
    public void setTarifa(BigDecimal tarifa) { this.tarifa = tarifa; }

    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }

    public BigDecimal getValorPago() { return valorPago; }
    public void setValorPago(BigDecimal valorPago) { this.valorPago = valorPago; }

    public LocalDateTime getDataEmissao() { return dataEmissao; }
    public void setDataEmissao(LocalDateTime dataEmissao) { this.dataEmissao = dataEmissao; }

    public LocalDate getVencimento() { return vencimento; }
    public void setVencimento(LocalDate vencimento) { this.vencimento = vencimento; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}
