package mz.edu.fontanaria.dto;

import java.math.BigDecimal;

/**
 * Contagens da página de pagamentos: recibos registados, dinheiro recebido
 * (mês e total) e o que ainda falta receber nas facturas pendentes.
 */
public record ResumoPagamentos(long total,
                               long esteMes,
                               BigDecimal recebidoEsteMes,
                               BigDecimal recebidoTotal,
                               BigDecimal porReceber) {
}
