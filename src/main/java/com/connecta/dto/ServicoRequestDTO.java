package com.connecta.dto;

import java.util.List;

// Usado apenas para LER o JSON que o front-end manda no cadastro (POST) e na edição (PUT).
// Não é persistido diretamente - o Servlet usa os dados daqui para montar um Servico
// e repassa a lista de fotos separadamente para o DAO.
public class ServicoRequestDTO {
    private int id; // só é usado na edição (PUT); no cadastro vem 0 e é ignorado
    private String nome;
    private String descricao;
    private String descricaoDetalhada;
    private String telefone;
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

    public List<String> getFotos() { return fotos; }
    public void setFotos(List<String> fotos) { this.fotos = fotos; }
}