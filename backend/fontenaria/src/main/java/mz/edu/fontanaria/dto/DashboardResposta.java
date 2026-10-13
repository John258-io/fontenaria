package mz.edu.fontanaria.dto;

import mz.edu.fontanaria.models.UsuarioSessao;

import java.util.List;

/**
 * Resumo da operação devolvido por GET /api/dashboard.
 *
 * NOTA: os números (clientes, receitas, actividade) são dados de demonstração,
 * os mesmos que a interface mostrava antes da separação front/back. Quando
 * existirem as entidades reais (Cliente, Leitura, Pagamento), substitua-os pelas
 * consultas à base de dados.
 */
public record DashboardResposta(
        UsuarioSessao usuario,
        String saudacao,
        String data,
        List<Indicador> indicadores,
        Receita receita,
        Atencoes atencoes,
        List<Actividade> actividade) {

    /** Cartão de indicador do topo do painel. */
    public record Indicador(String rotulo, String valor, long valorNumerico,
                            String nota, boolean notaDestaque,
                            String tom, String icone, String href) {
    }

    /** Gráfico de barras da receita mensal. */
    public record Receita(String total, String variacao, String mesActual, List<Mes> meses) {
    }

    /** Um mês do gráfico de receita (a altura da barra é calculada no frontend). */
    public record Mes(String nome, String rotulo, long valor) {
    }

    /** Cartão "Atenção necessária". */
    public record Atencoes(int total, List<Item> itens) {
    }

    public record Item(String titulo, String descricao, String tom, String icone, String href) {
    }

    /** Linha da tabela "Actividade recente" (entrada = valor recebido, a verde). */
    public record Actividade(String cliente, String referencia, String tipo, String data,
                             String metodo, String valor, String estado, String tom,
                             boolean entrada) {
    }
}
