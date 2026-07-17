package com.connecta.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.connecta.conexao.Conexao;
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

    public static List<Servico> buscarServicos(String bairro, boolean topAvaliacoes) {
        List<Servico> lista = new ArrayList<>();
        
        // Uso do 1=1 facilita a adição dinâmica de cláusulas AND
        StringBuilder sql = new StringBuilder("SELECT * FROM servicos WHERE 1=1");
        
        if (bairro != null && !bairro.trim().isEmpty()) {
            sql.append(" AND bairro = ?");
        }
        
        if (topAvaliacoes) {
            sql.append(" ORDER BY avaliacao_media DESC");
        }

        try (Connection conn = Conexao.getConnection(); 
             PreparedStatement prepare = conn.prepareStatement(sql.toString())) {
             
            int paramIndex = 1;
            if (bairro != null && !bairro.trim().isEmpty()) {
                prepare.setString(paramIndex++, bairro.trim());
            }
            
            try (ResultSet result = prepare.executeQuery()) {
                while (result.next()) {
                    Servico servico = new Servico();
                    servico.setId(result.getInt("id"));
                    servico.setIdUsuario(result.getInt("id_usuario"));
                    servico.setNome(result.getString("nome"));
                    servico.setDescricao(result.getString("descricao"));
                    servico.setTelefone(result.getString("telefone"));
                    servico.setBairro(result.getString("bairro"));
                    servico.setFotoUrl(result.getString("foto_url"));
                    servico.setAvaliacaoMedia(result.getDouble("avaliacao_media"));
                    servico.setDescricaoDetalhada(result.getString("descricao_detalhada"));
                    servico.setTotalAvaliacoes(result.getInt("total_avaliacoes"));
                    lista.add(servico);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return lista;
    }
    
    public static Servico pegarServico(int idServico) {
    	String sql = "SELECT * FROM servicos WHERE id = ? ";
    	
    	 try (Connection conn = Conexao.getConnection(); 
                PreparedStatement prepare = conn.prepareStatement(sql)) {
    		 	prepare.setInt(1, idServico);
                
                try (ResultSet result = prepare.executeQuery()) {
                    if (result.next()) {
                        Servico servico = new Servico();
                        servico.setId(result.getInt("id"));
                        servico.setIdUsuario(result.getInt("id_usuario"));
                        servico.setNome(result.getString("nome"));
                        servico.setDescricao(result.getString("descricao"));
                        servico.setTelefone(result.getString("telefone"));
                        servico.setBairro(result.getString("bairro"));
                        servico.setFotoUrl(result.getString("foto_url"));
                        servico.setAvaliacaoMedia(result.getDouble("avaliacao_media"));
                        servico.setDescricaoDetalhada(result.getString("descricao_detalhada"));
                        servico.setTotalAvaliacoes(result.getInt("total_avaliacoes"));
                        return servico;
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
                
            }
    	 return null;
    }
    
}