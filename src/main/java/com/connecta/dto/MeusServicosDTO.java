package com.connecta.dto;

public class MeusServicosDTO {
    private int id;
    private String nome;
    private String descricao;
    private String descricaoDetalhada;
    private String telefone;
    private String bairro;
    private String fotoUrl;
    private String nomeUsuario;

    public MeusServicosDTO(int id, String nome, String descricao, String descricaoDetalhada, 
                           String telefone, String bairro, String fotoUrl, String nomeUsuario) {
        this.id = id;
        this.nome = nome;
        this.descricao = descricao;
        this.descricaoDetalhada = descricaoDetalhada;
        this.telefone = telefone;
        this.bairro = bairro;
        this.fotoUrl = fotoUrl;
        this.nomeUsuario = nomeUsuario;
    }
    
    // Getters
    public int getId() { return id; }
    public String getNome() { return nome; }
    public String getDescricao() { return descricao; }
    public String getDescricaoDetalhada() { return descricaoDetalhada; }
    public String getTelefone() { return telefone; }
    public String getBairro() { return bairro; }
    public String getFotoUrl() { return fotoUrl; }
    public String getNomeUsuario() { return nomeUsuario; }
}