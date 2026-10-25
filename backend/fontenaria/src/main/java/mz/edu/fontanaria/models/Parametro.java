package mz.edu.fontanaria.models;

import jakarta.persistence.*;

/**
 * Parâmetro de configuração do sistema (chave/valor), editado na página de
 * Definições. Exemplos: tarifa por m³ de cada categoria, dias de vencimento.
 */
@Entity
@Table(name = "parametros")
public class Parametro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String chave;

    @Column(nullable = false, length = 40)
    private String valor;

    @Column(length = 120)
    private String descricao;

    public Parametro() {
    }

    public Parametro(String chave, String valor, String descricao) {
        this.chave = chave;
        this.valor = valor;
        this.descricao = descricao;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getChave() { return chave; }
    public void setChave(String chave) { this.chave = chave; }

    public String getValor() { return valor; }
    public void setValor(String valor) { this.valor = valor; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
}
