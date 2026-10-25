package mz.edu.fontanaria.dto;

import java.math.BigDecimal;

/**
 * Dados de um pagamento enviados pelo frontend (POST /api/pagamentos).
 * O valor deve ser positivo e não pode exceder o saldo da factura.
 */
public record PagamentoPedido(Long facturaId,
                              BigDecimal valor,
                              String metodo,
                              String observacao) {
}
