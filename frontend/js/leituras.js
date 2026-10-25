/**
 * leituras.js — página de leituras: lista, pesquisa por mês, registo,
 * edição e eliminação (depende de api.js, app.js, layout.js e pagina.js).
 */

const fmtQuantidade = new Intl.NumberFormat("pt-PT", { maximumFractionDigits: 2 });
const fmtDataCurta = new Intl.DateTimeFormat("pt-PT", { day: "2-digit", month: "short", year: "numeric" });

const elemsLeitura = {
    corpo: document.getElementById("leituras-corpo"),
    tabelaWrap: document.getElementById("tabela-wrap"),
    vazio: document.getElementById("vazio"),
    vazioTitulo: document.getElementById("vazio-titulo"),
    vazioTexto: document.getElementById("vazio-texto"),
    listaTotal: document.getElementById("lista-total"),
    busca: document.getElementById("busca"),
    filtroPeriodo: document.getElementById("filtro-periodo"),
    aviso: document.getElementById("aviso"),
    erroLista: document.getElementById("erro-lista"),
    modal: document.getElementById("modal"),
    form: document.getElementById("form-leitura"),
    modalTitulo: document.getElementById("modal-titulo"),
    modalSub: document.getElementById("modal-sub"),
    modalErro: document.getElementById("modal-erro"),
    btnGuardar: document.getElementById("btn-guardar")
};

let leituras = [];          // última lista devolvida pela API
let clientesDisponiveis = [];   // clientes carregados para o <select>
let termoLeitura = "";

/** Escapa texto do utilizador antes de o inserir no HTML. */
function escLeitura(valor) {
    return String(valor ?? "").replace(/[&<>"']/g, caractere => ({
        "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;"
    })[caractere]);
}

/** Mensagens no topo da lista. */
let temporizadorAvisoLeitura;
function mostrarAvisoLeitura(texto) {
    clearTimeout(temporizadorAvisoLeitura);
    elemsLeitura.aviso.textContent = texto || "";
    elemsLeitura.aviso.hidden = !texto;
    if (texto) {
        temporizadorAvisoLeitura = setTimeout(() => { elemsLeitura.aviso.hidden = true; }, 5000);
    }
}

function mostrarErroLeitura(texto) {
    elemsLeitura.erroLista.textContent = texto || "";
    elemsLeitura.erroLista.hidden = !texto;
}

/** "2026-10" -> "Outubro de 2026" */
function nomePeriodo(periodo) {
    const [ano, mes] = String(periodo).split("-");
    const nomes = ["Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
        "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"];
    return `${nomes[Number(mes) - 1]} de ${ano}`;
}

/** Mês actual no formato do input[type=month]. */
function mesActual() {
    const agora = new Date();
    return `${agora.getFullYear()}-${String(agora.getMonth() + 1).padStart(2, "0")}`;
}

// ---------------------------------------------------------------- Lista

function desenharResumo(resumo) {
    document.getElementById("s-total").textContent = fmtQuantidade.format(resumo.total);
    document.getElementById("s-total-nota").textContent =
        resumo.semLeitura > 0 ? `${resumo.semLeitura} por ler este mês` : "Todos os clientes lidos";

    document.getElementById("s-mes").textContent = fmtQuantidade.format(resumo.esteMes);
    document.getElementById("s-mes-nota").textContent = nomePeriodo(mesActual());

    document.getElementById("s-consumo").textContent = fmtQuantidade.format(resumo.consumoEsteMes) + " m³";
    document.getElementById("s-consumo-nota").textContent = "Medido no mês corrente";

    document.getElementById("s-por-ler").textContent = fmtQuantidade.format(resumo.semLeitura);
    document.getElementById("s-por-ler-nota").textContent =
        resumo.semLeitura > 0 ? "Activos com contador sem leitura" : "Sem pendências";
}

