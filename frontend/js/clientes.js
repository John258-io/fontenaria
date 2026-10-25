/**
 * clientes.js — página de clientes: lista, pesquisa, criação, edição e
 * eliminação (depende de api.js, app.js, layout.js e pagina.js).
 */

const fmtNumero = new Intl.NumberFormat("pt-PT");
const fmtData = new Intl.DateTimeFormat("pt-PT", { day: "2-digit", month: "short", year: "numeric" });

const elems = {
    corpo: document.getElementById("clientes-corpo"),
    tabelaWrap: document.getElementById("tabela-wrap"),
    vazio: document.getElementById("vazio"),
    vazioTitulo: document.getElementById("vazio-titulo"),
    vazioTexto: document.getElementById("vazio-texto"),
    listaTotal: document.getElementById("lista-total"),
    busca: document.getElementById("busca"),
    filtroEstado: document.getElementById("filtro-estado"),
    aviso: document.getElementById("aviso"),
    erroLista: document.getElementById("erro-lista"),
    modal: document.getElementById("modal"),
    form: document.getElementById("form-cliente"),
    modalTitulo: document.getElementById("modal-titulo"),
    modalSub: document.getElementById("modal-sub"),
    modalErro: document.getElementById("modal-erro"),
    btnGuardar: document.getElementById("btn-guardar"),
    badge: document.getElementById("badge-clientes")
};

let clientes = [];          // última lista devolvida pela API
let termoActual = "";       // pesquisa em vigor (para recarregar após guardar)

/** Escapa texto do utilizador antes de o inserir no HTML. */
function esc(valor) {
    return String(valor ?? "").replace(/[&<>"']/g, caractere => ({
        "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;"
    })[caractere]);
}

/** "841234567" → "84 123 4567" (formato visual). */
function formatarTelefone(digitos) {
    const d = String(digitos || "").replace(/\D/g, "");
    return d.length === 9 ? `${d.slice(0, 2)} ${d.slice(2, 5)} ${d.slice(5)}` : digitos;
}

/** Mostra/oculta mensagens da página (aviso de sucesso ou erro). */
let temporizadorAviso;
function mostrarAviso(texto) {
    clearTimeout(temporizadorAviso);
    elems.aviso.textContent = texto || "";
    elems.aviso.hidden = !texto;
    if (texto) {
        temporizadorAviso = setTimeout(() => { elems.aviso.hidden = true; }, 5000);
    }
}

function mostrarErroLista(texto) {
    elems.erroLista.textContent = texto || "";
    elems.erroLista.hidden = !texto;
}

// ---------------------------------------------------------------- Lista

/** Cartões de indicadores do topo. */
function desenharResumo(resumo) {
    document.getElementById("s-total").textContent = fmtNumero.format(resumo.total);
    document.getElementById("s-total-nota").textContent =
        resumo.novosEsteMes > 0 ? `+${resumo.novosEsteMes} este mês` : "Sem novidades este mês";

    document.getElementById("s-activos").textContent = fmtNumero.format(resumo.activos);
    document.getElementById("s-activos-nota").textContent =
        resumo.total ? `${Math.round(resumo.activos / resumo.total * 100)}% do total` : "—";

    document.getElementById("s-suspensos").textContent = fmtNumero.format(resumo.suspensos);
    document.getElementById("s-suspensos-nota").textContent =
        resumo.incompletos > 0
            ? `${resumo.incompletos} cadastro${resumo.incompletos === 1 ? "" : "s"} incompleto${resumo.incompletos === 1 ? "" : "s"}`
            : "Todos com contador";

    document.getElementById("s-novos").textContent = fmtNumero.format(resumo.novosEsteMes);

    if (elems.badge) {
        elems.badge.textContent = fmtNumero.format(resumo.total);
        elems.badge.hidden = false;
    }
}

