package mz.edu.fontanaria.services;

import mz.edu.fontanaria.models.Usuario;
import mz.edu.fontanaria.models.UsuarioSessao;
import mz.edu.fontanaria.repositories.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder encoder;

    public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder encoder) {
        this.usuarioRepository = usuarioRepository;
        this.encoder = encoder;
    }

    /** Devolve o utilizador se o telefone e a palavra-passe estiverem correctos. */
    public Optional<UsuarioSessao> autenticar(String telefone, String senha) {
        if (senha == null || senha.isBlank()) {
            return Optional.empty();
        }
        return usuarioRepository.findByTelefone(normalizarTelefone(telefone))
                .filter(Usuario::isActivo)
                .filter(u -> encoder.matches(senha, u.getSenha()))
                .map(u -> new UsuarioSessao(u.getId(), u.getNome(), u.getPerfil()));
    }

    public Optional<Usuario> porId(Long id) {
        return usuarioRepository.findById(id);
    }

    public Usuario registar(String nome, String telefone, String senha, String perfil) {
        Usuario u = new Usuario();
        u.setNome(nome);
        u.setTelefone(normalizarTelefone(telefone));
        u.setSenha(encoder.encode(senha));
        u.setPerfil(perfil);
        return usuarioRepository.save(u);
    }

    public boolean existeTelefone(String telefone) {
        return usuarioRepository.existsByTelefone(normalizarTelefone(telefone));
    }

    /** "84 123 4567" ou "+258 84 123 4567" -> "841234567" */
    public String normalizarTelefone(String valor) {
        if (valor == null) {
            return "";
        }
        String digitos = valor.replaceAll("\\D", "");
        if (digitos.length() == 12 && digitos.startsWith("258")) {
            digitos = digitos.substring(3);
        }
        return digitos;
    }
}
