package mz.edu.fontanaria.dto;

/**
 * Corpo do pedido de login: { "telefone": "841234567", "senha": "admin123" }.
 */
public record LoginPedido(String telefone, String senha) {
}
