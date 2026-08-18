package com.connecta.dto;

public class AnuncioCardDTO {
    private int id;
    private String nome;
    private String descricao;
    private String fotoCapa;
    private String nomeUsuario;

    public AnuncioCardDTO(int id, String nome, String descricao, String fotoCapa, String nomeUsuario) {
        this.id = id;
        this.nome = nome;
        this.descricao = descricao;
        this.fotoCapa = fotoCapa;
        this.nomeUsuario = nomeUsuario;
    }

    // Getters
    public int getId() { return id; }
    public String getNome() { return nome; }
    public String getDescricao() { return descricao; }
    public String getFotoCapa() { return fotoCapa; }
    public String getNomeUsuario() { return nomeUsuario; }
}