/**
 * app.js — comportamentos comuns a todas as páginas autenticadas
 * (dashboard e páginas internas). Depende de api.js.
 */

/** Redirecciona para o login se não existir sessão no navegador. */
function exigirSessao() {
    if (!Auth.token()) {
        window.location.replace("login.html");
    }
}

/** Preenche os elementos da barra lateral e do topo com os dados do utilizador. */
function preencherUsuario(usuario) {
    document.querySelectorAll("[data-usuario='iniciais']").forEach(el => {
        el.textContent = usuario.iniciais;
    });
    document.querySelectorAll("[data-usuario='nome']").forEach(el => {
        el.textContent = usuario.nome;
    });
    document.querySelectorAll("[data-usuario='perfil']").forEach(el => {
        el.textContent = usuario.perfil;
    });
}

/** Termina a sessão: avisa o backend (best effort) e limpa o navegador. */
function terminarSessao() {
    API.logout()
        .catch(() => { /* o token até pode já não ser válido; seguimos em frente */ })
        .finally(() => {
            Auth.sair();
            window.location.replace("login.html?saiu=1");
        });
}

/** Liga o botão "Terminar sessão" da barra lateral. */
function ligarTerminoSessao() {
    const botao = document.getElementById("btn-logout");
    if (botao) {
        botao.addEventListener("click", evento => {
            evento.preventDefault();
            terminarSessao();
        });
    }
}

/** Mostra uma mensagem de erro/sucesso dentro de um elemento. */
function mostrarMensagem(elemento, texto, tipo) {
    elemento.textContent = texto;
    elemento.className = tipo === "sucesso" ? "form-success" : "form-error";
    elemento.hidden = false;
}
