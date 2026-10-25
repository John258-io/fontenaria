package mz.edu.fontanaria.controllers;

import mz.edu.fontanaria.dto.FacturaListaResposta;
import mz.edu.fontanaria.dto.FacturaPedido;
import mz.edu.fontanaria.dto.FacturaResposta;
import mz.edu.fontanaria.dto.GerarFacturasPedido;
import mz.edu.fontanaria.dto.MensagemContagem;
import mz.edu.fontanaria.dto.MensagemResposta;
import mz.edu.fontanaria.dto.PagamentoResposta;
import mz.edu.fontanaria.models.Factura;
import mz.edu.fontanaria.services.FacturaService;
import mz.edu.fontanaria.services.PagamentoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Facturação (transacções):
 *  - GET  /api/facturas               → lista + contagens (?busca=&estado=&periodo=)
 *  - GET  /api/facturas/{id}          → uma factura + os seus pagamentos
 *  - POST /api/facturas               → emite a factura de uma leitura (201)
 *  - POST /api/facturas/gerar-mes     → emite todas as facturas em falta de um mês
 *  - DELETE /api/facturas/{id}        → apaga (só facturas sem pagamentos)
 *
 * Todas as rotas exigem token (ver SecurityConfig).
 */
@RestController
@RequestMapping("/api/facturas")
public class FacturaController {

    private final FacturaService facturas;
    private final PagamentoService pagamentos;

    public FacturaController(FacturaService facturas, PagamentoService pagamentos) {
        this.facturas = facturas;
        this.pagamentos = pagamentos;
    }

    @GetMapping
    public FacturaListaResposta listar(@RequestParam(defaultValue = "") String busca,
                                       @RequestParam(defaultValue = "") String estado,
                                       @RequestParam(defaultValue = "") String periodo) {
        return facturas.listar(busca, estado, periodo);
    }

    @GetMapping("/{id}")
    public FacturaResposta porId(@PathVariable Long id) {
        Factura factura = facturas.porId(id);
        return FacturaResposta.de(factura, facturas.emAtraso(factura));
    }

    /** Factura + recibos (usado pelo detalhe da factura no frontend). */
    @GetMapping("/{id}/pagamentos")
    public List<PagamentoResposta> pagamentos(@PathVariable Long id) {
        facturas.porId(id); // garante que a factura existe
        return pagamentos.daFactura(id);
    }

    @PostMapping
    public ResponseEntity<FacturaResposta> gerar(@RequestBody FacturaPedido pedido) {
        if (pedido == null || pedido.leituraId() == null) {
            return ResponseEntity.badRequest().build();
        }
        var factura = facturas.gerar(pedido.leituraId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(FacturaResposta.de(factura, false));
    }

    @PostMapping("/gerar-mes")
    public MensagemContagem gerarMes(@RequestBody GerarFacturasPedido pedido) {
        int criadas = facturas.gerarMes(pedido == null ? null : pedido.periodo());
        return MensagemContagem.de(
                criadas == 1 ? "1 factura emitida." : criadas + " facturas emitidas.", criadas);
    }

    @DeleteMapping("/{id}")
    public MensagemResposta remover(@PathVariable Long id) {
        facturas.remover(id);
        return new MensagemResposta("Factura eliminada.");
    }
}
