package mz.edu.fontanaria.dto;

import java.util.List;

/**
 * Resposta de GET /api/pagamentos: lista (filtrada), cartões de indicadores e
 * as facturas ainda pendentes (para escolher no registo de pagamento).
 */
public record PagamentoListaResposta(ResumoPagamentos resumo,
                                     List<PagamentoResposta> pagamentos,
                                     List<FacturaResposta> pendentes) {
}
