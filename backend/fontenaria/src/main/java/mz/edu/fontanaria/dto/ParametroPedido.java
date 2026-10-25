package mz.edu.fontanaria.dto;

/** Novo valor de um parâmetro enviado pela página de Definições (PUT). */
public record ParametroPedido(String chave,
                              String valor) {
}
