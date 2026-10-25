package mz.edu.fontanaria.errors;

import mz.edu.fontanaria.dto.ErroResposta;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Converte as excepções da camada de serviços em respostas JSON no formato
 * que o frontend já espera: { "erro": "mensagem", "momento": "…" }.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(RegraDeNegocio.class)
    public ResponseEntity<ErroResposta> regraDeNegocio(RegraDeNegocio erro) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErroResposta(erro.getMessage()));
    }

    @ExceptionHandler(NaoEncontrado.class)
    public ResponseEntity<ErroResposta> naoEncontrado(NaoEncontrado erro) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErroResposta(erro.getMessage()));
    }

    @ExceptionHandler(SemPermissao.class)
    public ResponseEntity<ErroResposta> semPermissao(SemPermissao erro) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErroResposta(erro.getMessage()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResposta> pedidoInvalido(HttpMessageNotReadableException erro) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErroResposta("Conteúdo do pedido inválido."));
    }
}
