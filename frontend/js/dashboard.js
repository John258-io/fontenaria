/**
 * dashboard.js — carrega o resumo da operação em /api/dashboard e desenha-o
 * no ecrã (depende de api.js e de app.js).
 */

exigirSessao();
ligarTerminoSessao();

const fmtNumero = new Intl.NumberFormat("pt-PT");

/** Mostra uma mensagem de erro no lugar do conteúdo, se o pedido falhar. */
function mostrarErroDashboard(mensagem) {
    const conteudo = document.querySelector(".content");
    conteudo.innerHTML =
        '<div class="alert" role="alert">' +
        '<svg class="icon"><use href="#i-alert"/></svg> ' + mensagem +
        "</div>";
}

/** Cartões de indicadores (4 cartões no topo). */
function desenharIndicadores(indicadores) {
    const sec = document.getElementById("stats");
    sec.innerHTML = indicadores.map(ind => `
        <a href="${ind.href}" class="card stat">
            <div class="stat-icon ${ind.tom}"><svg class="icon"><use href="#${ind.icone}"/></svg></div>
            <div>
                <p class="stat-label">${ind.rotulo}</p>
                <p class="stat-value">${ind.valor}</p>
                <p class="stat-note${ind.notaDestaque ? " warn" : ""}">${ind.nota}</p>
            </div>
            <svg class="icon stat-arrow"><use href="#i-right"/></svg>
        </a>`).join("");
}

/** Gráfico de barras da receita mensal. */
function desenharReceita(receita) {
    document.getElementById("receita-total").textContent = receita.total;
    document.getElementById("receita-variacao").textContent = receita.variacao;

    const maximo = Math.max(...receita.meses.map(m => m.valor));
    document.getElementById("barras").innerHTML = receita.meses.map(mes => `
        <div class="bar-col${mes.mes === receita.mesActual ? " current" : ""}">
            <span class="bar-value">${mes.rotulo}</span>
            <div class="bar" style="height:${Math.round(mes.valor / maximo * 100)}%"></div>
        </div>`).join("");
    document.getElementById("bar-labels").innerHTML =
        receita.meses.map(mes => `<span>${mes.nome}</span>`).join("");
}

/** Lista "Atenção necessária". */
function desenharAtencoes(atencoes) {
    document.getElementById("atencoes-total").textContent = atencoes.total;
    document.getElementById("atencoes-lista").innerHTML = atencoes.itens.map(item => `
        <a href="${item.href}" class="attention-item">
            <div class="stat-icon ${item.tom}"><svg class="icon"><use href="#${item.icone}"/></svg></div>
            <div>
                <strong>${item.titulo}</strong>
                <span>${item.descricao}</span>
            </div>
            <svg class="icon"><use href="#i-right"/></svg>
        </a>`).join("");
}

/** Tabela de actividade recente. */
function desenharActividade(actividade) {
    document.getElementById("actividade-corpo").innerHTML = actividade.map(linha => `
        <tr>
            <td><span class="client">${linha.cliente}</span><span class="ref">${linha.referencia}</span></td>
            <td>${linha.tipo}</td>
            <td>${linha.data}</td>
            <td>${linha.metodo}</td>
            <td${linha.entrada ? ' class="money-in"' : ""}>${linha.valor}</td>
            <td><span class="pill ${linha.tom}">${linha.estado}</span></td>
        </tr>`).join("");
}

/** Pedido principal: hidratar toda a página. */
(async () => {
    try {
        const dados = await API.dashboard();

        preencherUsuario(dados.usuario);
        document.getElementById("badge-clientes").textContent = fmtNumero.format(dados.indicadores[0].valorNumerico);
        document.getElementById("saudacao").textContent = `${dados.saudacao}, ${dados.usuario.primeiroNome}`;
        document.getElementById("data-actual").textContent = dados.data;

        desenharIndicadores(dados.indicadores);
        desenharReceita(dados.receita);
        desenharAtencoes(dados.atencoes);
        desenharActividade(dados.actividade);
    } catch (erro) {
        if (erro.message !== "Sessão expirada.") {
            mostrarErroDashboard("Não foi possível carregar o painel: " + erro.message);
        }
    }
})();

/** Fechar o aviso de ambiente de demonstração. */
document.querySelector(".alert-close").addEventListener("click", () => {
    document.getElementById("demo-alert").remove();
});
