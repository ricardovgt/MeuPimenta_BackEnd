package com.connecta.dto;

public class ServicoCardDTO {
    private int id;
    private String nome;
    private String descricao;
    private String fotoUrl;
    private String bairro;

    private String nomeUsuario;

    public ServicoCardDTO(int id, String nome, String descricao, String fotoUrl, String bairro, String nomeUsuario) {
        this.id = id;
        this.nome = nome;
        this.descricao = descricao;
        this.fotoUrl = fotoUrl;
        this.bairro = bairro;
        this.nomeUsuario = nomeUsuario;
    }
    
    //Getters
	public int getId() {return id;}
	public String getNome() {return nome;}
	public String getDescricao() {return descricao;}
	public String getFotoUrl() {return fotoUrl;}
	public String getBairro() {return bairro;}
	public String getNomeUsuario() {return nomeUsuario;}
    
}