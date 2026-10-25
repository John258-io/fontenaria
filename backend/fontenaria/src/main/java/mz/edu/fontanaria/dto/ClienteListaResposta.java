package mz.edu.fontanaria.dto;

import java.util.List;

/**
 * Resposta de GET /api/clientes: a lista (possivelmente filtrada pela pesquisa)
 * e as contagens mostradas nos cartões do topo da página.
 */
public record ClienteListaResposta(ResumoClientes resumo,
                                   List<ClienteResposta> clientes) {
}
