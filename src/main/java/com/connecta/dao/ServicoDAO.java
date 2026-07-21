package com.connecta.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.connecta.conexao.Conexao;
import com.connecta.dto.ServicoCardDTO;
import com.connecta.dto.ServicoDetalheDTO;
import com.connecta.entity.Servico;

public class ServicoDAO {

	public static boolean cadastrar(Servico servico) {
	    String sql = "INSERT INTO servicos (id_usuario, nome, descricao, telefone, bairro, foto_url, descricao_detalhada) VALUES (?, ?, ?, ?, ?, ?, ?)";
	    
	    try (Connection conn = Conexao.getConnection(); 
	         PreparedStatement prepare = conn.prepareStatement(sql)) {
	         
	        prepare.setInt(1, servico.getIdUsuario());
	        prepare.setString(2, servico.getNome());
	        prepare.setString(3, servico.getDescricao());
	        prepare.setString(4, servico.getTelefone());
	        prepare.setString(5, servico.getBairro());
	        prepare.setString(6, servico.getFotoUrl());
	        prepare.setString(7, servico.getDescricaoDetalhada()); 
	        
	        return prepare.executeUpdate() > 0;
	        
	    } catch (SQLException e) {
	        e.printStackTrace();
	    }
	    return false;
	}

    public static List<ServicoCardDTO> buscarServicosCard(String bairro, boolean topAvaliacoes) {
        List<ServicoCardDTO> lista = new ArrayList<>();

        StringBuilder sql = new StringBuilder(
            "SELECT s.id, s.nome, s.descricao, s.foto_url, s.bairro, " +
            "u.nome AS nome_usuario " +
            "FROM servicos s JOIN usuarios u ON s.id_usuario = u.id WHERE 1=1"
        );

        if (bairro != null && !bairro.trim().isEmpty()) {
            sql.append(" AND s.bairro = ?");
        }
        if (topAvaliacoes) {
            sql.append(" ORDER BY s.avaliacao_media DESC");
        }

        try (Connection conn = Conexao.getConnection();
             PreparedStatement prepare = conn.prepareStatement(sql.toString())) {

            int paramIndex = 1;
            if (bairro != null && !bairro.trim().isEmpty()) {
                prepare.setString(paramIndex++, bairro.trim());
            }

            try (ResultSet r = prepare.executeQuery()) {
                while (r.next()) {
                    lista.add(new ServicoCardDTO(
                        r.getInt("id"),
                        r.getString("nome"),
                        r.getString("descricao"),
                        r.getString("foto_url"),
                        r.getString("bairro"),
                        r.getString("nome_usuario")
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }
    
    public static ServicoDetalheDTO pegarServicoDetalhe(int idServico) {
        String sql =
            "SELECT s.id, s.nome, s.descricao, s.descricao_detalhada, s.telefone, s.bairro, " +
            "s.foto_url, s.avaliacao_media, s.total_avaliacoes, " +
            "u.id AS id_usuario, u.nome AS nome_usuario " +
            "FROM servicos s JOIN usuarios u ON s.id_usuario = u.id " +
            "WHERE s.id = ?";

        try (Connection conn = Conexao.getConnection();
             PreparedStatement prepare = conn.prepareStatement(sql)) {

            prepare.setInt(1, idServico);

            try (ResultSet r = prepare.executeQuery()) {
                if (r.next()) {
                    return new ServicoDetalheDTO(
                        r.getInt("id"),
                        r.getString("nome"),
                        r.getString("descricao"),
                        r.getString("descricao_detalhada"),
                        r.getString("telefone"),
                        r.getString("bairro"),
                        r.getString("foto_url"),
                        r.getDouble("avaliacao_media"),
                        r.getInt("total_avaliacoes"),
                        r.getInt("id_usuario"),
                        r.getString("nome_usuario")
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}