package mz.edu.fontanaria.models;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Cliente da Fontanária: a quem o serviço é prestado e a quem são atribuídas
 * leituras, facturas e pagamentos (nessas entidades, por enquanto, ainda não
 * existem — a ligação será feita quando elas forem implementadas).
 */
@Entity
@Table(name = "clientes")
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    // Guardado só com dígitos, sem espaços nem +258 (ex.: 841234567)
    @Column(nullable = false, length = 12)
    private String telefone;

    @Column(nullable = false)
    private String bairro;

    // Categoria tarifária: Doméstica, Comercial ou Institucional
    @Column(nullable = false)
    private String categoria = "Doméstica";

    // Pode estar vazio: um cadastro sem contador é "incompleto"
    @Column(unique = true, length = 20)
    private String numeroContador;

    @Column(nullable = false)
    private String estado = "Activo";

    @Column(nullable = false)
    private LocalDateTime dataRegisto = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }

    public String getBairro() { return bairro; }
    public void setBairro(String bairro) { this.bairro = bairro; }

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }

    public String getNumeroContador() { return numeroContador; }
    public void setNumeroContador(String numeroContador) { this.numeroContador = numeroContador; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public LocalDateTime getDataRegisto() { return dataRegisto; }
    public void setDataRegisto(LocalDateTime dataRegisto) { this.dataRegisto = dataRegisto; }
}
