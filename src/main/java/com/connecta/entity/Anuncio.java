package com.connecta.entity;

public class Anuncio {
    private int id;
    private int idUsuario;
    private String nome;
    private String descricao;
    private String telefone;
    private String descricaoDetalhada;
    private String tipo;
    private String status;

    // Getters
    public int getId() { return id; }
    public int getIdUsuario() { return idUsuario; }
    public String getNome() { return nome; }
    public String getDescricao() { return descricao; }
    public String getTelefone() { return telefone; }
    public String getDescricaoDetalhada() { return descricaoDetalhada; }
    public String getTipo() { return tipo; }
    public String getStatus() { return status; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setIdUsuario(int idUsuario) { this.idUsuario = idUsuario; }
    public void setNome(String nome) { this.nome = nome; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public void setTelefone(String telefone) { this.telefone = telefone; }
    public void setDescricaoDetalhada(String descricaoDetalhada) { this.descricaoDetalhada = descricaoDetalhada; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public void setStatus(String status) { this.status = status; }
}
