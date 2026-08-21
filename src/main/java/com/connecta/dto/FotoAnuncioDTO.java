package com.connecta.dto;

public class FotoAnuncioDTO {
    private int id;
    private String fotoBase64;
    private boolean isCapa;

    public FotoAnuncioDTO(int id, String fotoBase64, boolean isCapa) {
        this.id = id;
        this.fotoBase64 = fotoBase64;
        this.isCapa = isCapa;
    }

 // Getters
    public int getId() { return id; }
    public String getFotoBase64() { return fotoBase64; }
    public boolean isCapa() { return isCapa; }

}
