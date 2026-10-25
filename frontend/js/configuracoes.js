/**
 * configuracoes.js — página de definições: parâmetros do sistema (tarifas e
 * vencimento) e gestão de utilizadores (depende de api.js, app.js, layout.js
 * e pagina.js).
 */

const utilizadorActual = Auth.usuario();
const eAdministrador = utilizadorActual && utilizadorActual.perfil === "Administrador";

const fmtQuantidadeConf = new Intl.NumberFormat("pt-PT");

const elemsConfig = {
    avisoPermissoes: document.getElementById("aviso-permissoes"),
    tabParametros: document.getElementById("tab-parametros"),
    tabUtilizadores: document.getElementById("tab-utilizadores"),
    painelParametros: document.getElementById("painel-parametros"),
    painelUtilizadores: document.getElementById("painel-utilizadores"),
    formParametros: document.getElementById("form-parametros"),
    avisoParametros: document.getElementById("aviso-parametros"),
    erroParametros: document.getElementById("erro-parametros"),
    btnParametros: document.getElementById("btn-guardar-parametros"),
    corpo: document.getElementById("usuarios-corpo"),
    tabelaWrap: document.getElementById("tabela-wrap"),
    vazio: document.getElementById("vazio"),
    avisoUsuarios: document.getElementById("aviso-usuarios"),
    erroUsuarios: document.getElementById("erro-usuarios"),
    modal: document.getElementById("modal"),
    form: document.getElementById("form-usuario"),
    modalTitulo: document.getElementById("modal-titulo"),
    modalSub: document.getElementById("modal-sub"),
    modalErro: document.getElementById("modal-erro"),
    btnGuardar: document.getElementById("btn-guardar"),
    btnNovo: document.getElementById("btn-novo")
};

let utilizadores = [];

/** Escapa texto do utilizador antes de o inserir no HTML. */
function escConfig(valor) {
    return String(valor ?? "").replace(/[&<>"']/g, caractere => ({
        "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;"
    })[caractere]);
}

/** "841234567" -> "84 123 4567" */
function formatarTelefoneConf(digitos) {
    const d = String(digitos || "").replace(/\D/g, "");
    return d.length === 9 ? `${d.slice(0, 2)} ${d.slice(2, 5)} ${d.slice(5)}` : digitos;
}

/** Mostra/oculta mensagens locais. */
let temporizadorAvisoConfig;
function mostrarAvisoConfig(elemento, texto) {
    clearTimeout(temporizadorAvisoConfig);
    elemento.textContent = texto || "";
    elemento.hidden = !texto;
    if (texto) {
        temporizadorAvisoConfig = setTimeout(() => { elemento.hidden = true; }, 5000);
    }
}

function mostrarErroConfig(elemento, texto) {
    elemento.textContent = texto || "";
    elemento.hidden = !texto;
}

// ---------------------------------------------------------------- Separadores

function activarSeparador(qual) {
    const parametros = qual === "parametros";
    elemsConfig.tabParametros.classList.toggle("active", parametros);
    elemsConfig.tabUtilizadores.classList.toggle("active", !parametros);
    elemsConfig.tabParametros.setAttribute("aria-selected", String(parametros));
    elemsConfig.tabUtilizadores.setAttribute("aria-selected", String(!parametros));
    elemsConfig.painelParametros.hidden = !parametros;
    elemsConfig.painelUtilizadores.hidden = parametros;
}

elemsConfig.tabParametros.addEventListener("click", () => activarSeparador("parametros"));
elemsConfig.tabUtilizadores.addEventListener("click", () => activarSeparador("utilizadores"));

// ---------------------------------------------------------------- Parâmetros

/** Chave do backend → id do campo no formulário. */
const CAMPOS_PARAMETROS = {
    "tarifa.domestica": "p-domestica",
    "tarifa.comercial": "p-comercial",
    "tarifa.institucional": "p-institucional",
    "vencimento.dias": "p-vencimento"
};

