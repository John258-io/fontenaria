package mz.edu.fontanaria.dto;

import java.math.BigDecimal;

/**
 * Contagens da página de leituras: total registado, leituras do mês actual,
 * consumo medido no mês e clientes ainda por ler.
 */
public record ResumoLeituras(long total,
                             long esteMes,
                             BigDecimal consumoEsteMes,
                             long semLeitura) {
}
