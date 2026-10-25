package mz.edu.fontanaria.dto;

/** Pedido para gerar de uma só vez todas as facturas de um mês ("AAAA-MM"). */
public record GerarFacturasPedido(String periodo) {
}
