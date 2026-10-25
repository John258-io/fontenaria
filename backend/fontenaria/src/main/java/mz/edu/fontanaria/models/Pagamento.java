package mz.edu.fontanaria.models;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Pagamento recebido de uma factura.
 *
 * Pode ser parcial: enquanto a soma dos pagamentos não chegar ao valor da
 * factura, ela continua "Pendente" (o saldo é calculado em cada acesso).
 */
@Entity
@Table(name = "pagamentos")
public class Pagamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Referência do recibo, ex.: REC-2026-0001
    @Column(nullable = false, unique = true, length = 20)
    private String codigo;

    // EAGER: o DTO do pagamento mostra sempre a factura (e o seu cliente)
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "factura_id", nullable = false)
    private Factura factura;

    // Valor recebido, em MZN (≤ saldo em dívida da factura)
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valor = BigDecimal.ZERO;

    @Column(nullable = false)
    private LocalDateTime dataPagamento = LocalDateTime.now();

    // Numerário, M-Pesa, e-Mola, Transferência ou Multicaixa
    @Column(nullable = false, length = 30)
    private String metodo = "Numerário";

    // Nome do utilizador que recebeu o dinheiro
    @Column(nullable = false, length = 80)
    private String recebidoPor;

    @Column(length = 200)
    private String observacao;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public Factura getFactura() { return factura; }
    public void setFactura(Factura factura) { this.factura = factura; }

    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }

    public LocalDateTime getDataPagamento() { return dataPagamento; }
    public void setDataPagamento(LocalDateTime dataPagamento) { this.dataPagamento = dataPagamento; }

    public String getMetodo() { return metodo; }
    public void setMetodo(String metodo) { this.metodo = metodo; }

    public String getRecebidoPor() { return recebidoPor; }
    public void setRecebidoPor(String recebidoPor) { this.recebidoPor = recebidoPor; }

    public String getObservacao() { return observacao; }
    public void setObservacao(String observacao) { this.observacao = observacao; }
}
