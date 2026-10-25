package mz.edu.fontanaria.dto;

/** Pedido para gerar uma factura a partir de uma leitura (POST /api/facturas). */
public record FacturaPedido(Long leituraId) {
}
