package mz.edu.fontanaria.controllers;

import mz.edu.fontanaria.dto.UsuarioPedido;
import mz.edu.fontanaria.dto.UsuarioResposta;
import mz.edu.fontanaria.models.UsuarioSessao;
import mz.edu.fontanaria.services.UsuarioService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Gestão de utilizadores (Definições, apenas administradores):
 *  - GET  /api/usuarios       → lista
 *  - POST /api/usuarios       → cria (201)
 *  - PUT  /api/usuarios/{id}  → edita nome, telefone, perfil, estado e senha
 */
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarios;

    public UsuarioController(UsuarioService usuarios) {
        this.usuarios = usuarios;
    }

    @GetMapping
    public List<UsuarioResposta> listar(@AuthenticationPrincipal UsuarioSessao usuario) {
        return usuarios.listar(usuario);
    }

    @PostMapping
    public ResponseEntity<UsuarioResposta> criar(@RequestBody UsuarioPedido pedido,
                                                 @AuthenticationPrincipal UsuarioSessao usuario) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarios.criar(pedido, usuario));
    }

    @PutMapping("/{id}")
    public UsuarioResposta actualizar(@PathVariable Long id,
                                      @RequestBody UsuarioPedido pedido,
                                      @AuthenticationPrincipal UsuarioSessao usuario) {
        return usuarios.actualizar(id, pedido, usuario);
    }
}
