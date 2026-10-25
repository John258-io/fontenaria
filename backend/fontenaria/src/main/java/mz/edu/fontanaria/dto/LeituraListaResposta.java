package mz.edu.fontanaria.dto;

import java.util.List;

/** Resposta de GET /api/leituras: lista (filtrada) + cartões de indicadores. */
public record LeituraListaResposta(ResumoLeituras resumo,
                                   List<LeituraResposta> leituras) {
}
