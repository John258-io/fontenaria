/**
 * layout.js — constrói o sprite de ícones, a barra lateral e a barra superior
 * das páginas internas ainda em construção (clientes, leituras, definições),
 * para não duplicar centenas de linhas de HTML em cada página.
 *
 * Uso: montarLayout({ titulo: "Clientes", caminho: "clientes.html" });
 */

function montarLayout(opcoes) {
    const { titulo, caminho } = opcoes;

    const itemNav = (href, icone, texto, extra = "") => `
        <a href="${href}" class="nav-item${href === caminho ? " active" : ""}"${href === caminho ? ' aria-current="page"' : ""}>
            <svg class="icon"><use href="#${icone}"/></svg> ${texto}${extra}
        </a>`;

    const html = `
    <svg width="0" height="0" style="position:absolute" aria-hidden="true">
        <symbol id="i-grid" viewBox="0 0 24 24"><rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/></symbol>
        <symbol id="i-users" viewBox="0 0 24 24"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></symbol>
        <symbol id="i-file" viewBox="0 0 24 24"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><path d="M14 2v6h6"/><path d="M16 13H8"/><path d="M16 17H8"/><path d="M10 9H8"/></symbol>
        <symbol id="i-cash" viewBox="0 0 24 24"><rect x="2" y="6" width="20" height="12" rx="2"/><circle cx="12" cy="12" r="2"/><path d="M6 12h.01M18 12h.01"/></symbol>
        <symbol id="i-chart" viewBox="0 0 24 24"><path d="M12 20V10"/><path d="M18 20V4"/><path d="M6 20v-4"/></symbol>
        <symbol id="i-gear" viewBox="0 0 24 24"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.7 1.7 0 0 0 .3 1.8l.1.1a2 2 0 1 1-2.8 2.8l-.1-.1a1.7 1.7 0 0 0-1.8-.3 1.7 1.7 0 0 0-1 1.5V21a2 2 0 1 1-4 0v-.1a1.7 1.7 0 0 0-1.1-1.5 1.7 1.7 0 0 0-1.8.3l-.1.1a2 2 0 1 1-2.8-2.8l.1-.1a1.7 1.7 0 0 0 .3-1.8 1.7 1.7 0 0 0-1.5-1H3a2 2 0 1 1 0-4h.1a1.7 1.7 0 0 0 1.5-1.1 1.7 1.7 0 0 0-.3-1.8l-.1-.1a2 2 0 1 1 2.8-2.8l.1.1a1.7 1.7 0 0 0 1.8.3H9a1.7 1.7 0 0 0 1-1.5V3a2 2 0 1 1 4 0v.1a1.7 1.7 0 0 0 1 1.5 1.7 1.7 0 0 0 1.8-.3l.1-.1a2 2 0 1 1 2.8 2.8l-.1.1a1.7 1.7 0 0 0-.3 1.8V9a1.7 1.7 0 0 0 1.5 1H21a2 2 0 1 1 0 4h-.1a1.7 1.7 0 0 0-1.5 1Z"/></symbol>
        <symbol id="i-calendar" viewBox="0 0 24 24"><rect x="3" y="4" width="18" height="18" rx="2"/><path d="M16 2v4"/><path d="M8 2v4"/><path d="M3 10h18"/></symbol>
        <symbol id="i-bell" viewBox="0 0 24 24"><path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/><path d="M13.73 21a2 2 0 0 1-3.46 0"/></symbol>
        <symbol id="i-down" viewBox="0 0 24 24"><path d="M6 9l6 6 6-6"/></symbol>
        <symbol id="i-logout" viewBox="0 0 24 24"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><path d="M16 17l5-5-5-5"/><path d="M21 12H9"/></symbol>
        <symbol id="i-info" viewBox="0 0 24 24"><circle cx="12" cy="12" r="10"/><path d="M12 16v-4"/><path d="M12 8h.01"/></symbol>
        <symbol id="i-close" viewBox="0 0 24 24"><path d="M18 6L6 18M6 6l12 12"/></symbol>
        <symbol id="i-more" viewBox="0 0 24 24"><circle cx="5" cy="12" r="1"/><circle cx="12" cy="12" r="1"/><circle cx="19" cy="12" r="1"/></symbol>
        <symbol id="i-plus" viewBox="0 0 24 24"><path d="M12 5v14M5 12h14"/></symbol>
        <symbol id="i-right" viewBox="0 0 24 24"><path d="M9 18l6-6-6-6"/></symbol>
        <symbol id="i-search" viewBox="0 0 24 24"><circle cx="11" cy="11" r="7"/><path d="M21 21l-4.35-4.35"/></symbol>
        <symbol id="i-edit" viewBox="0 0 24 24"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.12 2.12 0 0 1 3 3L12 15l-4 1 1-4z"/></symbol>
        <symbol id="i-trash" viewBox="0 0 24 24"><path d="M3 6h18"/><path d="M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/><path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6"/><path d="M10 11v6M14 11v6"/></symbol>
        <symbol id="i-drop" viewBox="0 0 24 24"><path d="M12 2.69l5.66 5.66a8 8 0 1 1-11.31 0z"/></symbol>
        <symbol id="i-alert" viewBox="0 0 24 24"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/><path d="M12 9v4"/><path d="M12 17h.01"/></symbol>
    </svg>

    <div class="app">
        <aside class="sidebar">
            <div class="brand-logo">
                <svg viewBox="0 0 30 36" fill="none" stroke="#8ec5e8" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                    <path d="M15 3C15 3 6 13 6 20a9 9 0 0 0 18 0C24 13 15 3 15 3Z"/>
                    <path d="M11 21a4 4 0 0 0 4 4"/>
                    <path d="M2 32c6-2 14-2 26 0"/>
                </svg>
                <div class="brand-logo__name">
                    <strong>Fontanária</strong>
                    <span>Manhiça</span>
                </div>
            </div>
            <hr>

            <nav>
                ${itemNav("dashboard.html", "i-grid", "Visão geral")}
                <p class="nav-section">Operações</p>
                ${itemNav("clientes.html", "i-users", "Clientes", '<span class="badge" id="badge-clientes" hidden></span>')}
                ${itemNav("leituras.html", "i-file", "Leituras")}
                ${itemNav("facturas.html", "i-file", "Facturação")}
                ${itemNav("pagamentos.html", "i-cash", "Pagamentos")}

                <p class="nav-section">Análise</p>
                ${itemNav("relatorios.html", "i-chart", "Relatórios")}

                <p class="nav-section">Sistema</p>
                ${itemNav("configuracoes.html", "i-gear", "Definições")}
            </nav>

            <div class="sidebar-footer">
                <div class="sidebar-user">
                    <div class="avatar" data-usuario="iniciais">—</div>
                    <div class="user-info">
                        <strong data-usuario="nome">…</strong>
                        <span data-usuario="perfil">…</span>
                    </div>
                    <svg class="icon"><use href="#i-more"/></svg>
                </div>
                <a href="#" id="btn-logout" class="nav-item">
                    <svg class="icon"><use href="#i-logout"/></svg> Terminar sessão
                </a>
            </div>
        </aside>

        <div class="main">
            <header class="topbar">
                <div>
                    <p class="breadcrumb">Fontanária Manhiça <span>/</span> ${titulo}</p>
                    <h1>${titulo}</h1>
                </div>

                <div class="topbar-right">
                    <button class="bell" type="button" aria-label="Notificações">
                        <svg class="icon"><use href="#i-bell"/></svg>
                    </button>
                    <div class="topbar-user">
                        <div class="avatar" data-usuario="iniciais">—</div>
                        <div class="user-info">
                            <strong data-usuario="nome">…</strong>
                            <span data-usuario="perfil">…</span>
                        </div>
                        <svg class="icon"><use href="#i-down"/></svg>
                    </div>
                </div>
            </header>

            <main class="content" id="conteudo"></main>
        </div>
    </div>`;

    document.body.insertAdjacentHTML("afterbegin", html);

    // Traz o conteúdo da página (que começa escondido no HTML) para dentro da área principal
    const conteudo = document.getElementById("conteudo-pagina");
    if (conteudo) {
        conteudo.hidden = false;
        document.getElementById("conteudo").append(conteudo);
    }
}
