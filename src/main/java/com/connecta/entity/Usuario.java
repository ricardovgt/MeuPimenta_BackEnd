package com.connecta.entity;

public class Usuario {
    private int id;
    private String nome;
    private String email;
    private String senha;
    private String tipoConta;
    private String foto_perfil;
    private String status;

    // Getters
    public String getNome() { return nome; }
    public String getSenha() { return senha; }
    public String getEmail() { return email; }
    public int getId() { return id; }
    public String getTipoConta() { return tipoConta; }
    public String getFotoPerfil() { return foto_perfil; }
    public String getStatus() { return status; }

    // Setters
    public void setNome(String nome) { this.nome = nome; }
    public void setSenha(String senha) { this.senha = senha; }
    public void setEmail(String email) { this.email = email; }
    public void setId(int id) { this.id = id; }
    public void setTipoConta(String tipoConta) { this.tipoConta = tipoConta; }
    public void setFotoPerfil(String foto_perfil) { this.foto_perfil = foto_perfil; }
    public void setStatus(String status) { this.status = status; }
}