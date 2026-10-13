/**
 * login.js — lógica da página de login (depende de api.js).
 */

const form = document.getElementById("form-login");
const campoTelefone = document.getElementById("telefone");
const campoSenha = document.getElementById("senha");
const caixaMensagem = document.getElementById("mensagem");
const botaoEntrar = form.querySelector("button[type='submit']");
const textoOriginalBotao = botaoEntrar.textContent;

/** Já há sessão? Nem sequer mostrar o login. */
if (Auth.token()) {
    window.location.replace("dashboard.html");
}

/** Avisos vindos do logout ou de sessão expirada (?saiu=1 / ?expirou=1). */
const parametros = new URLSearchParams(window.location.search);
if (parametros.has("saiu")) {
    mostrarMensagem(caixaMensagem, "Sessão terminada com sucesso.", "sucesso");
} else if (parametros.has("expirou")) {
    mostrarMensagem(caixaMensagem, "A sua sessão expirou. Entre novamente.", "erro");
}

/** Botão de mostrar/esconder palavra-passe. */
const toggle = document.querySelector(".toggle-password");
toggle.addEventListener("click", () => {
    const visivel = campoSenha.type === "password";
    campoSenha.type = visivel ? "text" : "password";
    toggle.setAttribute("aria-label", visivel ? "Esconder palavra-passe" : "Mostrar palavra-passe");
});

/** Submissão do formulário → POST /api/auth/login. */
form.addEventListener("submit", async evento => {
    evento.preventDefault();
    caixaMensagem.hidden = true;

    botaoEntrar.disabled = true;
    botaoEntrar.textContent = "A entrar…";

    try {
        const resposta = await API.login(campoTelefone.value.trim(), campoSenha.value);
        Auth.guardar(resposta.token, resposta.usuario);
        window.location.replace("dashboard.html");
    } catch (erro) {
        mostrarMensagem(caixaMensagem, erro.message, "erro");
        botaoEntrar.disabled = false;
        botaoEntrar.textContent = textoOriginalBotao;
        campoSenha.focus();
    }
});
