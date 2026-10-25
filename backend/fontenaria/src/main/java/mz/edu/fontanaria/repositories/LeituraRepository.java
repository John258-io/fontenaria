package mz.edu.fontanaria.repositories;

import mz.edu.fontanaria.models.Leitura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface LeituraRepository extends JpaRepository<Leitura, Long> {

    /**
     * Lista de leituras com filtro opcional de pesquisa (dados do cliente) e de
     * período ("AAAA-MM"). Ambos os filtros vêm sempre preenchidos ("" = tudo).
     */
    @Query("""
           SELECT l FROM Leitura l
           JOIN l.cliente c
           WHERE (:periodo = '' OR l.periodo = :periodo)
             AND (:busca = '' OR LOWER(c.nome) LIKE LOWER(CONCAT('%', :busca, '%'))
                              OR LOWER(c.telefone) LIKE LOWER(CONCAT('%', :busca, '%'))
                              OR LOWER(c.bairro) LIKE LOWER(CONCAT('%', :busca, '%'))
                              OR LOWER(c.numeroContador) LIKE LOWER(CONCAT('%', :busca, '%')))
           ORDER BY l.dataLeitura DESC, l.id DESC
           """)
    List<Leitura> filtrar(@Param("busca") String busca, @Param("periodo") String periodo);

    List<Leitura> findByPeriodoOrderByDataLeituraAsc(String periodo);

    /** Leituras anteriores à data indicada, da mais recente para a mais antiga. */
    Optional<Leitura> findTopByClienteIdAndDataLeituraBeforeOrderByDataLeituraDesc(Long clienteId,
                                                                                  LocalDate data);

    boolean existsByClienteIdAndPeriodo(Long clienteId, String periodo);

    boolean existsByClienteIdAndPeriodoAndIdNot(Long clienteId, String periodo, Long id);

    long countByPeriodo(String periodo);

    /** Soma do consumo (m³) num período. */
    @Query("SELECT COALESCE(SUM(l.consumo), 0) FROM Leitura l WHERE l.periodo = :periodo")
    BigDecimal consumoDoPeriodo(@Param("periodo") String periodo);

    /** Clientes activos com contador que ainda NÃO têm leitura no período. */
    @Query("""
           SELECT COUNT(c) FROM Cliente c
           WHERE c.estado = 'Activo'
             AND c.numeroContador IS NOT NULL
             AND NOT EXISTS (SELECT l FROM Leitura l WHERE l.cliente = c AND l.periodo = :periodo)
           """)
    long contarClientesSemLeitura(@Param("periodo") String periodo);

    List<Leitura> findAllByDataLeituraBetweenOrderByDataLeituraAsc(LocalDate inicio, LocalDate fim);

    List<Leitura> findTop5ByOrderByDataRegistoDesc();
}
