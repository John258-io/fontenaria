package mz.edu.fontanaria.dto;

import mz.edu.fontanaria.models.UsuarioSessao;

/**
 * Resposta ao login: o token JWT e os dados do utilizador para a interface.
 */
public record RespostaLogin(String token, UsuarioSessao usuario) {
}
