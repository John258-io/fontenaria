package mz.edu.fontanaria.controllers;

import mz.edu.fontanaria.dto.ClienteListaResposta;
import mz.edu.fontanaria.dto.ClientePedido;
import mz.edu.fontanaria.dto.ClienteResposta;
import mz.edu.fontanaria.dto.MensagemResposta;
import mz.edu.fontanaria.services.ClienteService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Clientes da Fontanária:
 *  - GET    /api/clientes          → lista + contagens (aceita ?busca=…)
 *  - GET    /api/clientes/{id}     → um cliente
 *  - POST   /api/clientes          → cria (201)
 *  - PUT    /api/clientes/{id}     → actualiza
 *  - DELETE /api/clientes/{id}     → elimina
 *
 * Todas as rotas exigem token (ver SecurityConfig); as validações são feitas
 * no ClienteService e convertidas em 400/404 pelo ApiExceptionHandler.
 */
@RestController
@RequestMapping("/api/clientes")
public class ClienteController {

    private final ClienteService clientes;

    public ClienteController(ClienteService clientes) {
        this.clientes = clientes;
    }

    @GetMapping
    public ClienteListaResposta listar(@RequestParam(defaultValue = "") String busca) {
        return clientes.listar(busca);
    }

    @GetMapping("/{id}")
    public ClienteResposta porId(@PathVariable Long id) {
        return ClienteResposta.de(clientes.porId(id));
    }

    @PostMapping
    public ResponseEntity<ClienteResposta> criar(@RequestBody ClientePedido pedido) {
        ClienteResposta criado = ClienteResposta.de(clientes.criar(pedido));
        return ResponseEntity.status(HttpStatus.CREATED).body(criado);
    }

    @PutMapping("/{id}")
    public ClienteResposta actualizar(@PathVariable Long id, @RequestBody ClientePedido pedido) {
        return ClienteResposta.de(clientes.actualizar(id, pedido));
    }

    @DeleteMapping("/{id}")
    public MensagemResposta remover(@PathVariable Long id) {
        clientes.remover(id);
        return new MensagemResposta("Cliente eliminado.");
    }
}
