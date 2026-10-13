/**
 * pagina.js — comportamento das páginas internas ainda em construção
 * (clientes, leituras, definições). Depende de api.js e de app.js.
 */

exigirSessao();
ligarTerminoSessao();

/** Preenche a barra lateral/topo com o utilizador guardado no login. */
const usuario = Auth.usuario();
if (usuario) {
    preencherUsuario(usuario);
}
