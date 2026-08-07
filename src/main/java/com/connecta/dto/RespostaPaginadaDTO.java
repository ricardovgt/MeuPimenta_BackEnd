package com.connecta.dto;
import java.util.List;

public class RespostaPaginadaDTO {
	int paginaAtual;
    int limite;
    int totalAvaliacoes;
    int totalPaginas;
    List<AvaliacaoDTO> avaliacoes;

    public RespostaPaginadaDTO(int paginaAtual, int limite, int totalAvaliacoes,
                        int totalPaginas, List<AvaliacaoDTO> avaliacoes) {
    	
        this.paginaAtual = paginaAtual;
        this.limite = limite;
        this.totalAvaliacoes = totalAvaliacoes;
        this.totalPaginas = totalPaginas;
        this.avaliacoes = avaliacoes;
    }
}