package mz.edu.fontanaria.models;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Leitura de um contador: o valor do contador numa data, de onde sai o consumo
 * do período (actual − anterior).
 *
 * O período de referência é o mês da data da leitura, no formato "AAAA-MM";
 * cada cliente pode ter apenas UMA leitura por mês (restrição única
 * cliente + período).
 */
@Entity
@Table(name = "leituras",
        uniqueConstraints = @UniqueConstraint(columnNames = {"cliente_id", "periodo"}))
public class Leitura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // EAGER: o DTO da leitura mostra sempre os dados do cliente
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    // Mês de referência no formato "AAAA-MM"
    @Column(nullable = false, length = 7)
    private String periodo;

    // Valor lido na leitura anterior (ZERO quando é a primeira do cliente)
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal leituraAnterior = BigDecimal.ZERO;

    // Valor actual do contador, em m³
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal leituraActual = BigDecimal.ZERO;

    // Consumo medido no período, em m³ (leituraActual − leituraAnterior)
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal consumo = BigDecimal.ZERO;

    @Column(nullable = false)
    private LocalDate dataLeitura;

    // Quando a leitura foi lançada no sistema (usado na actividade do painel)
    @Column(nullable = false)
    private LocalDateTime dataRegisto = LocalDateTime.now();

    @Column(length = 200)
    private String observacao;

    // Nome do utilizador que registou a leitura
    @Column(nullable = false, length = 80)
    private String registadoPor;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }

    public String getPeriodo() { return periodo; }
    public void setPeriodo(String periodo) { this.periodo = periodo; }

    public BigDecimal getLeituraAnterior() { return leituraAnterior; }
    public void setLeituraAnterior(BigDecimal leituraAnterior) { this.leituraAnterior = leituraAnterior; }

    public BigDecimal getLeituraActual() { return leituraActual; }
    public void setLeituraActual(BigDecimal leituraActual) { this.leituraActual = leituraActual; }

    public BigDecimal getConsumo() { return consumo; }
    public void setConsumo(BigDecimal consumo) { this.consumo = consumo; }

    public LocalDate getDataLeitura() { return dataLeitura; }
    public void setDataLeitura(LocalDate dataLeitura) { this.dataLeitura = dataLeitura; }

    public LocalDateTime getDataRegisto() { return dataRegisto; }
    public void setDataRegisto(LocalDateTime dataRegisto) { this.dataRegisto = dataRegisto; }

    public String getObservacao() { return observacao; }
    public void setObservacao(String observacao) { this.observacao = observacao; }

    public String getRegistadoPor() { return registadoPor; }
    public void setRegistadoPor(String registadoPor) { this.registadoPor = registadoPor; }
}
