package mz.edu.fontanaria.controllers;

import mz.edu.fontanaria.dto.LeituraListaResposta;
import mz.edu.fontanaria.dto.LeituraPedido;
import mz.edu.fontanaria.dto.LeituraResposta;
import mz.edu.fontanaria.dto.MensagemResposta;
import mz.edu.fontanaria.models.UsuarioSessao;
import mz.edu.fontanaria.services.FacturaService;
import mz.edu.fontanaria.services.LeituraService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Leituras de contadores:
 *  - GET    /api/leituras         → lista + contagens (?busca=…&periodo=AAAA-MM)
 *  - GET    /api/leituras/{id}    → uma leitura
 *  - POST   /api/leituras         → regista (201); calcula período, anterior e consumo
 *  - PUT    /api/leituras/{id}    → edita (recalcula o consumo)
 *  - DELETE /api/leituras/{id}    → elimina (só se ainda não foi facturada)
 *
 * Todas as rotas exigem token (ver SecurityConfig).
 */
@RestController
@RequestMapping("/api/leituras")
public class LeituraController {

    private final LeituraService leituras;
    private final FacturaService facturas;

    public LeituraController(LeituraService leituras, FacturaService facturas) {
        this.leituras = leituras;
        this.facturas = facturas;
    }

    @GetMapping
    public LeituraListaResposta listar(@RequestParam(defaultValue = "") String busca,
                                       @RequestParam(defaultValue = "") String periodo) {
        return leituras.listar(busca, periodo);
    }

    @GetMapping("/{id}")
    public LeituraResposta porId(@PathVariable Long id) {
        return LeituraResposta.de(leituras.porId(id), facturas.jaFacturada(id));
    }

    @PostMapping
    public ResponseEntity<LeituraResposta> criar(@RequestBody LeituraPedido pedido,
                                                 @AuthenticationPrincipal UsuarioSessao usuario) {
        LeituraResposta criada = LeituraResposta.de(leituras.criar(pedido, usuario), false);
        return ResponseEntity.status(HttpStatus.CREATED).body(criada);
    }

    @PutMapping("/{id}")
    public LeituraResposta actualizar(@PathVariable Long id,
                                      @RequestBody LeituraPedido pedido,
                                      @AuthenticationPrincipal UsuarioSessao usuario) {
        return LeituraResposta.de(leituras.actualizar(id, pedido, usuario), facturas.jaFacturada(id));
    }

    @DeleteMapping("/{id}")
    public MensagemResposta remover(@PathVariable Long id) {
        leituras.remover(id);
        return new MensagemResposta("Leitura eliminada.");
    }
}
