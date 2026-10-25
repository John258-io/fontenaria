package mz.edu.fontanaria.repositories;

import mz.edu.fontanaria.models.Pagamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface PagamentoRepository extends JpaRepository<Pagamento, Long> {

    /** Pagamentos com filtro opcional por pesquisa (recibo, factura ou cliente) e método. */
    @Query("""
           SELECT p FROM Pagamento p
           JOIN p.factura f
           JOIN f.cliente c
           WHERE (:busca = '' OR LOWER(p.codigo) LIKE LOWER(CONCAT('%', :busca, '%'))
                              OR LOWER(f.codigo) LIKE LOWER(CONCAT('%', :busca, '%'))
                              OR LOWER(c.nome) LIKE LOWER(CONCAT('%', :busca, '%'))
                              OR LOWER(p.metodo) LIKE LOWER(CONCAT('%', :busca, '%')))
             AND (:metodo = '' OR p.metodo = :metodo)
           ORDER BY p.dataPagamento DESC, p.id DESC
           """)
    List<Pagamento> filtrar(@Param("busca") String busca, @Param("metodo") String metodo);

    List<Pagamento> findByFacturaIdOrderByDataPagamentoDesc(Long facturaId);

    List<Pagamento> findTop5ByOrderByDataPagamentoDesc();

    long countByCodigoStartingWith(String prefixo);

    long countByFacturaId(Long facturaId);

    long countByDataPagamentoAfter(LocalDateTime momento);

    /** Soma do dinheiro recebido entre duas datas. */
    @Query("SELECT COALESCE(SUM(p.valor), 0) FROM Pagamento p WHERE p.dataPagamento BETWEEN :inicio AND :fim")
    BigDecimal totalEntre(@Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim);

    @Query("SELECT COALESCE(SUM(p.valor), 0) FROM Pagamento p")
    BigDecimal totalGeral();
}
