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
    },
    clientes: {
        /** Lista clientes, opcionalmente filtrados por ?busca=. */
        lista(busca = "") {
            const caminho = busca.trim()
                ? "/clientes?busca=" + encodeURIComponent(busca.trim())
                : "/clientes";
            return api(caminho);
        },
        obter(id) {
            return api("/clientes/" + id);
        },
        criar(dados) {
            return api("/clientes", { method: "POST", body: JSON.stringify(dados) });
        },
        actualizar(id, dados) {
            return api("/clientes/" + id, { method: "PUT", body: JSON.stringify(dados) });
        },
        remover(id) {
            return api("/clientes/" + id, { method: "DELETE" });
        }
    },
    leituras: {
        /** Lista leituras; filtros opcionais por ?busca= e ?periodo=AAAA-MM. */
        lista(busca = "", periodo = "") {
            const parametros = new URLSearchParams();
            if (busca.trim()) parametros.set("busca", busca.trim());
            if (periodo.trim()) parametros.set("periodo", periodo.trim());
            const query = parametros.toString();
            return api("/leituras" + (query ? "?" + query : ""));
        },
        criar(dados) {
            return api("/leituras", { method: "POST", body: JSON.stringify(dados) });
        },
        actualizar(id, dados) {
            return api("/leituras/" + id, { method: "PUT", body: JSON.stringify(dados) });
        },
        remover(id) {
            return api("/leituras/" + id, { method: "DELETE" });
        }
    },
    facturas: {
        /** Lista facturas; filtros opcionais por ?busca=, ?estado= e ?periodo=. */
        lista(busca = "", estado = "", periodo = "") {
            const parametros = new URLSearchParams();
            if (busca.trim()) parametros.set("busca", busca.trim());
            if (estado.trim()) parametros.set("estado", estado.trim());
            if (periodo.trim()) parametros.set("periodo", periodo.trim());
            const query = parametros.toString();
            return api("/facturas" + (query ? "?" + query : ""));
        },
        obter(id) {
            return api("/facturas/" + id);
        },
        pagamentos(id) {
            return api("/facturas/" + id + "/pagamentos");
        },
        /** Emite a factura de uma leitura. */
        gerar(leituraId) {
            return api("/facturas", { method: "POST", body: JSON.stringify({ leituraId }) });
        },
        /** Emite todas as facturas em falta de um mês ("AAAA-MM"). */
        gerarMes(periodo) {
            return api("/facturas/gerar-mes", { method: "POST", body: JSON.stringify({ periodo }) });
        },
        remover(id) {
            return api("/facturas/" + id, { method: "DELETE" });
        }
    },
    pagamentos: {
        /** Lista pagamentos; filtros opcionais por ?busca= e ?metodo=. */
        lista(busca = "", metodo = "") {
            const parametros = new URLSearchParams();
            if (busca.trim()) parametros.set("busca", busca.trim());
            if (metodo.trim()) parametros.set("metodo", metodo.trim());
            const query = parametros.toString();
            return api("/pagamentos" + (query ? "?" + query : ""));
        },
        registar(dados) {
            return api("/pagamentos", { method: "POST", body: JSON.stringify(dados) });
        },
        remover(id) {
            return api("/pagamentos/" + id, { method: "DELETE" });
        }
    },
    usuarios: {
        lista() {
            return api("/usuarios");
        },
        criar(dados) {
            return api("/usuarios", { method: "POST", body: JSON.stringify(dados) });
        },
        actualizar(id, dados) {
            return api("/usuarios/" + id, { method: "PUT", body: JSON.stringify(dados) });
        }
    },
    parametros: {
        lista() {
            return api("/parametros");
        },
        /** Actualiza os parâmetros: lista de { chave, valor }. */
        guardar(pedidos) {
            return api("/parametros", { method: "PUT", body: JSON.stringify(pedidos) });
        }
    },
    relatorios: {
        /** Relatório de um período (datas AAAA-MM-DD; vazio = últimos 6 meses). */
        gerar(inicio = "", fim = "") {
            const parametros = new URLSearchParams();
            if (inicio.trim()) parametros.set("inicio", inicio.trim());
            if (fim.trim()) parametros.set("fim", fim.trim());
            const query = parametros.toString();
            return api("/relatorios" + (query ? "?" + query : ""));
        }
    }
};
