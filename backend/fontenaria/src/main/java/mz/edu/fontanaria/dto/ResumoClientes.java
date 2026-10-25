package mz.edu.fontanaria.dto;

/**
 * Contagens gerais dos clientes, usadas nos cartões de indicadores da página
 * de clientes e no painel (GET /api/dashboard).
 */
public record ResumoClientes(long total,
                             long activos,
                             long suspensos,
                             long incompletos,
                             long novosEsteMes) {
}
