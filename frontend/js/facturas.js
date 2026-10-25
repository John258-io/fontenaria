/**
 * facturas.js — página de facturação: lista, emissão a partir de leituras,
 * detalhe com pagamentos e eliminação (depende de api.js, app.js, layout.js
 * e pagina.js).
 */

const fmtValor = new Intl.NumberFormat("pt-PT", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
const fmtQuantidadeFact = new Intl.NumberFormat("pt-PT", { maximumFractionDigits: 2 });
const fmtDataFact = new Intl.DateTimeFormat("pt-PT", { day: "2-digit", month: "short", year: "numeric" });

const elemsFactura = {
    corpo: document.getElementById("facturas-corpo"),
    tabelaWrap: document.getElementById("tabela-wrap"),
    vazio: document.getElementById("vazio"),
    vazioTitulo: document.getElementById("vazio-titulo"),
    vazioTexto: document.getElementById("vazio-texto"),
    listaTotal: document.getElementById("lista-total"),
    busca: document.getElementById("busca"),
    filtroEstado: document.getElementById("filtro-estado"),
    filtroPeriodo: document.getElementById("filtro-periodo"),
    aviso: document.getElementById("aviso"),
    erroLista: document.getElementById("erro-lista"),
    modalNova: document.getElementById("modal-nova"),
    modalDetalhe: document.getElementById("modal-detalhe"),
    novaErro: document.getElementById("nova-erro"),
    detalheTitulo: document.getElementById("detalhe-titulo"),
    detalheSub: document.getElementById("detalhe-sub"),
    detalheLista: document.getElementById("detalhe-lista"),
    detalhePagamentos: document.getElementById("detalhe-pagamentos"),
    btnEliminar: document.getElementById("btn-eliminar-factura"),
    btnPagar: document.getElementById("btn-pagar-factura")
};

let facturas = [];
let termoFactura = "";

/** Escapa texto do utilizador antes de o inserir no HTML. */
function escFactura(valor) {
    return String(valor ?? "").replace(/[&<>"']/g, caractere => ({
        "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;"
    })[caractere]);
}

/** "860" -> "860,00 MZN" */
function moedaFactura(valor) {
    return fmtValor.format(Number(valor)) + " MZN";
}

let temporizadorAvisoFactura;
function mostrarAvisoFactura(texto) {
    clearTimeout(temporizadorAvisoFactura);
    elemsFactura.aviso.textContent = texto || "";
    elemsFactura.aviso.hidden = !texto;
    if (texto) {
        temporizadorAvisoFactura = setTimeout(() => { elemsFactura.aviso.hidden = true; }, 5000);
    }
}

function mostrarErroFactura(texto) {
    elemsFactura.erroLista.textContent = texto || "";
    elemsFactura.erroLista.hidden = !texto;
}

/** "2026-10" -> "Outubro de 2026" */
function nomePeriodoFactura(periodo) {
    const [ano, mes] = String(periodo).split("-");
    const nomes = ["Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
        "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"];
    return `${nomes[Number(mes) - 1]} de ${ano}`;
}

function mesActualFactura() {
    const agora = new Date();
    return `${agora.getFullYear()}-${String(agora.getMonth() + 1).padStart(2, "0")}`;
}

// ---------------------------------------------------------------- Lista

function desenharResumoFactura(resumo) {
    document.getElementById("s-total").textContent = fmtQuantidadeFact.format(resumo.total);
    document.getElementById("s-total-nota").textContent =
        "Total facturado: " + moedaFactura(resumo.faturado);

    document.getElementById("s-pendentes").textContent = fmtQuantidadeFact.format(resumo.pendentes);
    document.getElementById("s-pendentes-nota").textContent =
        resumo.pendentes > 0 ? "A aguardar pagamento" : "Nenhuma pendência";

    document.getElementById("s-pagas").textContent = fmtQuantidadeFact.format(resumo.pagas);
    document.getElementById("s-pagas-nota").textContent =
        resumo.total > 0 ? Math.round(resumo.pagas / resumo.total * 100) + "% das facturas" : "—";

    document.getElementById("s-saldo").textContent = moedaFactura(resumo.saldoPorReceber);
    document.getElementById("s-saldo-nota").textContent =
        resumo.saldoPorReceber > 0 ? "A receber nas facturas pendentes" : "Tudo liquidado";
}

