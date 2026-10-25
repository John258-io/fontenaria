package mz.edu.fontanaria.services;

import mz.edu.fontanaria.dto.ParametroPedido;
import mz.edu.fontanaria.dto.ParametroResposta;
import mz.edu.fontanaria.errors.RegraDeNegocio;
import mz.edu.fontanaria.errors.SemPermissao;
import mz.edu.fontanaria.models.Parametro;
import mz.edu.fontanaria.models.UsuarioSessao;
import mz.edu.fontanaria.repositories.ParametroRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Parâmetros do sistema: tarifa por m³ de cada categoria e prazo de
 * vencimento das facturas. Só um administrador pode alterá-los.
 */
@Service
public class ParametroService {

    /** Chaves reconhecidas e a descrição mostrada na página de Definições. */
    public static final Map<String, String> CONHECIDOS = Map.of(
            "tarifa.domestica", "Tarifa por m³ — categoria Doméstica (MZN)",
            "tarifa.comercial", "Tarifa por m³ — categoria Comercial (MZN)",
            "tarifa.institucional", "Tarifa por m³ — categoria Institucional (MZN)",
            "vencimento.dias", "Dias até ao vencimento das facturas");

    private final ParametroRepository repositorio;

    public ParametroService(ParametroRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Transactional(readOnly = true)
    public List<ParametroResposta> listar() {
        return repositorio.findAllByOrderByChaveAsc().stream()
                .map(p -> new ParametroResposta(p.getChave(), p.getValor(), p.getDescricao()))
                .toList();
    }

    /** Actualiza os parâmetros enviados (apenas administradores). */
    @Transactional
    public void actualizar(List<ParametroPedido> pedidos, UsuarioSessao usuario) {
        exigirAdministrador(usuario);

        if (pedidos == null || pedidos.isEmpty()) {
            throw new RegraDeNegocio("Não há parâmetros para actualizar.");
        }

        for (ParametroPedido pedido : pedidos) {
            if (pedido == null || pedido.chave() == null || !CONHECIDOS.containsKey(pedido.chave())) {
                throw new RegraDeNegocio("Parâmetro desconhecido.");
            }
            String valor = pedido.valor() == null ? "" : pedido.valor().trim();
            validar(pedido.chave(), valor);

            Parametro parametro = repositorio.findByChave(pedido.chave())
                    .orElseGet(() -> new Parametro(pedido.chave(), valor, CONHECIDOS.get(pedido.chave())));
            parametro.setValor(valor);
            repositorio.save(parametro);
        }
    }

    /** Tarifa (MZN por m³) da categoria do cliente. */
    @Transactional(readOnly = true)
    public BigDecimal tarifaDe(String categoria) {
        String chave = chaveTarifa(categoria);
        return numero(chave, new BigDecimal("45"));
    }

    /** Dias entre a emissão e o vencimento de uma factura. */
    @Transactional(readOnly = true)
    public int diasVencimento() {
        return numero("vencimento.dias", BigDecimal.valueOf(15)).intValue();
    }

    /** "Doméstica" -> "tarifa.domestica" */
    public static String chaveTarifa(String categoria) {
        String base = categoria == null ? "domestica" : categoria.trim().toLowerCase(Locale.ROOT);
        return switch (base) {
            case "comercial" -> "tarifa.comercial";
            case "institucional" -> "tarifa.institucional";
            default -> "tarifa.domestica";
        };
    }

    // --- Regras ---

    private BigDecimal numero(String chave, BigDecimal padrao) {
        Optional<Parametro> parametro = repositorio.findByChave(chave);
        if (parametro.isEmpty()) {
            return padrao;
        }
        try {
            return new BigDecimal(parametro.get().getValor().trim());
        } catch (NumberFormatException e) {
            return padrao;
        }
    }

    private void validar(String chave, String valor) {
        if (valor.isEmpty()) {
            throw new RegraDeNegocio("O valor do parâmetro " + chave + " é obrigatório.");
        }
        boolean inteiro = chave.startsWith("vencimento.");
        try {
            BigDecimal numero = new BigDecimal(valor);
            if (numero.signum() <= 0) {
                throw new RegraDeNegocio("O valor do parâmetro " + chave + " deve ser positivo.");
            }
            if (inteiro && numero.stripTrailingZeros().scale() > 0) {
                throw new RegraDeNegocio("O parâmetro " + chave + " deve ser um número inteiro.");
            }
        } catch (NumberFormatException e) {
            throw new RegraDeNegocio("O valor do parâmetro " + chave + " deve ser numérico (ex.: 45 ou 15).");
        }
    }

    private void exigirAdministrador(UsuarioSessao usuario) {
        if (usuario == null || !"Administrador".equals(usuario.getPerfil())) {
            throw new SemPermissao("Apenas administradores podem alterar os parâmetros do sistema.");
        }
    }
}
