package mz.edu.fontanaria.dto;

import mz.edu.fontanaria.models.Factura;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Factura devolvida pela API (saldo e atraso já calculados). */
public record FacturaResposta(Long id,
                              String codigo,
                              Long clienteId,
                              String cliente,
                              String telefone,
                              String bairro,
                              String categoria,
                              String numeroContador,
                              Long leituraId,
                              String periodo,
                              BigDecimal consumo,
                              BigDecimal tarifa,
                              BigDecimal valor,
                              BigDecimal valorPago,
                              BigDecimal saldo,
                              String estado,
                              boolean emAtraso,
                              LocalDateTime dataEmissao,
                              LocalDate vencimento) {

    public static FacturaResposta de(Factura factura, boolean emAtraso) {
        return new FacturaResposta(
                factura.getId(),
                factura.getCodigo(),
                factura.getCliente().getId(),
                factura.getCliente().getNome(),
                factura.getCliente().getTelefone(),
                factura.getCliente().getBairro(),
                factura.getCliente().getCategoria(),
                factura.getCliente().getNumeroContador(),
                factura.getLeitura().getId(),
                factura.getPeriodo(),
                factura.getConsumo(),
                factura.getTarifa(),
                factura.getValor(),
                factura.getValorPago(),
                factura.getValor().subtract(factura.getValorPago()),
                factura.getEstado(),
                emAtraso,
                factura.getDataEmissao(),
                factura.getVencimento());
    }
}
