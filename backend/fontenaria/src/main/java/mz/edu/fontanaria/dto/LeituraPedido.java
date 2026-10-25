package mz.edu.fontanaria.dto;

import java.math.BigDecimal;

/**
 * Dados de uma leitura enviados pelo frontend (POST/PUT).
 *
 * O período é calculado no servidor a partir da data da leitura; a leitura
 * anterior e o consumo também são calculados pelo LeituraService.
 */
public record LeituraPedido(Long clienteId,
                            String dataLeitura,
                            BigDecimal leituraActual,
                            String observacao) {
}
