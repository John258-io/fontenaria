package mz.edu.fontanaria.dto;

/** Mensagem da API com um número anexado (ex.: "3 facturas geradas."). */
public record MensagemContagem(String mensagem, long quantidade) {

    public static MensagemContagem de(String mensagem, long quantidade) {
        return new MensagemContagem(mensagem, quantidade);
    }
}