/** Tabela de clientes (aplica o filtro de estado no cliente). */
function desenharLista(lista) {
    const filtro = elems.filtroEstado.value;
    const visiveis = filtro ? lista.filter(c => c.estado === filtro) : lista;

    elems.corpo.innerHTML = visiveis.map(cliente => `
        <tr>
            <td>
                <span class="client">${esc(cliente.nome)}</span>
                <span class="ref">${esc(formatarTelefone(cliente.telefone))}</span>
            </td>
            <td>${esc(cliente.bairro)}</td>
            <td>${cliente.numeroContador ? esc(cliente.numeroContador) : '<span class="ref">por completar</span>'}</td>
            <td>${esc(cliente.categoria)}</td>
            <td><span class="pill ${cliente.estado === "Activo" ? "pill-green" : "pill-orange"}">${esc(cliente.estado)}</span></td>
            <td>${esc(fmtData.format(new Date(cliente.dataRegisto)))}</td>
            <td>
                <div class="row-actions">
                    <button class="icon-btn" type="button" data-accao="editar" data-id="${cliente.id}"
                            aria-label="Editar ${esc(cliente.nome)}" title="Editar">
                        <svg class="icon"><use href="#i-edit"/></svg>
                    </button>
                    <button class="icon-btn danger" type="button" data-accao="eliminar" data-id="${cliente.id}"
                            aria-label="Eliminar ${esc(cliente.nome)}" title="Eliminar">
                        <svg class="icon"><use href="#i-trash"/></svg>
                    </button>
                </div>
            </td>
        </tr>`).join("");

    const filtrando = termoActual !== "" || filtro !== "";
    const semResultados = visiveis.length === 0;

    elems.tabelaWrap.hidden = semResultados;
    elems.vazio.hidden = !semResultados;

    if (semResultados) {
        const procurar = termoActual.trim();
        elems.vazioTitulo.textContent = procurar ? `Nenhum cliente corresponde a “${procurar}”` : "Nenhum cliente neste filtro";
        elems.vazioTexto.textContent = procurar
            ? "Verifique a grafia ou limpe a pesquisa."
            : "Ajuste o filtro de estado para ver outros clientes.";
    }

    elems.listaTotal.textContent = semResultados
        ? ""
        : `A mostrar ${fmtNumero.format(visiveis.length)} de ${fmtNumero.format(lista.length)} cliente${lista.length === 1 ? "" : "s"}.`;
}

/** Carrega a lista (e as contagens) da API. */
async function carregar() {
    mostrarErroLista("");
    try {
        const dados = await API.clientes.lista(termoActual);
        clientes = dados.clientes;
        desenharResumo(dados.resumo);
        desenharLista(clientes);
    } catch (erro) {
        if (erro.message !== "Sessão expirada.") {
            mostrarErroLista("Não foi possível carregar os clientes: " + erro.message);
        }
    }
}

// ---------------------------------------------------------------- Modal

/** Abre o modal para criar (id = null) ou editar (id = número). */
async function abrirModal(cliente) {
    elems.form.reset();
    elems.modalErro.hidden = true;

    if (cliente) {
        elems.modalTitulo.textContent = "Editar cliente";
        elems.modalSub.textContent = `Cliente #${cliente.id} · registado em ${fmtData.format(new Date(cliente.dataRegisto))}`;
        document.getElementById("f-nome").value = cliente.nome;
        document.getElementById("f-telefone").value = formatarTelefone(cliente.telefone);
        document.getElementById("f-bairro").value = cliente.bairro;
        document.getElementById("f-categoria").value = cliente.categoria;
        document.getElementById("f-estado").value = cliente.estado;
        document.getElementById("f-contador").value = cliente.numeroContador || "";
    } else {
        elems.modalTitulo.textContent = "Novo cliente";
        elems.modalSub.textContent = "Preencha os dados do cliente.";
        document.getElementById("f-categoria").value = "Doméstica";
        document.getElementById("f-estado").value = "Activo";
    }

    elems.form.dataset.id = cliente ? cliente.id : "";
    elems.modal.hidden = false;
    document.getElementById("f-nome").focus();
}

function fecharModal() {
    elems.modal.hidden = true;
}

