package mz.edu.fontanaria.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Resposta de GET /api/relatorios: o período pedido, os indicadores gerais,
 * o consumo/facturação por categoria e a receita dos últimos meses.
 */
public record RelatorioResposta(String inicio,
                                String fim,
                                long clientesActivos,
                                List<Indicador> indicadores,
                                List<Categoria> categorias,
                                List<Mes> receitaMensal) {

    /** Cartão de indicador do topo do relatório. */
    public record Indicador(String rotulo, String valor, String nota, String tom, String icone) {
    }

    /** Consumo e facturação de uma categoria no período. */
    public record Categoria(String nome,
                            long leituras,
                            BigDecimal consumo,
                            BigDecimal facturado) {
    }

    /** Facturação de um mês (para o gráfico de barras). */
    public record Mes(String nome, BigDecimal valor) {
    }
}
