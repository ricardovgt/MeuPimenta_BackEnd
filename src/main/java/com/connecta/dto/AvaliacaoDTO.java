package com.connecta.dto;

public class AvaliacaoDTO {
    private int id;
    private double nota;
    private String comentario;
    private String dataAvaliacao;
    private int idUsuario;
    private String nomeUsuario;
    private String fotoPerfilUsuario;
    
    public AvaliacaoDTO(int id, double nota, String comentario, String dataAvaliacao,
                         int idUsuario, String nomeUsuario, String fotoPerfilUsuario) {
        this.id = id;
        this.nota = nota;
        this.comentario = comentario;
        this.dataAvaliacao = dataAvaliacao;
        this.idUsuario = idUsuario;
        this.nomeUsuario = nomeUsuario;
        this.fotoPerfilUsuario = fotoPerfilUsuario; 
    }

    public int getId() { return id; }
    public double getNota() { return nota; }
    public String getComentario() { return comentario; }
    public String getDataAvaliacao() { return dataAvaliacao; }
    public int getIdUsuario() { return idUsuario; }
    public String getNomeUsuario() { return nomeUsuario; }
    public String getFotoPerfilUsuario() { return fotoPerfilUsuario; }
}