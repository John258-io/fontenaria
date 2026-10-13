package mz.edu.fontanaria.repositories;

import mz.edu.fontanaria.models.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByTelefone(String telefone);

    boolean existsByTelefone(String telefone);
}
