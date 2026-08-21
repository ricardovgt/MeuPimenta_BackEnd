package com.connecta.dto;

public class AnuncioPublicoDTO {
    private final int id;
    private final String nome;
    private final String descricao;
    private final String fotoCapa;
    private final String nomeUsuario;
    private final String tipo;
    private final double avaliacaoMedia;
    private final int totalAvaliacoes;

    public AnuncioPublicoDTO(int id, String nome, String descricao, String fotoCapa,
                            String nomeUsuario, String tipo, double avaliacaoMedia,
                            int totalAvaliacoes) {
        this.id = id;
        this.nome = nome;
        this.descricao = descricao;
        this.fotoCapa = fotoCapa;
        this.nomeUsuario = nomeUsuario;
        this.tipo = tipo;
        this.avaliacaoMedia = avaliacaoMedia;
        this.totalAvaliacoes = totalAvaliacoes;
    }

    public int getId() { return id; }
    public String getNome() { return nome; }
    public String getDescricao() { return descricao; }
    public String getFotoCapa() { return fotoCapa; }
    public String getNomeUsuario() { return nomeUsuario; }
    public String getTipo() { return tipo; }
    public double getAvaliacaoMedia() { return avaliacaoMedia; }
    public int getTotalAvaliacoes() { return totalAvaliacoes; }
}
