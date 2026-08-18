package com.connecta.dto;

import java.util.List;

public class AnuncioDetalheDTO {
    private int id;
    private String nome;
    private String descricao;
    private String descricaoDetalhada;
    private String telefone;
    private String tipo;
    private String status;
    private List<FotoAnuncioDTO> fotos;
    private double avaliacaoMedia;
    private int totalAvaliacoes;

    // CAMPOS PARA O GRÁFICO / DISTRIBUIÇÃO DE ESTRELAS
    private int total5Estrelas;
    private int total4Estrelas;
    private int total3Estrelas;
    private int total2Estrelas;
    private int total1Estrelas;

    private int idUsuario;
    private String nomeUsuario;
    private String fotoPerfilUsuario;

    public AnuncioDetalheDTO(int id, String nome, String descricao, String descricaoDetalhada,
                              String telefone, String tipo, String status,
                              List<FotoAnuncioDTO> fotos,
                              double avaliacaoMedia, int totalAvaliacoes,
                              int total5Estrelas, int total4Estrelas, int total3Estrelas,
                              int total2Estrelas, int total1Estrelas,
                              int idUsuario, String nomeUsuario, String fotoPerfilUsuario) {
        this.id = id;
        this.nome = nome;
        this.descricao = descricao;
        this.descricaoDetalhada = descricaoDetalhada;
        this.telefone = telefone;
        this.tipo = tipo;
        this.status = status;
        this.fotos = fotos;
        this.avaliacaoMedia = avaliacaoMedia;
        this.totalAvaliacoes = totalAvaliacoes;
        this.total5Estrelas = total5Estrelas;
        this.total4Estrelas = total4Estrelas;
        this.total3Estrelas = total3Estrelas;
        this.total2Estrelas = total2Estrelas;
        this.total1Estrelas = total1Estrelas;
        this.idUsuario = idUsuario;
        this.nomeUsuario = nomeUsuario;
        this.fotoPerfilUsuario = fotoPerfilUsuario;
    }

    // Getters
    public int getId() { return id; }
    public String getNome() { return nome; }
    public String getDescricao() { return descricao; }
    public String getDescricaoDetalhada() { return descricaoDetalhada; }
    public String getTelefone() { return telefone; }
    public String getTipo() { return tipo; }
    public String getStatus() { return status; }
    public List<FotoAnuncioDTO> getFotos() { return fotos; }
    public double getAvaliacaoMedia() { return avaliacaoMedia; }
    public int getTotalAvaliacoes() { return totalAvaliacoes; }

    public int getTotal5Estrelas() { return total5Estrelas; }
    public int getTotal4Estrelas() { return total4Estrelas; }
    public int getTotal3Estrelas() { return total3Estrelas; }
    public int getTotal2Estrelas() { return total2Estrelas; }
    public int getTotal1Estrelas() { return total1Estrelas; }

    public int getIdUsuario() { return idUsuario; }
    public String getNomeUsuario() { return nomeUsuario; }
    public String getFotoPerfilUsuario() { return fotoPerfilUsuario; }
}
