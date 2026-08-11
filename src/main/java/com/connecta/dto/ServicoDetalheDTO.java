package com.connecta.dto;

import java.util.List;

public class ServicoDetalheDTO {
    private int id;
    private String nome;
    private String descricao;
    private String descricaoDetalhada;
    private String telefone;
    private List<FotoServicoDTO> fotos;
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

    public ServicoDetalheDTO(int id, String nome, String descricao, String descricaoDetalhada,
                              String telefone, List<FotoServicoDTO> fotos,
                              double avaliacaoMedia, int totalAvaliacoes,
                              int total5Estrelas, int total4Estrelas, int total3Estrelas,
                              int total2Estrelas, int total1Estrelas,
                              int idUsuario, String nomeUsuario) {
        this.id = id;
        this.nome = nome;
        this.descricao = descricao;
        this.descricaoDetalhada = descricaoDetalhada;
        this.telefone = telefone;
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
    }

    // Getters
    public int getId() { return id; }
    public String getNome() { return nome; }
    public String getDescricao() { return descricao; }
    public String getDescricaoDetalhada() { return descricaoDetalhada; }
    public String getTelefone() { return telefone; }
    public List<FotoServicoDTO> getFotos() { return fotos; }
    public double getAvaliacaoMedia() { return avaliacaoMedia; }
    public int getTotalAvaliacoes() { return totalAvaliacoes; }

    public int getTotal5Estrelas() { return total5Estrelas; }
    public int getTotal4Estrelas() { return total4Estrelas; }
    public int getTotal3Estrelas() { return total3Estrelas; }
    public int getTotal2Estrelas() { return total2Estrelas; }
    public int getTotal1Estrelas() { return total1Estrelas; }

    public int getIdUsuario() { return idUsuario; }
    public String getNomeUsuario() { return nomeUsuario; }
}