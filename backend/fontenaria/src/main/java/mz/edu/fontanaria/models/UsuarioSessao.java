package mz.edu.fontanaria.models;

/**
 * Dados do utilizador autenticado (sem a palavra-passe).
 *
 * É este objecto que a API devolve no login e em /api/auth/me, e que serve de
 * principal (identidade) nos pedidos autenticados, extraído do token JWT.
 */
public class UsuarioSessao {

    private final Long id;
    private final String nome;
    private final String perfil;

    public UsuarioSessao(Long id, String nome, String perfil) {
        this.id = id;
        this.nome = nome;
        this.perfil = perfil;
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public String getPerfil() { return perfil; }

    public String getPrimeiroNome() {
        return nome.trim().split("\\s+")[0];
    }

    /** "Ernesto Sitoe" -> "ES" */
    public String getIniciais() {
        String[] partes = nome.trim().split("\\s+");
        String iniciais = partes.length == 1
                ? partes[0].substring(0, Math.min(2, partes[0].length()))
                : "" + partes[0].charAt(0) + partes[partes.length - 1].charAt(0);
        return iniciais.toUpperCase();
    }
}
