package mz.edu.fontanaria.dto;

/**
 * Dados de um cliente enviados pelo frontend para criar/editar (POST/PUT).
 *
 * O telefone pode chegar "84 123 4567" ou "+258 84 123 4567": o serviço
 * normaliza-o para só dígitos, tal como acontece no login.
 */
public record ClientePedido(String nome,
                            String telefone,
                            String bairro,
                            String categoria,
                            String numeroContador,
                            String estado) {
}
