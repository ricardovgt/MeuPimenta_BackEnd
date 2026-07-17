package com.connecta.entity;

public class Usuario {
    private int id;
    private String nome;
    private String email;
    private String senha;
    private String tipoConta;

    // Getters
    public String getNome() { return nome; }
    public String getSenha() { return senha; }
    public String getEmail() { return email; }
    public int getId() { return id; }
    public String getTipoConta() { return tipoConta; }
    
    //Setters
    public void setNome(String nome) { this.nome = nome; }
    public void setSenha(String senha) { this.senha = senha; }
    public void setEmail(String email) { this.email = email; }
    public void setId(int id) { this.id = id; }
    public void setTipoConta(String tipoConta) { this.tipoConta = tipoConta; }
}
