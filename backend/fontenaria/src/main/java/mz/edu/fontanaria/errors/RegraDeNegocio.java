package mz.edu.fontanaria.errors;

/**
 * Dados do pedido inválidos (validação falhou). A API responde com 400.
 */
public class RegraDeNegocio extends RuntimeException {

    public RegraDeNegocio(String mensagem) {
        super(mensagem);
    }
}
