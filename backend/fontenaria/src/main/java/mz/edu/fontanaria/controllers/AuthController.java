package mz.edu.fontanaria.controllers;

import mz.edu.fontanaria.dto.ErroResposta;
import mz.edu.fontanaria.dto.LoginPedido;
import mz.edu.fontanaria.dto.MensagemResposta;
import mz.edu.fontanaria.dto.RespostaLogin;
import mz.edu.fontanaria.models.UsuarioSessao;
import mz.edu.fontanaria.security.JwtService;
import mz.edu.fontanaria.services.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

/**
 * Autenticação da API (substitui o antigo LoginController de Thymeleaf):
 *  - POST /api/auth/login  → { token, usuario }  (público)
 *  - GET  /api/auth/me     → utilizador do token (autenticado)
 *  - POST /api/auth/logout → confirmação         (autenticado; o token é descartado pelo cliente)
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String ERRO_CREDENCIAIS =
            "Número de telefone ou palavra-passe incorrectos.";

    private final AuthService authService;
    private final JwtService jwtService;

    public AuthController(AuthService authService, JwtService jwtService) {
        this.authService = authService;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginPedido pedido) {
        Optional<UsuarioSessao> usuario = authService.autenticar(pedido.telefone(), pedido.senha());

        if (usuario.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErroResposta(ERRO_CREDENCIAIS));
        }

        return ResponseEntity.ok(new RespostaLogin(jwtService.gerar(usuario.get()), usuario.get()));
    }

    @GetMapping("/me")
    public UsuarioSessao me(@AuthenticationPrincipal UsuarioSessao usuario) {
        return usuario;
    }

    @PostMapping("/logout")
    public MensagemResposta logout() {
        // API stateless: o token não pode ser "revogado" aqui, o cliente descarta-o.
        return new MensagemResposta("Sessão terminada.");
    }
}
