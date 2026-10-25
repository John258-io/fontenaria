package mz.edu.fontanaria.dto;

import mz.edu.fontanaria.models.Cliente;

import java.time.LocalDateTime;

/** Cliente devolvido pela API (sem dados internos). */
public record ClienteResposta(Long id,
                              String nome,
                              String telefone,
                              String bairro,
                              String categoria,
                              String numeroContador,
                              String estado,
                              LocalDateTime dataRegisto) {

    public static ClienteResposta de(Cliente cliente) {
        return new ClienteResposta(
                cliente.getId(),
                cliente.getNome(),
                formatarTelefone(cliente.getTelefone()),
                cliente.getBairro(),
                cliente.getCategoria(),
                cliente.getNumeroContador(),
                cliente.getEstado(),
                cliente.getDataRegisto());
    }

    /** "841234567" -> "84 123 4567" */
    private static String formatarTelefone(String digitos) {
        if (digitos == null || digitos.length() != 9) {
            return digitos;
        }
        return digitos.substring(0, 2) + " " + digitos.substring(2, 5) + " " + digitos.substring(5);
    }
}
