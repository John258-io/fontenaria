/**
 * pagamentos.js — página de pagamentos: lista, registo contra facturas
 * pendentes e estorno de recibos (depende de api.js, app.js, layout.js e
 * pagina.js).
 *
 * Aceita ?factura=ID para abrir o modal já com a factura escolhida (vem do
 * detalhe da factura em facturas.html).
 */

const fmtMontante = new Intl.NumberFormat("pt-PT", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
const fmtQuantidadePag = new Intl.NumberFormat("pt-PT");
const fmtDataPag = new Intl.DateTimeFormat("pt-PT", {
    day: "2-digit", month: "short", year: "numeric", hour: "2-digit", minute: "2-digit"
});

const elemsPagamento = {
    corpo: document.getElementById("pagamentos-corpo"),
    tabelaWrap: document.getElementById("tabela-wrap"),
    vazio: document.getElementById("vazio"),
    vazioTitulo: document.getElementById("vazio-titulo"),
    vazioTexto: document.getElementById("vazio-texto"),
    listaTotal: document.getElementById("lista-total"),
    busca: document.getElementById("busca"),
    filtroMetodo: document.getElementById("filtro-metodo"),
    aviso: document.getElementById("aviso"),
    erroLista: document.getElementById("erro-lista"),
    modal: document.getElementById("modal"),
    form: document.getElementById("form-pagamento"),
    modalSub: document.getElementById("modal-sub"),
    modalErro: document.getElementById("modal-erro"),
    btnGuardar: document.getElementById("btn-guardar")
};

let pagamentos = [];
let pendentes = [];      // facturas pendentes devolvidas pela API
let termoPagamento = "";

/** Escapa texto do utilizador antes de o inserir no HTML. */
function escPagamento(valor) {
    return String(valor ?? "").replace(/[&<>"']/g, caractere => ({
        "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;"
    })[caractere]);
}

/** "860" -> "860,00 MZN" */
function moedaPagamento(valor) {
    return fmtMontante.format(Number(valor)) + " MZN";
}

let temporizadorAvisoPagamento;
function mostrarAvisoPagamento(texto) {
    clearTimeout(temporizadorAvisoPagamento);
    elemsPagamento.aviso.textContent = texto || "";
    elemsPagamento.aviso.hidden = !texto;
    if (texto) {
        temporizadorAvisoPagamento = setTimeout(() => { elemsPagamento.aviso.hidden = true; }, 5000);
    }
}

function mostrarErroPagamento(texto) {
    elemsPagamento.erroLista.textContent = texto || "";
    elemsPagamento.erroLista.hidden = !texto;
}

// ---------------------------------------------------------------- Lista

function desenharResumoPagamento(resumo) {
    document.getElementById("s-mes").textContent = moedaPagamento(resumo.recebidoEsteMes);
    document.getElementById("s-mes-nota").textContent =
        resumo.esteMes > 0 ? resumo.esteMes + " pagamento(s) no mês" : "Ainda sem recebimentos";

    document.getElementById("s-total").textContent = moedaPagamento(resumo.recebidoTotal);
    document.getElementById("s-total-nota").textContent = "Desde o início da operação";

    document.getElementById("s-quantidade").textContent = fmtQuantidadePag.format(resumo.total);
    document.getElementById("s-quantidade-nota").textContent = "Recibos emitidos";

    document.getElementById("s-por-receber").textContent = moedaPagamento(resumo.porReceber);
    document.getElementById("s-por-receber-nota").textContent =
        resumo.porReceber > 0 ? "Saldo das facturas pendentes" : "Tudo liquidado";
}

