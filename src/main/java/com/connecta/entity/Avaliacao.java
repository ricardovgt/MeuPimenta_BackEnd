package com.connecta.entity;

import java.time.LocalDateTime;

public class Avaliacao {
    private int idAnuncio;
    private int idUsuario;
    private double nota;
    private String comentario;
    private LocalDateTime dataAvaliacao;

    // Getters
    public int getIdAnuncio() { return idAnuncio; }
    public int getIdUsuario() { return idUsuario; }
    public double getNota() { return nota; }
    public String getComentario() { return comentario; }
    public LocalDateTime getDataAvaliacao() { return dataAvaliacao; }

    // Setters
    public void setIdAnuncio(int idAnuncio) { this.idAnuncio = idAnuncio; }
    public void setIdUsuario(int idUsuario) { this.idUsuario = idUsuario; }
    public void setNota(double nota) { this.nota = nota; }
    public void setComentario(String comentario) { this.comentario = comentario; }
    public void setDataAvaliacao(LocalDateTime dataAvaliacao) { this.dataAvaliacao = dataAvaliacao; }
}