function desenharListaFactura(lista) {
    elemsFactura.corpo.innerHTML = lista.map(factura => `
        <tr>
            <td>
                <span class="client">${escFactura(factura.codigo)}</span>
                <span class="ref">${escFactura(nomePeriodoFactura(factura.periodo))}</span>
            </td>
            <td>
                <span class="client">${escFactura(factura.cliente)}</span>
                <span class="ref">${escFactura(factura.numeroContador || factura.bairro)}</span>
            </td>
            <td>${fmtQuantidadeFact.format(factura.consumo)} m³ × ${fmtValor.format(Number(factura.tarifa))}</td>
            <td>${moedaFactura(factura.valor)}</td>
            <td class="${Number(factura.valorPago) > 0 ? "money-in" : ""}">${moedaFactura(factura.valorPago)}</td>
            <td>${Number(factura.saldo) > 0
                ? '<span class="saldo-devendo">' + moedaFactura(factura.saldo) + "</span>"
                : "—"}</td>
            <td>${escFactura(fmtDataFact.format(new Date(factura.vencimento)))}</td>
            <td>
                <span class="pill ${factura.estado === "Paga" ? "pill-green" : "pill-orange"}">${escFactura(factura.estado)}</span>
                ${factura.emAtraso ? ' <span class="pill pill-red">Em atraso</span>' : ""}
            </td>
            <td>
                <div class="row-actions">
                    <button class="icon-btn" type="button" data-accao="detalhe" data-id="${factura.id}"
                            aria-label="Ver ${escFactura(factura.codigo)}" title="Ver detalhe">
                        <svg class="icon"><use href="#i-info"/></svg>
                    </button>
                    <button class="icon-btn danger" type="button" data-accao="eliminar" data-id="${factura.id}"
                            aria-label="Eliminar ${escFactura(factura.codigo)}" title="Eliminar">
                        <svg class="icon"><use href="#i-trash"/></svg>
                    </button>
                </div>
            </td>
        </tr>`).join("");

    const filtrando = termoFactura !== "" || elemsFactura.filtroEstado.value !== ""
        || elemsFactura.filtroPeriodo.value !== "";
    const semResultados = lista.length === 0;

    elemsFactura.tabelaWrap.hidden = semResultados;
    elemsFactura.vazio.hidden = !semResultados;

    if (semResultados) {
        const procurar = termoFactura.trim();
        elemsFactura.vazioTitulo.textContent = procurar
            ? `Nenhuma factura corresponde a “${procurar}”`
            : (filtrando ? "Nenhuma factura neste filtro" : "Nenhuma factura emitida");
        elemsFactura.vazioTexto.textContent = filtrando
            ? "Altere a pesquisa, o estado ou o mês seleccionado."
            : "Use “Nova factura” ou “Gerar do mês” para emitir a partir das leituras.";
    }

    elemsFactura.listaTotal.textContent = semResultados
        ? ""
        : `A mostrar ${fmtQuantidadeFact.format(lista.length)} factura${lista.length === 1 ? "" : "s"}.`;
}

async function carregarFacturas() {
    mostrarErroFactura("");
    try {
        const dados = await API.facturas.lista(
            termoFactura, elemsFactura.filtroEstado.value, elemsFactura.filtroPeriodo.value);
        facturas = dados.facturas;
        desenharResumoFactura(dados.resumo);
        desenharListaFactura(facturas);
    } catch (erro) {
        if (erro.message !== "Sessão expirada.") {
            mostrarErroFactura("Não foi possível carregar as facturas: " + erro.message);
        }
    }
}

// ---------------------------------------------------------------- Emissão

/** Emite todas as facturas em falta do mês do filtro (ou do mês actual). */
document.getElementById("btn-gerar-mes").addEventListener("click", async evento => {
    const botao = evento.currentTarget;
    const periodo = elemsFactura.filtroPeriodo.value || mesActualFactura();

    botao.disabled = true;
    botao.textContent = "A gerar…";

    try {
        const resposta = await API.facturas.gerarMes(periodo);
        mostrarAvisoFactura(resposta.mensagem + " (" + nomePeriodoFactura(periodo) + ")");
        await carregarFacturas();
    } catch (erro) {
        if (erro.message !== "Sessão expirada.") {
            mostrarErroFactura("Não foi possível gerar as facturas: " + erro.message);
        }
    } finally {
        botao.disabled = false;
        botao.innerHTML = '<svg class="icon"><use href="#i-calendar"/></svg> Gerar do mês';
    }
});

