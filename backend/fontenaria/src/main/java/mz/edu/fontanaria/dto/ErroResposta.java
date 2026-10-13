package mz.edu.fontanaria.dto;

import java.time.Instant;

/** Resposta de erro da API: { "erro": "mensagem", "momento": "…" }. */
public record ErroResposta(String erro, Instant momento) {

    public ErroResposta(String erro) {
        this(erro, Instant.now());
    }
}
