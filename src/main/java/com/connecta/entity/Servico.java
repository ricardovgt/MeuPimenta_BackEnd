package com.connecta.entity;

public class Servico {
    private int id;
    private int idUsuario;
    private String nome;
    private String descricao;
    private String telefone;
    private String bairro;
    private String fotoUrl;
    private double avaliacaoMedia;
    private String descricaoDetalhada;
    private int totalAvaliacoes;

    // Getters
    public int getId() { return id; }
    public int getIdUsuario() { return idUsuario; }
    public String getNome() { return nome; }
    public String getDescricao() { return descricao; }
    public String getTelefone() { return telefone; }
    public String getBairro() { return bairro; }
    public String getFotoUrl() { return fotoUrl; }
    public double getAvaliacaoMedia() { return avaliacaoMedia; }
    public String getDescricaoDetalhada() { return descricaoDetalhada; }
    public void setDescricaoDetalhada(String descricaoDetalhada) { this.descricaoDetalhada = descricaoDetalhada; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setIdUsuario(int idUsuario) { this.idUsuario = idUsuario; }
    public void setNome(String nome) { this.nome = nome; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public void setTelefone(String telefone) { this.telefone = telefone; }
    public void setBairro(String bairro) { this.bairro = bairro; }
    public void setFotoUrl(String fotoUrl) { this.fotoUrl = fotoUrl; }
    public void setAvaliacaoMedia(double avaliacaoMedia) { this.avaliacaoMedia = avaliacaoMedia; }
    public int getTotalAvaliacoes() { return totalAvaliacoes; }
    public void setTotalAvaliacoes(int totalAvaliacoes) { this.totalAvaliacoes = totalAvaliacoes; }
}