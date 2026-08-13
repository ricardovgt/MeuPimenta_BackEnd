package com.connecta.dto;

public class UsuarioRequestDTO {
    private String nome;
    private String email;
    private String tipoConta;
    private String fotoPerfil;
    private String senhaAtual;
    private String novaSenha;
    private String confirmarNovaSenha;

    // Getters e Setters
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTipoConta() { return tipoConta; }
    public void setTipoConta(String tipoConta) { this.tipoConta = tipoConta; }

    public String getFotoPerfil() { return fotoPerfil; }
    public void setFotoPerfil(String fotoPerfil) { this.fotoPerfil = fotoPerfil; }

    public String getSenhaAtual() { return senhaAtual; }
    public void setSenhaAtual(String senhaAtual) { this.senhaAtual = senhaAtual; }

    public String getNovaSenha() { return novaSenha; }
    public void setNovaSenha(String novaSenha) { this.novaSenha = novaSenha; }

    public String getConfirmarNovaSenha() { return confirmarNovaSenha; }
    public void setConfirmarNovaSenha(String confirmarNovaSenha) { this.confirmarNovaSenha = confirmarNovaSenha; }
}