/** Modal "Nova factura": lista as leituras ainda por facturar. */
document.getElementById("btn-nova").addEventListener("click", async () => {
    elemsFactura.novaErro.hidden = true;
    try {
        const dados = await API.leituras.lista("", elemsFactura.filtroPeriodo.value);
        const porFacturar = dados.leituras.filter(leitura => !leitura.facturada);

        const select = document.getElementById("f-leitura");
        if (porFacturar.length === 0) {
            mostrarErroFactura("Não há leituras por facturar"
                + (elemsFactura.filtroPeriodo.value
                    ? " em " + nomePeriodoFactura(elemsFactura.filtroPeriodo.value) + "."
                    : "."));
            return;
        }

        select.innerHTML = porFacturar.map(leitura =>
            `<option value="${leitura.id}">${escFactura(leitura.cliente)} — ` +
            `${escFactura(nomePeriodoFactura(leitura.periodo))} · ` +
            `${fmtQuantidadeFact.format(leitura.consumo)} m³</option>`
        ).join("");

        elemsFactura.modalNova.hidden = false;
    } catch (erro) {
        if (erro.message !== "Sessão expirada.") {
            mostrarErroFactura("Não foi possível carregar as leituras: " + erro.message);
        }
    }
});

function fecharModalNova() {
    elemsFactura.modalNova.hidden = true;
}

document.getElementById("btn-fechar-nova").addEventListener("click", fecharModalNova);
document.getElementById("btn-cancelar-nova").addEventListener("click", fecharModalNova);

document.getElementById("form-nova").addEventListener("submit", async evento => {
    evento.preventDefault();

    const botao = document.getElementById("btn-emitir");
    const leituraId = Number(document.getElementById("f-leitura").value);

    botao.disabled = true;
    botao.textContent = "A emitir…";

    try {
        const factura = await API.facturas.gerar(leituraId);
        fecharModalNova();
        mostrarAvisoFactura("Factura " + factura.codigo + " emitida: " + moedaFactura(factura.valor) + ".");
        await carregarFacturas();
    } catch (erro) {
        mostrarMensagem(elemsFactura.novaErro, erro.message, "erro");
    } finally {
        botao.disabled = false;
        botao.textContent = "Emitir factura";
    }
});

// ---------------------------------------------------------------- Detalhe

function linhaDetalhe(rotulo, valor) {
    return `<div><span class="rotulo">${escFactura(rotulo)}</span><strong class="valor">${valor}</strong></div>`;
}

function abrirDetalhe(factura) {
    elemsFactura.detalheTitulo.textContent = "Factura " + factura.codigo;
    elemsFactura.detalheSub.textContent =
        factura.cliente + " · " + nomePeriodoFactura(factura.periodo) + " · " +
        (factura.emAtraso ? "em atraso" : factura.estado.toLowerCase());

    elemsFactura.detalheLista.innerHTML =
        linhaDetalhe("Cliente", escFactura(factura.cliente)) +
        linhaDetalhe("Bairro / contador",
            escFactura(factura.bairro) + (factura.numeroContador ? " · " + escFactura(factura.numeroContador) : "")) +
        linhaDetalhe("Período", escFactura(nomePeriodoFactura(factura.periodo))) +
        linhaDetalhe("Consumo", fmtQuantidadeFact.format(factura.consumo) + " m³") +
        linhaDetalhe("Tarifa aplicada", moedaFactura(factura.tarifa) + " / m³") +
        linhaDetalhe("Valor da factura", moedaFactura(factura.valor)) +
        linhaDetalhe("Já pago", moedaFactura(factura.valorPago)) +
        linhaDetalhe("Saldo", moedaFactura(factura.saldo)) +
        linhaDetalhe("Vencimento", escFactura(fmtDataFact.format(new Date(factura.vencimento))));

    elemsFactura.btnEliminar.dataset.id = factura.id;
    elemsFactura.btnEliminar.disabled = Number(factura.valorPago) > 0;
    elemsFactura.btnEliminar.title = Number(factura.valorPago) > 0
        ? "Factura com pagamentos: anule-os primeiro"
        : "Eliminar factura";
    elemsFactura.btnPagar.href = "pagamentos.html?factura=" + factura.id;
    elemsFactura.btnPagar.hidden = factura.estado === "Paga";

    elemsFactura.modalDetalhe.hidden = false;

    // Pagamentos da factura
    API.facturas.pagamentos(factura.id).then(pagamentos => {
        elemsFactura.detalhePagamentos.innerHTML = pagamentos.length === 0
            ? '<div><span class="rotulo">Ainda sem pagamentos</span></div>'
            : pagamentos.map(pagamento => `
                <div>
                    <span class="rotulo">${escFactura(pagamento.codigo)} · ${escFactura(pagamento.metodo)} · ` +
                    `${escFactura(fmtDataFact.format(new Date(pagamento.dataPagamento)))}</span>
                    <strong class="valor money-in">${moedaFactura(pagamento.valor)}</strong>
                </div>`).join("");
    }).catch(erro => {
        if (erro.message !== "Sessão expirada.") {
            elemsFactura.detalhePagamentos.innerHTML =
                '<div><span class="rotulo">Não foi possível carregar os pagamentos</span></div>';
        }
    });
}

