package mz.edu.fontanaria.controllers;

import mz.edu.fontanaria.dto.ParametroPedido;
import mz.edu.fontanaria.dto.ParametroResposta;
import mz.edu.fontanaria.dto.MensagemResposta;
import mz.edu.fontanaria.models.UsuarioSessao;
import mz.edu.fontanaria.services.ParametroService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Parâmetros do sistema (Definições, apenas administradores):
 *  - GET /api/parametros       → lista de parâmetros e descrições
 *  - PUT /api/parametros       → actualiza os valores enviados
 */
@RestController
@RequestMapping("/api/parametros")
public class ParametroController {

    private final ParametroService parametros;

    public ParametroController(ParametroService parametros) {
        this.parametros = parametros;
    }

    @GetMapping
    public List<ParametroResposta> listar() {
        return parametros.listar();
    }

    @PutMapping
    public ResponseEntity<MensagemResposta> actualizar(@RequestBody List<ParametroPedido> pedidos,
                                                       @AuthenticationPrincipal UsuarioSessao usuario) {
        parametros.actualizar(pedidos, usuario);
        return ResponseEntity.ok(new MensagemResposta("Parâmetros actualizados."));
    }
}
