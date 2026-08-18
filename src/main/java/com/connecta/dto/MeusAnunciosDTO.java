package com.connecta.dto;

public class MeusAnunciosDTO {
    private int id;
    private String nome;
    private String descricao;
    private String descricaoDetalhada;
    private String telefone;
    private String tipo;
    private String status;
    private String fotoCapa;
    private String nomeUsuario;

    public MeusAnunciosDTO(int id, String nome, String descricao, String descricaoDetalhada,
                           String telefone, String tipo, String status,
                           String fotoCapa, String nomeUsuario) {
        this.id = id;
        this.nome = nome;
        this.descricao = descricao;
        this.descricaoDetalhada = descricaoDetalhada;
        this.telefone = telefone;
        this.tipo = tipo;
        this.status = status;
        this.fotoCapa = fotoCapa;
        this.nomeUsuario = nomeUsuario;
    }

    // Getters
    public int getId() { return id; }
    public String getNome() { return nome; }
    public String getDescricao() { return descricao; }
    public String getDescricaoDetalhada() { return descricaoDetalhada; }
    public String getTelefone() { return telefone; }
    public String getTipo() { return tipo; }
    public String getStatus() { return status; }
    public String getFotoCapa() { return fotoCapa; }
    public String getNomeUsuario() { return nomeUsuario; }
}
