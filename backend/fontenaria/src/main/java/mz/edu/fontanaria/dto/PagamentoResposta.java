package mz.edu.fontanaria.dto;

import mz.edu.fontanaria.models.Pagamento;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Pagamento devolvido pela API (com a factura e o cliente já resumidos). */
public record PagamentoResposta(Long id,
                                String codigo,
                                Long facturaId,
                                String facturaCodigo,
                                Long clienteId,
                                String cliente,
                                BigDecimal valor,
                                LocalDateTime dataPagamento,
                                String metodo,
                                String recebidoPor,
                                String observacao) {

    public static PagamentoResposta de(Pagamento pagamento) {
        return new PagamentoResposta(
                pagamento.getId(),
                pagamento.getCodigo(),
                pagamento.getFactura().getId(),
                pagamento.getFactura().getCodigo(),
                pagamento.getFactura().getCliente().getId(),
                pagamento.getFactura().getCliente().getNome(),
                pagamento.getValor(),
                pagamento.getDataPagamento(),
                pagamento.getMetodo(),
                pagamento.getRecebidoPor(),
                pagamento.getObservacao());
    }
}
