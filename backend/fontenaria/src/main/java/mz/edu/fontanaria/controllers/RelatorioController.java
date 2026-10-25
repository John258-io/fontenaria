package mz.edu.fontanaria.controllers;

import mz.edu.fontanaria.dto.RelatorioResposta;
import mz.edu.fontanaria.services.RelatorioService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Relatórios:
 *  - GET /api/relatorios → consumo, facturação e recebimentos de um período
 *    (?inicio=AAAA-MM-DD&fim=AAAA-MM-DD; por omissão, os últimos 6 meses)
 */
@RestController
@RequestMapping("/api/relatorios")
public class RelatorioController {

    private final RelatorioService relatorios;

    public RelatorioController(RelatorioService relatorios) {
        this.relatorios = relatorios;
    }

    @GetMapping
    public RelatorioResposta gerar(@RequestParam(defaultValue = "") String inicio,
                                   @RequestParam(defaultValue = "") String fim) {
        return relatorios.gerar(inicio, fim);
    }
}
