package mz.edu.fontanaria.errors;

/**
 * Acção reservada a administradores (convertida em HTTP 403 pelo
 * ApiExceptionHandler).
 */
public class SemPermissao extends RuntimeException {

    public SemPermissao(String mensagem) {
        super(mensagem);
    }
}