function fecharModalDetalhe() {
    elemsFactura.modalDetalhe.hidden = true;
}

document.getElementById("btn-fechar-detalhe").addEventListener("click", fecharModalDetalhe);

elemsFactura.btnEliminar.addEventListener("click", async () => {
    const id = elemsFactura.btnEliminar.dataset.id;
    const factura = facturas.find(f => String(f.id) === id);
    if (!factura) return;

    const confirmacao = window.confirm(
        `Eliminar a factura ${factura.codigo} de ${factura.cliente}?\n\nEsta acção não pode ser desfeita.`);
    if (!confirmacao) return;

    try {
        await API.facturas.remover(id);
        fecharModalDetalhe();
        mostrarAvisoFactura("Factura " + factura.codigo + " eliminada.");
        await carregarFacturas();
    } catch (erro) {
        if (erro.message !== "Sessão expirada.") {
            mostrarErroFactura("Não foi possível eliminar a factura: " + erro.message);
        }
    }
});

/** Ver detalhe / eliminar (delegado nos botões de cada linha). */
elemsFactura.corpo.addEventListener("click", evento => {
    const botao = evento.target.closest("button[data-accao]");
    if (!botao) return;

    const factura = facturas.find(f => String(f.id) === botao.dataset.id);
    if (!factura) return;

    if (botao.dataset.accao === "detalhe") {
        abrirDetalhe(factura);
        return;
    }

    if (Number(factura.valorPago) > 0) {
        mostrarErroFactura("A factura " + factura.codigo + " já tem pagamentos; anule-os primeiro.");
        return;
    }

    const confirmacao = window.confirm(
        `Eliminar a factura ${factura.codigo} de ${factura.cliente}?\n\nEsta acção não pode ser desfeita.`);
    if (!confirmacao) return;

    API.facturas.remover(factura.id)
        .then(() => {
            mostrarAvisoFactura("Factura " + factura.codigo + " eliminada.");
            return carregarFacturas();
        })
        .catch(erro => {
            if (erro.message !== "Sessão expirada.") {
                mostrarErroFactura("Não foi possível eliminar a factura: " + erro.message);
            }
        });
});

// ---------------------------------------------------------------- Filtros

let temporizadorBuscaFactura;
elemsFactura.busca.addEventListener("input", () => {
    clearTimeout(temporizadorBuscaFactura);
    temporizadorBuscaFactura = setTimeout(() => {
        termoFactura = elemsFactura.busca.value.trim();
        carregarFacturas();
    }, 300);
});

elemsFactura.filtroEstado.addEventListener("change", () => carregarFacturas());
elemsFactura.filtroPeriodo.addEventListener("change", () => carregarFacturas());

/** Modais fecham com Escape. */
document.addEventListener("keydown", evento => {
    if (evento.key !== "Escape") return;
    if (!elemsFactura.modalNova.hidden) fecharModalNova();
    if (!elemsFactura.modalDetalhe.hidden) fecharModalDetalhe();
});

/** Primeira carga. */
carregarFacturas();
