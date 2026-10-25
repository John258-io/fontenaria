package mz.edu.fontanaria.dto;

/**
 * Dados de um utilizador enviados pela página de Definições (POST/PUT).
 * A palavra-passe só é obrigatória na criação; ao editar, vazia = manter.
 */
public record UsuarioPedido(String nome,
                            String telefone,
                            String senha,
                            String perfil,
                            Boolean activo) {
}