function desenharLista(lista) {
    elemsLeitura.corpo.innerHTML = lista.map(leitura => `
        <tr>
            <td>
                <span class="client">${escLeitura(leitura.cliente)}</span>
                <span class="ref">${escLeitura(leitura.bairro)}</span>
            </td>
            <td>${leitura.numeroContador ? escLeitura(leitura.numeroContador) : '<span class="ref">sem contador</span>'}</td>
            <td>${escLeitura(nomePeriodo(leitura.periodo))}</td>
            <td>${fmtQuantidade.format(leitura.leituraAnterior)} m³</td>
            <td>${fmtQuantidade.format(leitura.leituraActual)} m³</td>
            <td><strong>${fmtQuantidade.format(leitura.consumo)} m³</strong></td>
            <td>${escLeitura(fmtDataCurta.format(new Date(leitura.dataLeitura)))}</td>
            <td>${leitura.facturada
                ? '<span class="pill pill-green">Facturada</span>'
                : '<span class="pill pill-orange">Por facturar</span>'}</td>
            <td>
                <div class="row-actions">
                    <button class="icon-btn" type="button" data-accao="editar" data-id="${leitura.id}"
                            aria-label="Editar leitura de ${escLeitura(leitura.cliente)}" title="Editar">
                        <svg class="icon"><use href="#i-edit"/></svg>
                    </button>
                    <button class="icon-btn danger" type="button" data-accao="eliminar" data-id="${leitura.id}"
                            aria-label="Eliminar leitura de ${escLeitura(leitura.cliente)}" title="Eliminar">
                        <svg class="icon"><use href="#i-trash"/></svg>
                    </button>
                </div>
            </td>
        </tr>`).join("");

    const filtrando = termoLeitura !== "" || elemsLeitura.filtroPeriodo.value !== "";
    const semResultados = lista.length === 0;

    elemsLeitura.tabelaWrap.hidden = semResultados;
    elemsLeitura.vazio.hidden = !semResultados;

    if (semResultados) {
        const procurar = termoLeitura.trim();
        elemsLeitura.vazioTitulo.textContent = procurar
            ? `Nenhuma leitura corresponde a “${procurar}”`
            : (filtrando ? "Nenhuma leitura neste filtro" : "Nenhuma leitura registada");
        elemsLeitura.vazioTexto.textContent = filtrando
            ? "Altere a pesquisa ou o mês seleccionado."
            : "Clique em “Nova leitura” para registar a primeira.";
    }

    elemsLeitura.listaTotal.textContent = semResultados
        ? ""
        : `A mostrar ${fmtQuantidade.format(lista.length)} leitura${lista.length === 1 ? "" : "s"}.`;
}

async function carregarLeituras() {
    mostrarErroLeitura("");
    try {
        const dados = await API.leituras.lista(termoLeitura, elemsLeitura.filtroPeriodo.value);
        leituras = dados.leituras;
        desenharResumo(dados.resumo);
        desenharLista(leituras);
    } catch (erro) {
        if (erro.message !== "Sessão expirada.") {
            mostrarErroLeitura("Não foi possível carregar as leituras: " + erro.message);
        }
    }
}

// ---------------------------------------------------------------- Modal

/** Carrega os clientes para o <select> (uma vez). */
async function carregarClientesParaSelect() {
    if (clientesDisponiveis.length > 0) return;
    const dados = await API.clientes.lista();
    clientesDisponiveis = dados.clientes;

    document.getElementById("f-cliente").innerHTML = clientesDisponiveis.map(cliente =>
        `<option value="${cliente.id}">${escLeitura(cliente.nome)} — ${escLeitura(cliente.bairro)}` +
        `${cliente.numeroContador ? " (" + escLeitura(cliente.numeroContador) + ")" : " (sem contador)"}</option>`
    ).join("");
}

function abrirModalLeitura(leitura) {
    elemsLeitura.form.reset();
    elemsLeitura.modalErro.hidden = true;

    const campoCliente = document.getElementById("f-cliente");
    const hoje = new Date().toISOString().slice(0, 10);

    if (leitura) {
        elemsLeitura.modalTitulo.textContent = "Editar leitura";
        elemsLeitura.modalSub.textContent =
            `${leitura.cliente} · ${nomePeriodo(leitura.periodo)} · consumiu ${fmtQuantidade.format(leitura.consumo)} m³`;
        campoCliente.value = String(leitura.clienteId);
        campoCliente.disabled = true; // não se pode mudar o cliente de uma leitura
        document.getElementById("f-data").value = leitura.dataLeitura.slice(0, 10);
        document.getElementById("f-valor").value = leitura.leituraActual;
        document.getElementById("f-observacao").value = leitura.observacao || "";
    } else {
        elemsLeitura.modalTitulo.textContent = "Nova leitura";
        elemsLeitura.modalSub.textContent = "O consumo é calculado em relação à leitura anterior.";
        campoCliente.disabled = false;
        document.getElementById("f-data").value = hoje;
    }

    elemsLeitura.form.dataset.id = leitura ? leitura.id : "";
    elemsLeitura.modal.hidden = false;
    document.getElementById("f-data").focus();
}

function fecharModalLeitura() {
    elemsLeitura.modal.hidden = true;
    document.getElementById("f-cliente").disabled = false;
}

