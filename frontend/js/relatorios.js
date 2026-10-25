/**
 * relatorios.js — página de relatórios: consumo, facturação e recebimentos de
 * um período, facturação mensal e consumo por categoria (depende de api.js,
 * app.js, layout.js e pagina.js).
 */

const fmtRelatorio = new Intl.NumberFormat("pt-PT", { maximumFractionDigits: 2 });

const elemsRelatorio = {
    periodo: document.getElementById("periodo-actual"),
    inicio: document.getElementById("filtro-inicio"),
    fim: document.getElementById("filtro-fim"),
    btnGerar: document.getElementById("btn-gerar"),
    erroLista: document.getElementById("erro-lista"),
    barras: document.getElementById("barras"),
    barLabels: document.getElementById("bar-labels"),
    receitaTotal: document.getElementById("receita-total"),
    receitaNota: document.getElementById("receita-nota"),
    categorias: document.getElementById("categorias"),
    vazio: document.getElementById("vazio")
};

/** Escapa texto antes de o inserir no HTML. */
function escRelatorio(valor) {
    return String(valor ?? "").replace(/[&<>"']/g, caractere => ({
        "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;"
    })[caractere]);
}

function mostrarErroRelatorio(texto) {
    elemsRelatorio.erroLista.textContent = texto || "";
    elemsRelatorio.erroLista.hidden = !texto;
}

/** "2026-10-01" -> "1 out 2026" */
function formatarData(iso) {
    if (!iso) return "—";
    return new Intl.DateTimeFormat("pt-PT", { day: "numeric", month: "short", year: "numeric" })
        .format(new Date(iso));
}

/** Desenha os 4 cartões de indicadores (o backend já formata os valores). */
function desenharIndicadoresRelatorio(relatorio) {
    const ids = [
        ["s-consumo", "s-consumo-nota"],
        ["s-faturado", "s-faturado-nota"],
        ["s-recebido", "s-recebido-nota"],
        ["s-por-receber", "s-por-receber-nota"]
    ];

    relatorio.indicadores.forEach((indicador, indice) => {
        const [valor, nota] = ids[indice];
        if (!valor) return;
        document.getElementById(valor).textContent = indicador.valor;
        document.getElementById(nota).textContent = indicador.nota;
    });
}

/** Gráfico de barras da facturação mensal (últimos 6 meses). */
function desenharReceitaRelatorio(relatorio) {
    const meses = relatorio.receitaMensal;
    const maximo = Math.max(...meses.map(m => Number(m.valor)), 1);

    const total = meses.reduce((soma, m) => soma + Number(m.valor), 0);
    elemsRelatorio.receitaTotal.textContent = "MZN " + fmtRelatorio.format(total);
    elemsRelatorio.receitaNota.textContent = "últimos 6 meses";

    elemsRelatorio.barras.innerHTML = meses.map(mes => `
        <div class="bar-col">
            <span class="bar-value">${fmtRelatorio.format(Number(mes.valor) / 1000)}k</span>
            <div class="bar" style="height:${Math.max(2, Math.round(Number(mes.valor) / maximo * 100))}%"></div>
        </div>`).join("");

    elemsRelatorio.barLabels.innerHTML = meses
        .map(mes => `<span>${escRelatorio(mes.nome)}</span>`).join("");
}

/** Barras horizontais do consumo por categoria. */
function desenharCategoriasRelatorio(relatorio) {
    const temDados = relatorio.categorias.length > 0;
    elemsRelatorio.categorias.hidden = !temDados;
    elemsRelatorio.vazio.hidden = temDados;

    const maximo = Math.max(...relatorio.categorias.map(c => Number(c.consumo)), 1);

    elemsRelatorio.categorias.innerHTML = relatorio.categorias.map(categoria => `
        <div class="barra-h">
            <span>${escRelatorio(categoria.nome)}</span>
            <div class="trilho">
                <div class="preenchimento" style="width:${Math.max(2, Math.round(Number(categoria.consumo) / maximo * 100))}%"></div>
            </div>
            <span class="rotulo-valor">${fmtRelatorio.format(categoria.consumo)} m³ ·
                MZN ${fmtRelatorio.format(categoria.facturado)}</span>
        </div>`).join("");
}

async function gerarRelatorio() {
    mostrarErroRelatorio("");
    elemsRelatorio.btnGerar.disabled = true;
    elemsRelatorio.btnGerar.textContent = "A gerar…";

    try {
        const relatorio = await API.relatorios.gerar(elemsRelatorio.inicio.value, elemsRelatorio.fim.value);

        elemsRelatorio.periodo.textContent =
            `Período de ${formatarData(relatorio.inicio)} a ${formatarData(relatorio.fim)}` +
            ` · ${relatorio.clientesActivos} clientes activos`;

        document.getElementById("clientes-activos").textContent = fmtRelatorio.format(relatorio.clientesActivos);

        desenharIndicadoresRelatorio(relatorio);
        desenharReceitaRelatorio(relatorio);
        desenharCategoriasRelatorio(relatorio);
    } catch (erro) {
        if (erro.message !== "Sessão expirada.") {
            mostrarErroRelatorio("Não foi possível gerar o relatório: " + erro.message);
        }
    } finally {
        elemsRelatorio.btnGerar.disabled = false;
        elemsRelatorio.btnGerar.innerHTML = '<svg class="icon"><use href="#i-chart"/></svg> Gerar';
    }
}

elemsRelatorio.btnGerar.addEventListener("click", gerarRelatorio);

/** Primeira carga (datas vazias = últimos 6 meses, por omissão do backend). */
gerarRelatorio();
