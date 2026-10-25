package mz.edu.fontanaria.repositories;

import mz.edu.fontanaria.models.Factura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface FacturaRepository extends JpaRepository<Factura, Long> {

    /**
     * Facturas com filtro opcional por pesquisa (código ou dados do cliente),
     * estado e período. Os três filtros vêm sempre preenchidos ("" = tudo).
     */
    @Query("""
           SELECT f FROM Factura f
           JOIN f.cliente c
           WHERE (:busca = '' OR LOWER(f.codigo) LIKE LOWER(CONCAT('%', :busca, '%'))
                              OR LOWER(c.nome) LIKE LOWER(CONCAT('%', :busca, '%'))
                              OR LOWER(c.bairro) LIKE LOWER(CONCAT('%', :busca, '%'))
                              OR LOWER(c.numeroContador) LIKE LOWER(CONCAT('%', :busca, '%')))
             AND (:estado = '' OR f.estado = :estado)
             AND (:periodo = '' OR f.periodo = :periodo)
           ORDER BY f.dataEmissao DESC, f.id DESC
           """)
    List<Factura> filtrar(@Param("busca") String busca,
                          @Param("estado") String estado,
                          @Param("periodo") String periodo);

    List<Factura> findAllByEstadoOrderByVencimentoAsc(String estado);

    List<Factura> findByPeriodoOrderByDataEmissaoAsc(String periodo);

    List<Factura> findTop5ByOrderByDataEmissaoDesc();

    boolean existsByLeituraId(Long leituraId);

    long countByCodigoStartingWith(String prefixo);

    long countByEstado(String estado);

    long countByEstadoAndVencimentoBefore(String estado, LocalDate data);

    /** Soma ainda em dívida (valor − valorPago) das facturas pendentes. */
    @Query("SELECT COALESCE(SUM(f.valor - f.valorPago), 0) FROM Factura f WHERE f.estado = 'Pendente'")
    BigDecimal saldoPorReceber();

    /** Soma do valor facturado num período. */
    @Query("SELECT COALESCE(SUM(f.valor), 0) FROM Factura f WHERE f.periodo = :periodo")
    BigDecimal faturadoDoPeriodo(@Param("periodo") String periodo);
}
