package com.connecta.dto;

public class UsuarioResponseDTO {
    private int id;
    private String nome;
    private String email;
    private String tipoConta;
    private String fotoPerfil;

    public UsuarioResponseDTO(int id, String nome, String email, String tipoConta, String fotoPerfil) {
        this.id = id;
        this.nome = nome;
        this.email = email;
        this.tipoConta = tipoConta;
        this.fotoPerfil = fotoPerfil;
    }

    // Getters
    public int getId() { return id; }
    public String getNome() { return nome; }
    public String getEmail() { return email; }
    public String getTipoConta() { return tipoConta; }
    public String getFotoPerfil() { return fotoPerfil; }
}	