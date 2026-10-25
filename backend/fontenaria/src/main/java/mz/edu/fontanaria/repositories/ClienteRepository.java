package mz.edu.fontanaria.repositories;

import mz.edu.fontanaria.models.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    List<Cliente> findAllByOrderByNomeAsc();

    /** Procura por nome, telefone, bairro ou número do contador. */
    @Query("""
           SELECT c FROM Cliente c
           WHERE LOWER(c.nome) LIKE LOWER(CONCAT('%', :busca, '%'))
              OR LOWER(c.telefone) LIKE LOWER(CONCAT('%', :busca, '%'))
              OR LOWER(c.bairro) LIKE LOWER(CONCAT('%', :busca, '%'))
              OR LOWER(c.numeroContador) LIKE LOWER(CONCAT('%', :busca, '%'))
           ORDER BY c.nome
           """)
    List<Cliente> procurar(@Param("busca") String busca);

    Optional<Cliente> findByNumeroContador(String numeroContador);

    long countByEstado(String estado);

    long countByNumeroContadorIsNull();

    long countByDataRegistoAfter(LocalDateTime momento);
}