async function carregarParametros() {
    try {
        const parametros = await API.parametros.lista();
        parametros.forEach(parametro => {
            const campo = document.getElementById(CAMPOS_PARAMETROS[parametro.chave]);
            if (campo) {
                campo.value = parametro.valor;
                campo.placeholder = parametro.descricao || parametro.valor;
            }
        });
    } catch (erro) {
        if (erro.message !== "Sessão expirada.") {
            mostrarErroConfig(elemsConfig.erroParametros,
                "Não foi possível carregar os parâmetros: " + erro.message);
        }
    }
}

elemsConfig.formParametros.addEventListener("submit", async evento => {
    evento.preventDefault();

    if (!eAdministrador) {
        mostrarErroConfig(elemsConfig.erroParametros,
            "Apenas administradores podem alterar os parâmetros.");
        return;
    }

    const pedidos = Object.entries(CAMPOS_PARAMETROS).map(([chave, id]) => ({
        chave,
        valor: document.getElementById(id).value.trim()
    }));

    const vazio = pedidos.find(p => p.valor === "");
    if (vazio) {
        mostrarErroConfig(elemsConfig.erroParametros, "Preencha todos os valores antes de guardar.");
        return;
    }

    elemsConfig.btnParametros.disabled = true;
    elemsConfig.btnParametros.textContent = "A guardar…";

    try {
        await API.parametros.guardar(pedidos);
        mostrarAvisoConfig(elemsConfig.avisoParametros, "Parâmetros actualizados.");
    } catch (erro) {
        mostrarErroConfig(elemsConfig.erroParametros, erro.message);
    } finally {
        elemsConfig.btnParametros.disabled = false;
        elemsConfig.btnParametros.textContent = "Guardar parâmetros";
    }
});

// ---------------------------------------------------------------- Utilizadores

function desenharUtilizadores(lista) {
    elemsConfig.corpo.innerHTML = lista.map(pessoa => `
        <tr>
            <td>
                <span class="client">${escConfig(pessoa.nome)}</span>
                <span class="ref">${escConfig(formatarTelefoneConf(pessoa.telefone))}</span>
            </td>
            <td>${escConfig(pessoa.perfil)}</td>
            <td>${pessoa.activo
                ? '<span class="pill pill-green">Activo</span>'
                : '<span class="pill pill-orange">Desactivado</span>'}</td>
            <td>
                <div class="row-actions">
                    <button class="icon-btn" type="button" data-accao="editar" data-id="${pessoa.id}"
                            aria-label="Editar ${escConfig(pessoa.nome)}" title="Editar"
                            ${eAdministrador ? "" : "hidden"}>
                        <svg class="icon"><use href="#i-edit"/></svg>
                    </button>
                </div>
            </td>
        </tr>`).join("");

    const semResultados = lista.length === 0;
    elemsConfig.tabelaWrap.hidden = semResultados;
    elemsConfig.vazio.hidden = !semResultados;
}

async function carregarUtilizadores() {
    mostrarErroConfig(elemsConfig.erroUsuarios, "");
    try {
        utilizadores = await API.usuarios.lista();
        desenharUtilizadores(utilizadores);
    } catch (erro) {
        if (erro.message !== "Sessão expirada.") {
            mostrarErroConfig(elemsConfig.erroUsuarios,
                "Não foi possível carregar os utilizadores: " + erro.message);
        }
    }
}

/** Modal de criação (id = null) ou edição (id = número). */
function abrirModalUtilizador(pessoa) {
    elemsConfig.form.reset();
    elemsConfig.modalErro.hidden = true;

    if (pessoa) {
        elemsConfig.modalTitulo.textContent = "Editar utilizador";
        elemsConfig.modalSub.textContent = "A palavra-passe fica inalterada se deixada vazia.";
        document.getElementById("u-nome").value = pessoa.nome;
        document.getElementById("u-telefone").value = formatarTelefoneConf(pessoa.telefone);
        document.getElementById("u-perfil").value = pessoa.perfil;
        document.getElementById("u-estado").value = String(pessoa.activo);
        document.getElementById("u-senha").required = false;
    } else {
        elemsConfig.modalTitulo.textContent = "Novo utilizador";
        elemsConfig.modalSub.textContent = "A conta pode ser desactivada mais tarde.";
        document.getElementById("u-perfil").value = "Operador";
        document.getElementById("u-estado").value = "true";
        document.getElementById("u-senha").required = true;
    }

    elemsConfig.form.dataset.id = pessoa ? pessoa.id : "";
    elemsConfig.modal.hidden = false;
    document.getElementById("u-nome").focus();
}

