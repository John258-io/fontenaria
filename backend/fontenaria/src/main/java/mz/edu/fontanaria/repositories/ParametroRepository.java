package mz.edu.fontanaria.repositories;

import mz.edu.fontanaria.models.Parametro;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ParametroRepository extends JpaRepository<Parametro, Long> {

    List<Parametro> findAllByOrderByChaveAsc();

    Optional<Parametro> findByChave(String chave);

    boolean existsByChave(String chave);
}
