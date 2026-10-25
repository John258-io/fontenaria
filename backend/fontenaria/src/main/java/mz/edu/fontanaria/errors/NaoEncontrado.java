package mz.edu.fontanaria.errors;

/**
 * O recurso pedido não existe (ex.: cliente com id inexistente). A API responde com 404.
 */
public class NaoEncontrado extends RuntimeException {

    public NaoEncontrado(String mensagem) {
        super(mensagem);
    }
}
