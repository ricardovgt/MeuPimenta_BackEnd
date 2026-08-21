package com.connecta.dto;

import java.util.List;

public class AnunciosPaginadosDTO {
    private final List<AnuncioPublicoDTO> anuncios;
    private final int paginaAtual;
    private final int limite;
    private final long totalAnuncios;
    private final long totalPaginas;
    private final boolean temPaginaAnterior;
    private final boolean temProximaPagina;

    public AnunciosPaginadosDTO(List<AnuncioPublicoDTO> anuncios, int paginaAtual,
                               int limite, long totalAnuncios) {
        this.anuncios = anuncios;
        this.paginaAtual = paginaAtual;
        this.limite = limite;
        this.totalAnuncios = totalAnuncios;
        this.totalPaginas = totalAnuncios == 0
                ? 0
                : (totalAnuncios + limite - 1L) / limite;
        this.temPaginaAnterior = paginaAtual > 1;
        this.temProximaPagina = paginaAtual < totalPaginas;
    }

    public List<AnuncioPublicoDTO> getAnuncios() { return anuncios; }
    public int getPaginaAtual() { return paginaAtual; }
    public int getLimite() { return limite; }
    public long getTotalAnuncios() { return totalAnuncios; }
    public long getTotalPaginas() { return totalPaginas; }
    public boolean isTemPaginaAnterior() { return temPaginaAnterior; }
    public boolean isTemProximaPagina() { return temProximaPagina; }
}