function desenharListaPagamento(lista) {
    elemsPagamento.corpo.innerHTML = lista.map(pagamento => `
        <tr>
            <td>
                <span class="client">${escPagamento(pagamento.codigo)}</span>
                <span class="ref">${escPagamento(fmtDataPag.format(new Date(pagamento.dataPagamento)))}</span>
            </td>
            <td>${escPagamento(pagamento.cliente)}</td>
            <td>
                <a class="link-right" href="facturas.html?busca=${encodeURIComponent(pagamento.facturaCodigo)}">
                    ${escPagamento(pagamento.facturaCodigo)}
                </a>
            </td>
            <td class="money-in">+ ${moedaPagamento(pagamento.valor)}</td>
            <td><span class="pill pill-gray">${escPagamento(pagamento.metodo)}</span></td>
            <td>${escPagamento(pagamento.recebidoPor)}</td>
            <td>${escPagamento(pagamento.observacao || "—")}</td>
            <td>
                <div class="row-actions">
                    <button class="icon-btn danger" type="button" data-accao="estornar" data-id="${pagamento.id}"
                            aria-label="Anular ${escPagamento(pagamento.codigo)}" title="Anular recibo">
                        <svg class="icon"><use href="#i-trash"/></svg>
                    </button>
                </div>
            </td>
        </tr>`).join("");

    const filtrando = termoPagamento !== "" || elemsPagamento.filtroMetodo.value !== "";
    const semResultados = lista.length === 0;

    elemsPagamento.tabelaWrap.hidden = semResultados;
    elemsPagamento.vazio.hidden = !semResultados;

    if (semResultados) {
        const procurar = termoPagamento.trim();
        elemsPagamento.vazioTitulo.textContent = procurar
            ? `Nenhum pagamento corresponde a “${procurar}”`
            : (filtrando ? "Nenhum pagamento neste filtro" : "Nenhum pagamento registado");
        elemsPagamento.vazioTexto.textContent = filtrando
            ? "Altere a pesquisa ou o método seleccionado."
            : "Clique em “Registar pagamento” para receber a primeira factura.";
    }

    elemsPagamento.listaTotal.textContent = semResultados
        ? ""
        : `A mostrar ${fmtQuantidadePag.format(lista.length)} pagamento${lista.length === 1 ? "" : "s"}.`;
}

async function carregarPagamentos() {
    mostrarErroPagamento("");
    try {
        const dados = await API.pagamentos.lista(termoPagamento, elemsPagamento.filtroMetodo.value);
        pagamentos = dados.pagamentos;
        pendentes = dados.pendentes;
        desenharResumoPagamento(dados.resumo);
        desenharListaPagamento(pagamentos);
        return dados;
    } catch (erro) {
        if (erro.message !== "Sessão expirada.") {
            mostrarErroPagamento("Não foi possível carregar os pagamentos: " + erro.message);
        }
        return null;
    }
}

// ---------------------------------------------------------------- Modal

function preencherFacturasPendentes(seleccionada) {
    const select = document.getElementById("f-factura");

    if (pendentes.length === 0) {
        select.innerHTML = '<option value="">Nenhuma factura pendente</option>';
        select.disabled = true;
        return false;
    }

    select.disabled = false;
    select.innerHTML = pendentes.map(factura =>
        `<option value="${factura.id}">${escPagamento(factura.codigo)} — ${escPagamento(factura.cliente)} · ` +
        `dívida ${fmtMontante.format(Number(factura.saldo))} MZN` +
        `${factura.emAtraso ? " (em atraso)" : ""}</option>`
    ).join("");

    if (seleccionada && pendentes.some(f => String(f.id) === String(seleccionada))) {
        select.value = String(seleccionada);
    }
    return true;
}

/** Preenche o campo valor com o saldo da factura escolhida. */
function actualizarValorSugerido() {
    const factura = pendentes.find(f => String(f.id) === document.getElementById("f-factura").value);
    if (factura) {
        document.getElementById("f-valor").value = Number(factura.saldo).toFixed(2);
    }
}

async function abrirModalPagamento(facturaId) {
    elemsPagamento.form.reset();
    elemsPagamento.modalErro.hidden = true;

    if (pendentes.length === 0) {
        const dados = await carregarPagamentos();
        if (!dados) return;
    }

    if (!preencherFacturasPendentes(facturaId)) {
        mostrarErroPagamento("Não há facturas pendentes para receber. Gere as facturas do mês primeiro.");
        return;
    }

    elemsPagamento.modalSub.textContent = "O pagamento é lançado contra uma factura pendente.";
    elemsPagamento.modal.hidden = false;
    actualizarValorSugerido();
    document.getElementById("f-valor").focus();
}

function fecharModalPagamento() {
    elemsPagamento.modal.hidden = true;
}

