package mz.edu.fontanaria.dto;

import java.math.BigDecimal;

/** Contagens da página de facturas (cartões de indicadores). */
public record ResumoFacturas(long total,
                             long pendentes,
                             long pagas,
                             BigDecimal faturado,
                             BigDecimal saldoPorReceber) {
}
