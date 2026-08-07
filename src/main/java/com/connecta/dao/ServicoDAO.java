package com.connecta.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.connecta.conexao.Conexao;
import com.connecta.dto.MeusServicosDTO;
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

    public static List<ServicoCardDTO> buscarServicosCard(String busca, boolean topAvaliacoes) {
        List<ServicoCardDTO> lista = new ArrayList<>();

        // 1. Processa o texto digitado pelo usuário e filtra as preposições
        List<String> palavrasChave = new ArrayList<>();
        if (busca != null && !busca.trim().isEmpty()) {
            // Quebra o texto por espaços
            String[] palavras = busca.toLowerCase().split("\\s+");
            List<String> preposicoes = java.util.Arrays.asList(
                "a", "ante", "após", "ate", "até", "com", "contra", "de", 
                "desde", "em", "entre", "para", "perante", "por", "sem", 
                "sob", "sobre", "tras", "trás"
            );

            for (String palavra : palavras) {
                // Remove caracteres especiais grudados na palavra
                palavra = palavra.replaceAll("[^a-záéíóúãõç]", "");
                
                // Se não for uma preposição e tiver mais de 1 letra, vira palavra-chave
                if (!preposicoes.contains(palavra) && palavra.length() > 1) {
                    palavrasChave.add("%" + palavra + "%");
                }
            }
        }

        StringBuilder sql = new StringBuilder();

        // 2. Monta a consulta SQL baseada em Pontuação (Score)
        if (!palavrasChave.isEmpty()) {
            sql.append("SELECT * FROM ( ");
        }

        sql.append("SELECT s.id, s.nome, s.descricao, s.foto_url, s.bairro, s.avaliacao_media, u.nome AS nome_usuario ");

        // Se houver palavras-chave, cria a coluna dinâmica de pontuação
        if (!palavrasChave.isEmpty()) {
            sql.append(", (");
            for (int i = 0; i < palavrasChave.size(); i++) {
                if (i > 0) sql.append(" + ");
                // Peso 3 (Nome), Peso 2 (Descricao), Peso 1 (Descricao Detalhada)
                sql.append("(IF(LOWER(s.nome) LIKE ?, 3, 0) + ")
                   .append("IF(LOWER(s.descricao) LIKE ?, 2, 0) + ")
                   .append("IF(LOWER(s.descricao_detalhada) LIKE ?, 1, 0))");
            }
            sql.append(") AS pontuacao ");
        }

        sql.append("FROM servicos s JOIN usuarios u ON s.id_usuario = u.id");

        // 3. Filtros finais e Ordenação
        if (!palavrasChave.isEmpty()) {
            sql.append(") AS resultado WHERE pontuacao > 0 ");
            
            if (topAvaliacoes) {
                // Ordena por maior relevância e, em caso de empate, maior nota
                sql.append("ORDER BY pontuacao DESC, avaliacao_media DESC");
            } else {
                sql.append("ORDER BY pontuacao DESC");
            }
        } else {
            sql.append(" WHERE 1=1 "); // Caso o usuário não tenha digitado nada
            if (topAvaliacoes) {
                sql.append("ORDER BY s.avaliacao_media DESC");
            } else {
                sql.append("ORDER BY s.id DESC"); // Recentes primeiro se não houver filtro
            }
        }

        // 4. Executa no Banco de Dados de forma segura
        try (Connection conn = Conexao.getConnection();
             PreparedStatement prepare = conn.prepareStatement(sql.toString())) {

            // Se existirem palavras-chave, injetamos elas nos "?" (3 vezes para cada palavra)
            if (!palavrasChave.isEmpty()) {
                int paramIndex = 1;
                for (String palavra : palavrasChave) {
                    prepare.setString(paramIndex++, palavra); // Para o IF do nome
                    prepare.setString(paramIndex++, palavra); // Para o IF da descricao
                    prepare.setString(paramIndex++, palavra); // Para o IF da descricao_detalhada
                }
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
            "u.id AS id_usuario, u.nome AS nome_usuario, " +
            "COUNT(CASE WHEN ROUND(a.nota) = 5 THEN 1 END) AS total_5, " +
            "COUNT(CASE WHEN ROUND(a.nota) = 4 THEN 1 END) AS total_4, " +
            "COUNT(CASE WHEN ROUND(a.nota) = 3 THEN 1 END) AS total_3, " +
            "COUNT(CASE WHEN ROUND(a.nota) = 2 THEN 1 END) AS total_2, " +
            "COUNT(CASE WHEN ROUND(a.nota) = 1 THEN 1 END) AS total_1 " +
            "FROM servicos s " +
            "JOIN usuarios u ON s.id_usuario = u.id " +
            "LEFT JOIN avaliacoes a ON a.id_servico = s.id " +
            "WHERE s.id = ? " +
            "GROUP BY s.id, u.id";

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
                        r.getInt("total_5"),
                        r.getInt("total_4"),
                        r.getInt("total_3"),
                        r.getInt("total_2"),
                        r.getInt("total_1"),
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
    
    public static List<MeusServicosDTO> listarPorUsuario(int idUsuario) {
        List<MeusServicosDTO> lista = new ArrayList<>();

        String sql =
            "SELECT s.id, s.nome, s.descricao, s.descricao_detalhada, s.telefone, " +
            "s.foto_url, s.bairro, u.nome AS nome_usuario " +
            "FROM servicos s " +
            "JOIN usuarios u ON s.id_usuario = u.id " +
            "WHERE s.id_usuario = ? " +
            "ORDER BY s.id DESC";

        try (Connection conn = Conexao.getConnection();
             PreparedStatement prepare = conn.prepareStatement(sql)) {

            prepare.setInt(1, idUsuario);

            try (ResultSet r = prepare.executeQuery()) {
                while (r.next()) {
                    lista.add(new MeusServicosDTO(
                        r.getInt("id"),
                        r.getString("nome"),
                        r.getString("descricao"),
                        r.getString("descricao_detalhada"),
                        r.getString("telefone"),
                        r.getString("bairro"),
                        r.getString("foto_url"),
                        r.getString("nome_usuario")
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }

    public static boolean atualizar(Servico servico, int idUsuarioDono) {
        String sql = "UPDATE servicos SET nome = ?, descricao = ?, telefone = ?, " +
                     "bairro = ?, foto_url = ?, descricao_detalhada = ? " +
                     "WHERE id = ? AND id_usuario = ?";

        try (Connection conn = Conexao.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, servico.getNome());
            ps.setString(2, servico.getDescricao());
            ps.setString(3, servico.getTelefone());
            ps.setString(4, servico.getBairro());
            ps.setString(5, servico.getFotoUrl());
            ps.setString(6, servico.getDescricaoDetalhada());
            ps.setInt(7, servico.getId());
            ps.setInt(8, idUsuarioDono); // Garantia de segurança: só edita se for o dono!

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // EXCLUI O SERVIÇO, GARANTINDO QUE SOMENTE O DONO (id_usuario) POSSA APAGÁ-LO
    public static boolean deletar(int idServico, int idUsuarioDono) {
        String sql = "DELETE FROM servicos WHERE id = ? AND id_usuario = ?";

        try (Connection conn = Conexao.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, idServico);
            ps.setInt(2, idUsuarioDono); // Garantia de segurança: só apaga se for o dono!

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}