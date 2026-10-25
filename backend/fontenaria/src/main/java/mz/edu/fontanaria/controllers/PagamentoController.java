package mz.edu.fontanaria.controllers;

import mz.edu.fontanaria.dto.MensagemResposta;
import mz.edu.fontanaria.dto.PagamentoListaResposta;
import mz.edu.fontanaria.dto.PagamentoPedido;
import mz.edu.fontanaria.dto.PagamentoResposta;
import mz.edu.fontanaria.models.UsuarioSessao;
import mz.edu.fontanaria.services.PagamentoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Pagamentos recebidos:
 *  - GET    /api/pagamentos       → lista + contagens + facturas pendentes (?busca=&metodo=)
 *  - GET    /api/pagamentos/{id}  → um pagamento
 *  - POST   /api/pagamentos       → regista contra uma factura pendente (201)
 *  - DELETE /api/pagamentos/{id}  → anula um recibo (estorno)
 *
 * Todas as rotas exigem token (ver SecurityConfig).
 */
@RestController
@RequestMapping("/api/pagamentos")
public class PagamentoController {

    private final PagamentoService pagamentos;

    public PagamentoController(PagamentoService pagamentos) {
        this.pagamentos = pagamentos;
    }

    @GetMapping
    public PagamentoListaResposta listar(@RequestParam(defaultValue = "") String busca,
                                         @RequestParam(defaultValue = "") String metodo) {
        return pagamentos.listar(busca, metodo);
    }

    @GetMapping("/{id}")
    public PagamentoResposta porId(@PathVariable Long id) {
        return PagamentoResposta.de(pagamentos.porId(id));
    }

    @PostMapping
    public ResponseEntity<PagamentoResposta> registar(@RequestBody PagamentoPedido pedido,
                                                      @AuthenticationPrincipal UsuarioSessao usuario) {
        PagamentoResposta criado = PagamentoResposta.de(pagamentos.registar(pedido, usuario));
        return ResponseEntity.status(HttpStatus.CREATED).body(criado);
    }

    @DeleteMapping("/{id}")
    public MensagemResposta remover(@PathVariable Long id) {
        pagamentos.remover(id);
        return new MensagemResposta("Pagamento anulado.");
    }
}
