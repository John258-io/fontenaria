/**
 * api.js — ponto único de contacto com a API do backend.
 *
 * A sessão é guardada no localStorage: um token JWT ("fm_token") e os dados
 * básicos do utilizador ("fm_usuario"). Todos os pedidos autenticados levam o
 * token no header "Authorization: Bearer ...".
 */

const API_BASE = "http://localhost:8080/api";
const CHAVE_TOKEN = "fm_token";
const CHAVE_USUARIO = "fm_usuario";

/** Guarda / lê / remove a sessão do utilizador no navegador. */
const Auth = {
    guardar(token, usuario) {
        localStorage.setItem(CHAVE_TOKEN, token);
        localStorage.setItem(CHAVE_USUARIO, JSON.stringify(usuario));
    },
    token() {
        return localStorage.getItem(CHAVE_TOKEN);
    },
    usuario() {
        const dados = localStorage.getItem(CHAVE_USUARIO);
        return dados ? JSON.parse(dados) : null;
    },
    sair() {
        localStorage.removeItem(CHAVE_TOKEN);
        localStorage.removeItem(CHAVE_USUARIO);
    }
};

/** Faz um pedido à API, tratando erros e sessão expirada. */
async function api(caminho, opcoes = {}) {
    const headers = { ...(opcoes.headers || {}) };
    if (opcoes.body) {
        headers["Content-Type"] = "application/json";
    }

    const token = Auth.token();
    if (token) {
        headers["Authorization"] = "Bearer " + token;
    }

    const resposta = await fetch(API_BASE + caminho, { ...opcoes, headers });

    // Token inválido ou expirado: volta para o login (excepto nos próprios pedidos de auth)
    if (resposta.status === 401 && !caminho.startsWith("/auth/")) {
        Auth.sair();
        window.location.replace("login.html?expirou=1");
        throw new Error("Sessão expirada.");
    }

    if (!resposta.ok) {
        let mensagem = "Não foi possível contactar o servidor.";
        try {
            const dados = await resposta.json();
            if (dados && dados.erro) {
                mensagem = dados.erro;
            }
        } catch (ignorar) { /* resposta sem corpo JSON */ }
        throw new Error(mensagem);
    }

    if (resposta.status === 204) {
        return null;
    }
    return resposta.json();
}

/** Endpoints da API, agrupados por recurso. */
const API = {
    login(telefone, senha) {
        return api("/auth/login", {
            method: "POST",
            body: JSON.stringify({ telefone, senha })
        });
    },
    logout() {
        return api("/auth/logout", { method: "POST" });
    },
    eu() {
        return api("/auth/me");
    },
    dashboard() {
        return api("/dashboard");
    }
};
