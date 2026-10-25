package mz.edu.fontanaria.dto;

import java.util.List;

/** Resposta de GET /api/facturas: lista (filtrada) + cartões de indicadores. */
public record FacturaListaResposta(ResumoFacturas resumo,
                                   List<FacturaResposta> facturas) {
}
