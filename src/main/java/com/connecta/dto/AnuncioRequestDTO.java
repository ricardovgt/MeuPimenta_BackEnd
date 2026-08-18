package com.connecta.dto;

import java.util.List;

// Usado apenas para LER o JSON que o front-end manda no cadastro (POST) e na edição (PUT).
// Não é persistido diretamente - o Servlet usa os dados daqui para montar um Anuncio
// e repassa a lista de fotos separadamente para o DAO.
public class AnuncioRequestDTO {
    private int id;
    private String nome;
    private String descricao;
    private String descricaoDetalhada;
    private String telefone;
    private String tipo;
    private String status;
    private List<String> fotos;

    // Getters e Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public String getDescricaoDetalhada() { return descricaoDetalhada; }
    public void setDescricaoDetalhada(String descricaoDetalhada) { this.descricaoDetalhada = descricaoDetalhada; }

    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public List<String> getFotos() { return fotos; }
    public void setFotos(List<String> fotos) { this.fotos = fotos; }
}