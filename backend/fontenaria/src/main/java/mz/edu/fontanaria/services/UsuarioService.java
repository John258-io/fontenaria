package mz.edu.fontanaria.services;

import mz.edu.fontanaria.dto.UsuarioPedido;
import mz.edu.fontanaria.dto.UsuarioResposta;
import mz.edu.fontanaria.errors.NaoEncontrado;
import mz.edu.fontanaria.errors.RegraDeNegocio;
import mz.edu.fontanaria.errors.SemPermissao;
import mz.edu.fontanaria.models.Usuario;
import mz.edu.fontanaria.models.UsuarioSessao;
import mz.edu.fontanaria.repositories.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Gestão de utilizadores da página de Definições: criação, edição e
 * activação/desactivação de contas. Só um administrador pode fazê-lo — e
 * ninguém desactiva a própria conta.
 */
@Service
public class UsuarioService {

    public static final Set<String> PERFIS = Set.of("Administrador", "Operador");

    private final UsuarioRepository repositorio;
    private final PasswordEncoder encoder;
    private final AuthService auth;

    public UsuarioService(UsuarioRepository repositorio, PasswordEncoder encoder, AuthService auth) {
        this.repositorio = repositorio;
        this.encoder = encoder;
        this.auth = auth;
    }

    @Transactional(readOnly = true)
    public List<UsuarioResposta> listar(UsuarioSessao usuario) {
        exigirAdministrador(usuario);
        return repositorio.findAllByOrderByNomeAsc().stream()
                .map(UsuarioResposta::de)
                .toList();
    }

    @Transactional
    public UsuarioResposta criar(UsuarioPedido pedido, UsuarioSessao usuario) {
        exigirAdministrador(usuario);

        if (pedido == null) {
            throw new RegraDeNegocio("Os dados do utilizador são obrigatórios.");
        }

        String nome = exigirTexto(pedido.nome(), "O nome do utilizador é obrigatório.");
        String telefone = exigirTelefone(pedido.telefone());
        String senha = exigirSenha(pedido.senha(), false);
        String perfil = escolherPerfil(pedido.perfil());

        if (repositorio.existsByTelefone(telefone)) {
            throw new RegraDeNegocio("Já existe um utilizador com o telefone " + pedido.telefone().trim() + ".");
        }

        Usuario novo = new Usuario();
        novo.setNome(nome);
        novo.setTelefone(telefone);
        novo.setSenha(encoder.encode(senha));
        novo.setPerfil(perfil);
        novo.setActivo(true);
        return UsuarioResposta.de(repositorio.save(novo));
    }

    @Transactional
    public UsuarioResposta actualizar(Long id, UsuarioPedido pedido, UsuarioSessao usuario) {
        exigirAdministrador(usuario);

        if (pedido == null) {
            throw new RegraDeNegocio("Os dados do utilizador são obrigatórios.");
        }

        Usuario alvo = porId(id);

        alvo.setNome(exigirTexto(pedido.nome(), "O nome do utilizador é obrigatório."));
        alvo.setTelefone(exigirTelefone(pedido.telefone(), id));
        alvo.setPerfil(escolherPerfil(pedido.perfil()));
        alvo.setActivo(pedido.activo() == null || pedido.activo());

        // Vazio = manter a palavra-passe actual
        String senha = textoOpcional(pedido.senha());
        if (senha != null) {
            alvo.setSenha(encoder.encode(exigirSenha(senha, true)));
        }

        if (id.equals(usuario.getId()) && !alvo.isActivo()) {
            throw new RegraDeNegocio("Não pode desactivar a sua própria conta.");
        }

        return UsuarioResposta.de(repositorio.save(alvo));
    }

    @Transactional(readOnly = true)
    public Usuario porId(Long id) {
        return repositorio.findById(id)
                .orElseThrow(() -> new NaoEncontrado("Utilizador " + id + " não encontrado."));
    }

    // --- Regras de validação ---

    private String exigirTelefone(String valor) {
        String digitos = auth.normalizarTelefone(valor);
        if (!digitos.matches("8\\d{8}")) {
            throw new RegraDeNegocio(
                    "O telefone deve ter 9 dígitos e começar por 8 (ex.: 84 123 4567).");
        }
        return digitos;
    }

    /** Na edição, o telefone pode ser mantido pelo próprio utilizador. */
    private String exigirTelefone(String valor, Long idActual) {
        String digitos = exigirTelefone(valor);
        repositorio.findByTelefone(digitos).ifPresent(existente -> {
            if (!existente.getId().equals(idActual)) {
                throw new RegraDeNegocio("Já existe um utilizador com esse telefone.");
            }
        });
        return digitos;
    }

    private String exigirSenha(String valor, boolean obrigatoria) {
        String senha = textoOpcional(valor);
        if (senha == null) {
            if (obrigatoria) {
                throw new RegraDeNegocio("A palavra-passe não pode ficar vazia.");
            }
            throw new RegraDeNegocio("A palavra-passe é obrigatória.");
        }
        if (senha.length() < 6) {
            throw new RegraDeNegocio("A palavra-passe deve ter pelo menos 6 caracteres.");
        }
        return senha;
    }

    private String escolherPerfil(String valor) {
        String texto = textoOpcional(valor);
        if (texto == null) {
            return "Operador";
        }
        String capitalizado = texto.substring(0, 1).toUpperCase(Locale.ROOT)
                + texto.substring(1).toLowerCase(Locale.ROOT);
        if (!PERFIS.contains(capitalizado)) {
            throw new RegraDeNegocio("Perfil inválido: " + texto + " (use Administrador ou Operador).");
        }
        return capitalizado;
    }

    private String exigirTexto(String valor, String mensagem) {
        String texto = textoOpcional(valor);
        if (texto == null) {
            throw new RegraDeNegocio(mensagem);
        }
        return texto;
    }

    private String textoOpcional(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return valor.trim();
    }

    private void exigirAdministrador(UsuarioSessao usuario) {
        if (usuario == null || !"Administrador".equals(usuario.getPerfil())) {
            throw new SemPermissao("Apenas administradores podem gerir os utilizadores.");
        }
    }
}
