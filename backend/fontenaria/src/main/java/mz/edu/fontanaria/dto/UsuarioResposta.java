package mz.edu.fontanaria.dto;

import mz.edu.fontanaria.models.Usuario;

/** Utilizador devolvido pela API (nunca inclui a palavra-passe). */
public record UsuarioResposta(Long id,
                              String nome,
                              String telefone,
                              String perfil,
                              boolean activo) {

    public static UsuarioResposta de(Usuario usuario) {
        return new UsuarioResposta(
                usuario.getId(),
                usuario.getNome(),
                formatarTelefone(usuario.getTelefone()),
                usuario.getPerfil(),
                usuario.isActivo());
    }

    /** "841234567" -> "84 123 4567" */
    private static String formatarTelefone(String digitos) {
        if (digitos == null || digitos.length() != 9) {
            return digitos;
        }
        return digitos.substring(0, 2) + " " + digitos.substring(2, 5) + " " + digitos.substring(5);
    }
}
