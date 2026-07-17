package com.connecta.entity;

import java.time.LocalDateTime;

public class Avaliacao {
    private int id;
    private int idServico;
    private int idUsuario;
    private double nota;
    private LocalDateTime dataAvaliacao;

    // Getters
    public int getId() { return id; }
    public int getIdServico() { return idServico; }
    public int getIdUsuario() { return idUsuario; }
    public double getNota() { return nota; }
    public LocalDateTime getDataAvaliacao() { return dataAvaliacao; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setIdServico(int idServico) { this.idServico = idServico; }
    public void setIdUsuario(int idUsuario) { this.idUsuario = idUsuario; }
    public void setNota(double nota) { this.nota = nota; }
    public void setDataAvaliacao(LocalDateTime dataAvaliacao) { this.dataAvaliacao = dataAvaliacao; }
}