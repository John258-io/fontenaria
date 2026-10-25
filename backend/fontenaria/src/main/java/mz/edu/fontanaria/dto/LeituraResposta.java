package mz.edu.fontanaria.dto;

import mz.edu.fontanaria.models.Leitura;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Leitura devolvida pela API (com os dados do cliente já resumidos). */
public record LeituraResposta(Long id,
                              Long clienteId,
                              String cliente,
                              String telefone,
                              String bairro,
                              String numeroContador,
                              String categoria,
                              BigDecimal leituraAnterior,
                              BigDecimal leituraActual,
                              BigDecimal consumo,
                              String periodo,
                              LocalDate dataLeitura,
                              String observacao,
                              String registadoPor,
                              LocalDateTime dataRegisto,
                              boolean facturada) {

    public static LeituraResposta de(Leitura leitura, boolean facturada) {
        return new LeituraResposta(
                leitura.getId(),
                leitura.getCliente().getId(),
                leitura.getCliente().getNome(),
                leitura.getCliente().getTelefone(),
                leitura.getCliente().getBairro(),
                leitura.getCliente().getNumeroContador(),
                leitura.getCliente().getCategoria(),
                leitura.getLeituraAnterior(),
                leitura.getLeituraActual(),
                leitura.getConsumo(),
                leitura.getPeriodo(),
                leitura.getDataLeitura(),
                leitura.getObservacao(),
                leitura.getRegistadoPor(),
                leitura.getDataRegisto(),
                facturada);
    }
}
