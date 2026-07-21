package com.connecta.dto;

public class ServicoDetalheDTO {
    private int id;
    private String nome;
    private String descricao;
    private String descricaoDetalhada;
    private String telefone;
    private String bairro;
    private String fotoUrl;
    private double avaliacaoMedia;
    private int totalAvaliacoes;

    private int idUsuario;
    private String nomeUsuario;

    public ServicoDetalheDTO(int id, String nome, String descricao, String descricaoDetalhada,
                              String telefone, String bairro, String fotoUrl,
                              double avaliacaoMedia, int totalAvaliacoes,
                              int idUsuario, String nomeUsuario) {
        this.id = id;
        this.nome = nome;
        this.descricao = descricao;
        this.descricaoDetalhada = descricaoDetalhada;
        this.telefone = telefone;
        this.bairro = bairro;
        this.fotoUrl = fotoUrl;
        this.avaliacaoMedia = avaliacaoMedia;
        this.totalAvaliacoes = totalAvaliacoes;
        this.idUsuario = idUsuario;
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
    public double getAvaliacaoMedia() { return avaliacaoMedia; }
    public int getTotalAvaliacoes() { return totalAvaliacoes; }
    public int getIdUsuario() { return idUsuario; }
    public String getNomeUsuario() { return nomeUsuario; }
}