/** Validação simples no cliente (o backend valida igualmente). */
function validarFormulario() {
    const nome = document.getElementById("f-nome").value.trim();
    const telefone = document.getElementById("f-telefone").value.trim();
    const bairro = document.getElementById("f-bairro").value.trim();

    if (!nome) return "O nome do cliente é obrigatório.";
    if (!/^(\+?258\s?)?8\d(\s?\d){7}$/.test(telefone.replace(/\s+/g, " "))) {
        return "O telefone deve ter 9 dígitos e começar por 8 (ex.: 84 123 4567).";
    }
    if (!bairro) return "O bairro é obrigatório.";
    return null;
}

/** Submissão do modal: POST (criar) ou PUT (editar). */
elems.form.addEventListener("submit", async evento => {
    evento.preventDefault();

    const erroValidacao = validarFormulario();
    if (erroValidacao) {
        mostrarMensagem(elems.modalErro, erroValidacao, "erro");
        return;
    }

    const id = elems.form.dataset.id;
    const dados = {
        nome: document.getElementById("f-nome").value.trim(),
        telefone: document.getElementById("f-telefone").value.trim(),
        bairro: document.getElementById("f-bairro").value.trim(),
        categoria: document.getElementById("f-categoria").value,
        estado: document.getElementById("f-estado").value,
        numeroContador: document.getElementById("f-contador").value.trim() || null
    };

    elems.btnGuardar.disabled = true;
    elems.btnGuardar.textContent = "A guardar…";

    try {
        if (id) {
            await API.clientes.actualizar(id, dados);
            mostrarAviso(`Cliente “${dados.nome}” actualizado com sucesso.`);
        } else {
            await API.clientes.criar(dados);
            mostrarAviso(`Cliente “${dados.nome}” registado com sucesso.`);
        }
        fecharModal();
        await carregar();
    } catch (erro) {
        mostrarMensagem(elems.modalErro, erro.message, "erro");
    } finally {
        elems.btnGuardar.disabled = false;
        elems.btnGuardar.textContent = "Guardar cliente";
    }
});

// ---------------------------------------------------------------- Accções

document.getElementById("btn-novo").addEventListener("click", () => abrirModal(null));
document.getElementById("btn-fechar").addEventListener("click", fecharModal);
document.getElementById("btn-cancelar").addEventListener("click", fecharModal);

// Clique fora do modal fecha-o
elems.modal.addEventListener("click", evento => {
    if (evento.target === elems.modal) {
        fecharModal();
    }
});

// Tecla Escape fecha o modal
document.addEventListener("keydown", evento => {
    if (evento.key === "Escape" && !elems.modal.hidden) {
        fecharModal();
    }
});

/** Editar / eliminar (delegado nos botões de cada linha). */
elems.corpo.addEventListener("click", async evento => {
    const botao = evento.target.closest("button[data-accao]");
    if (!botao) return;

    const cliente = clientes.find(c => String(c.id) === botao.dataset.id);
    if (!cliente) return;

    if (botao.dataset.accao === "editar") {
        abrirModal(cliente);
        return;
    }

    const confirmacao = window.confirm(
        `Eliminar o cliente “${cliente.nome}”?\n\nEsta acção não pode ser desfeita.`);
    if (!confirmacao) return;

    try {
        await API.clientes.remover(cliente.id);
        mostrarAviso(`Cliente “${cliente.nome}” eliminado.`);
        await carregar();
    } catch (erro) {
        if (erro.message !== "Sessão expirada.") {
            mostrarErroLista("Não foi possível eliminar o cliente: " + erro.message);
        }
    }
});

/** Pesquisa com pequena pausa (o filtro de estado é aplicado no cliente). */
let temporizadorBusca;
elems.busca.addEventListener("input", () => {
    clearTimeout(temporizadorBusca);
    temporizadorBusca = setTimeout(() => {
        termoActual = elems.busca.value.trim();
        carregar();
    }, 300);
});

elems.filtroEstado.addEventListener("change", () => desenharLista(clientes));

/** Primeira carga. */
carregar();
