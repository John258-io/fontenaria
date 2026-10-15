package mz.edu.fontanaria.controllers;

import mz.edu.fontanaria.dto.DashboardResposta;
import mz.edu.fontanaria.dto.DashboardResposta.Actividade;
import mz.edu.fontanaria.dto.DashboardResposta.Atencoes;
import mz.edu.fontanaria.dto.DashboardResposta.Indicador;
import mz.edu.fontanaria.dto.DashboardResposta.Item;
import mz.edu.fontanaria.dto.DashboardResposta.Mes;
import mz.edu.fontanaria.dto.DashboardResposta.Receita;
import mz.edu.fontanaria.models.UsuarioSessao;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;


@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private static final String[] MESES = {
            "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
            "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"
    };

    @GetMapping
    public DashboardResposta dashboard(@AuthenticationPrincipal UsuarioSessao usuario) {
        return new DashboardResposta(
                usuario,
                saudacao(),
                dataActual(),
                indicadores(),
                receita(),
                atencoes(),
                actividade());
    }

    private String saudacao() {
        int hora = LocalTime.now(ZoneId.of("Africa/Maputo")).getHour();
        if (hora < 12) return "Bom dia";
        if (hora < 19) return "Boa tarde";
        return "Boa noite";
    }


    private String dataActual() {
        LocalDate hoje = LocalDate.now(ZoneId.of("Africa/Maputo"));
        return hoje.getDayOfMonth() + " de " + MESES[hoje.getMonthValue() - 1] + " de " + hoje.getYear();
    }

    // --- Dados de demonstração (os mesmos do painel original) ---

    private List<Indicador> indicadores() {
        return List.of(
                new Indicador("Clientes registados", "248", 248,
                        "+12 este mês", false, "tone-blue", "i-users", "clientes.html"),
                new Indicador("Consumo em Junho", "3 842 m³", 3842,
                        "+5,2% vs. Maio", false, "tone-teal", "i-drop", "leituras.html"),
                new Indicador("Pagamentos recebidos", "184 560 MZN", 184560,
                        "78% do facturado", false, "tone-green", "i-cash", "#"),
                new Indicador("Saldo por receber", "52 840 MZN", 52840,
                        "41 facturas pendentes", true, "tone-orange", "i-alert", "#"));
    }

    private Receita receita() {
        return new Receita(
                "784 320 MZN",
                "+8,4% vs. período anterior",
                "Junho",
                List.of(
                        new Mes("Jan", "96k", 96_000),
                        new Mes("Fev", "112k", 112_000),
                        new Mes("Mar", "128k", 128_000),
                        new Mes("Abr", "119k", 119_000),
                        new Mes("Mai", "145k", 145_000),
                        new Mes("Jun", "184k", 184_000)));
    }

    private Atencoes atencoes() {
        return new Atencoes(7, List.of(
                new Item("5 facturas em atraso", "Mais de 30 dias sem pagamento",
                        "tone-orange", "i-alert", "#"),
                new Item("2 leituras em falta", "Referentes ao mês de Junho",
                        "tone-gray", "i-file", "leituras.html"),
                new Item("3 cadastros incompletos", "Falta informação do contador",
                        "tone-gray", "i-users", "clientes.html")));
    }

    private List<Actividade> actividade() {
        return List.of(
                new Actividade("João Cossa", "REC-2025-0458", "Pagamento", "Hoje, 10:42",
                        "M-Pesa", "+ 860,00 MZN", "Pago", "pill-green", true),
                new Actividade("Ana Mucavele", "FAT-2025-0621", "Factura", "Hoje, 09:18",
                        "—", "1 240,00 MZN", "Pendente", "pill-orange", false),
                new Actividade("Emília Chongo", "REC-2025-0457", "Pagamento", "Ontem, 16:05",
                        "Numerário", "+ 480,00 MZN", "Pago", "pill-green", true),
                new Actividade("Rosa Simango", "LEI-2025-0833", "Leitura", "Ontem, 14:22",
                        "12 m³", "—", "Registada", "pill-green", false));
    }
}