/** Validação simples no cliente (o backend valida igualmente). */
function validarPagamento() {
    const facturaId = document.getElementById("f-factura").value;
    const valor = document.getElementById("f-valor").value;

    if (!facturaId) return "Escolha a factura a pagar.";
    if (valor === "" || Number(valor) <= 0) return "O valor recebido deve ser maior que zero.";

    const factura = pendentes.find(f => String(f.id) === facturaId);
    if (factura && Number(valor) > Number(factura.saldo)) {
        return "O valor excede o saldo em dívida (" + moedaPagamento(factura.saldo) + ").";
    }
    return null;
}

// ---------------------------------------------------------------- Accções

document.getElementById("btn-novo").addEventListener("click", () => abrirModalPagamento(null));
document.getElementById("btn-fechar").addEventListener("click", fecharModalPagamento);
document.getElementById("btn-cancelar").addEventListener("click", fecharModalPagamento);

elemsPagamento.modal.addEventListener("click", evento => {
    if (evento.target === elemsPagamento.modal) fecharModalPagamento();
});

document.getElementById("f-factura").addEventListener("change", actualizarValorSugerido);

document.addEventListener("keydown", evento => {
    if (evento.key === "Escape" && !elemsPagamento.modal.hidden) fecharModalPagamento();
});

elemsPagamento.form.addEventListener("submit", async evento => {
    evento.preventDefault();

    const erroValidacao = validarPagamento();
    if (erroValidacao) {
        mostrarMensagem(elemsPagamento.modalErro, erroValidacao, "erro");
        return;
    }

    const dados = {
        facturaId: Number(document.getElementById("f-factura").value),
        valor: Number(document.getElementById("f-valor").value),
        metodo: document.getElementById("f-metodo").value,
        observacao: document.getElementById("f-observacao").value.trim() || null
    };

    elemsPagamento.btnGuardar.disabled = true;
    elemsPagamento.btnGuardar.textContent = "A registar…";

    try {
        const pagamento = await API.pagamentos.registar(dados);
        fecharModalPagamento();
        mostrarAvisoPagamento(
            "Pagamento " + pagamento.codigo + " registado: " + moedaPagamento(pagamento.valor) + ".");
        pendentes = []; // força a recarga das facturas pendentes
        await carregarPagamentos();
    } catch (erro) {
        mostrarMensagem(elemsPagamento.modalErro, erro.message, "erro");
    } finally {
        elemsPagamento.btnGuardar.disabled = false;
        elemsPagamento.btnGuardar.textContent = "Registar pagamento";
    }
});

/** Estorno (delegado no botão de cada linha). */
elemsPagamento.corpo.addEventListener("click", async evento => {
    const botao = evento.target.closest("button[data-accao='estornar']");
    if (!botao) return;

    const pagamento = pagamentos.find(p => String(p.id) === botao.dataset.id);
    if (!pagamento) return;

    const confirmacao = window.confirm(
        `Anular o pagamento ${pagamento.codigo} (${moedaPagamento(pagamento.valor)})?\n\n` +
        "O valor volta a ficar em dívida na factura " + pagamento.facturaCodigo + ".");
    if (!confirmacao) return;

    try {
        await API.pagamentos.remover(pagamento.id);
        mostrarAvisoPagamento("Pagamento " + pagamento.codigo + " anulado.");
        pendentes = [];
        await carregarPagamentos();
    } catch (erro) {
        if (erro.message !== "Sessão expirada.") {
            mostrarErroPagamento("Não foi possível anular o pagamento: " + erro.message);
        }
    }
});

// ---------------------------------------------------------------- Filtros

let temporizadorBuscaPagamento;
elemsPagamento.busca.addEventListener("input", () => {
    clearTimeout(temporizadorBuscaPagamento);
    temporizadorBuscaPagamento = setTimeout(() => {
        termoPagamento = elemsPagamento.busca.value.trim();
        carregarPagamentos();
    }, 300);
});

elemsPagamento.filtroMetodo.addEventListener("change", () => carregarPagamentos());

/** Primeira carga (e abertura direta vinda do detalhe da factura). */
(async () => {
    await carregarPagamentos();

    const facturaPretendida = new URLSearchParams(window.location.search).get("factura");
    if (facturaPretendida) {
        await abrirModalPagamento(facturaPretendida);
    }
})();