function fecharModalUtilizador() {
    elemsConfig.modal.hidden = true;
}

function validarUtilizador(emEdicao) {
    const nome = document.getElementById("u-nome").value.trim();
    const telefone = document.getElementById("u-telefone").value.trim();
    const senha = document.getElementById("u-senha").value;

    if (!nome) return "O nome do utilizador é obrigatório.";
    if (!/^(\+?258\s?)?8\d(\s?\d){7}$/.test(telefone)) {
        return "O telefone deve ter 9 dígitos e começar por 8 (ex.: 84 123 4567).";
    }
    if (!emEdicao && senha.length < 6) return "A palavra-passe deve ter pelo menos 6 caracteres.";
    if (emEdicao && senha && senha.length < 6) return "A palavra-passe deve ter pelo menos 6 caracteres.";
    return null;
}

elemsConfig.btnNovo.addEventListener("click", () => abrirModalUtilizador(null));
document.getElementById("btn-fechar").addEventListener("click", fecharModalUtilizador);
document.getElementById("btn-cancelar").addEventListener("click", fecharModalUtilizador);

elemsConfig.modal.addEventListener("click", evento => {
    if (evento.target === elemsConfig.modal) fecharModalUtilizador();
});

document.addEventListener("keydown", evento => {
    if (evento.key === "Escape" && !elemsConfig.modal.hidden) fecharModalUtilizador();
});

elemsConfig.form.addEventListener("submit", async evento => {
    evento.preventDefault();

    const id = elemsConfig.form.dataset.id;
    const erroValidacao = validarUtilizador(Boolean(id));
    if (erroValidacao) {
        mostrarMensagem(elemsConfig.modalErro, erroValidacao, "erro");
        return;
    }

    const dados = {
        nome: document.getElementById("u-nome").value.trim(),
        telefone: document.getElementById("u-telefone").value.trim(),
        senha: document.getElementById("u-senha").value || null,
        perfil: document.getElementById("u-perfil").value,
        activo: document.getElementById("u-estado").value === "true"
    };

    elemsConfig.btnGuardar.disabled = true;
    elemsConfig.btnGuardar.textContent = "A guardar…";

    try {
        if (id) {
            await API.usuarios.actualizar(id, dados);
            mostrarAvisoConfig(elemsConfig.avisoUsuarios, `Utilizador “${dados.nome}” actualizado.`);
        } else {
            await API.usuarios.criar(dados);
            mostrarAvisoConfig(elemsConfig.avisoUsuarios, `Utilizador “${dados.nome}” criado.`);
        }
        fecharModalUtilizador();
        await carregarUtilizadores();
    } catch (erro) {
        mostrarMensagem(elemsConfig.modalErro, erro.message, "erro");
    } finally {
        elemsConfig.btnGuardar.disabled = false;
        elemsConfig.btnGuardar.textContent = "Guardar utilizador";
    }
});

/** Editar (delegado no botão de cada linha). */
elemsConfig.corpo.addEventListener("click", evento => {
    const botao = evento.target.closest("button[data-accao='editar']");
    if (!botao) return;

    const pessoa = utilizadores.find(u => String(u.id) === botao.dataset.id);
    if (pessoa) abrirModalUtilizador(pessoa);
});

// ---------------------------------------------------------------- Arranque

if (!eAdministrador) {
    mostrarErroConfig(elemsConfig.avisoPermissoes,
        "Sessão iniciada como " + utilizadorActual.perfil + ". "
        + "Apenas administradores podem alterar estas definições (a aplicação valida também no servidor).");
    elemsConfig.btnNovo.hidden = true;
    elemsConfig.btnParametros.disabled = true;
}

carregarParametros();
carregarUtilizadores();