/** Validação simples no cliente (o backend valida igualmente). */
function validarLeitura() {
    const clienteId = document.getElementById("f-cliente").value;
    const data = document.getElementById("f-data").value;
    const valor = document.getElementById("f-valor").value;

    if (!clienteId) return "Escolha o cliente da leitura.";
    if (!data) return "A data da leitura é obrigatória.";
    if (data > new Date().toISOString().slice(0, 10)) return "A data da leitura não pode estar no futuro.";
    if (valor === "" || Number(valor) < 0) return "Indique o valor do contador (em m³).";
    return null;
}

// ---------------------------------------------------------------- Accções

document.getElementById("btn-novo").addEventListener("click", async () => {
    try {
        await carregarClientesParaSelect();
        abrirModalLeitura(null);
    } catch (erro) {
        if (erro.message !== "Sessão expirada.") {
            mostrarErroLeitura("Não foi possível carregar os clientes: " + erro.message);
        }
    }
});

document.getElementById("btn-fechar").addEventListener("click", fecharModalLeitura);
document.getElementById("btn-cancelar").addEventListener("click", fecharModalLeitura);

elemsLeitura.modal.addEventListener("click", evento => {
    if (evento.target === elemsLeitura.modal) fecharModalLeitura();
});

document.addEventListener("keydown", evento => {
    if (evento.key === "Escape" && !elemsLeitura.modal.hidden) fecharModalLeitura();
});

elemsLeitura.form.addEventListener("submit", async evento => {
    evento.preventDefault();

    const erroValidacao = validarLeitura();
    if (erroValidacao) {
        mostrarMensagem(elemsLeitura.modalErro, erroValidacao, "erro");
        return;
    }

    const id = elemsLeitura.form.dataset.id;
    const dados = {
        clienteId: Number(document.getElementById("f-cliente").value),
        dataLeitura: document.getElementById("f-data").value,
        leituraActual: Number(document.getElementById("f-valor").value),
        observacao: document.getElementById("f-observacao").value.trim() || null
    };

    elemsLeitura.btnGuardar.disabled = true;
    elemsLeitura.btnGuardar.textContent = "A guardar…";

    try {
        if (id) {
            await API.leituras.actualizar(id, dados);
            mostrarAvisoLeitura("Leitura actualizada; o consumo foi recalculado.");
        } else {
            await API.leituras.criar(dados);
            mostrarAvisoLeitura("Leitura registada com sucesso.");
        }
        fecharModalLeitura();
        await carregarLeituras();
    } catch (erro) {
        mostrarMensagem(elemsLeitura.modalErro, erro.message, "erro");
    } finally {
        elemsLeitura.btnGuardar.disabled = false;
        elemsLeitura.btnGuardar.textContent = "Guardar leitura";
    }
});

/** Editar / eliminar (delegado nos botões de cada linha). */
elemsLeitura.corpo.addEventListener("click", async evento => {
    const botao = evento.target.closest("button[data-accao]");
    if (!botao) return;

    const leitura = leituras.find(l => String(l.id) === botao.dataset.id);
    if (!leitura) return;

    if (botao.dataset.accao === "editar") {
        if (leitura.facturada) {
            mostrarErroLeitura("A leitura de " + leitura.cliente + " já foi facturada e não pode ser editada.");
            return;
        }
        abrirModalLeitura(leitura);
        return;
    }

    const confirmacao = window.confirm(
        `Eliminar a leitura de ${leitura.cliente} (${nomePeriodo(leitura.periodo)})?\n\n` +
        "O consumo de " + fmtQuantidade.format(leitura.consumo) + " m³ deixará de contar.");
    if (!confirmacao) return;

    try {
        await API.leituras.remover(leitura.id);
        mostrarAvisoLeitura("Leitura eliminada.");
        await carregarLeituras();
    } catch (erro) {
        if (erro.message !== "Sessão expirada.") {
            mostrarErroLeitura("Não foi possível eliminar a leitura: " + erro.message);
        }
    }
});

/** Pesquisa com pequena pausa; o mês é aplicado de imediato. */
let temporizadorBuscaLeitura;
elemsLeitura.busca.addEventListener("input", () => {
    clearTimeout(temporizadorBuscaLeitura);
    temporizadorBuscaLeitura = setTimeout(() => {
        termoLeitura = elemsLeitura.busca.value.trim();
        carregarLeituras();
    }, 300);
});

elemsLeitura.filtroPeriodo.addEventListener("change", () => carregarLeituras());

/** Primeira carga. */
carregarLeituras();
