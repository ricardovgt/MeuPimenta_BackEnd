package com.connecta.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.connecta.conexao.Conexao;
import com.connecta.dto.FotoServicoDTO;
import com.connecta.dto.MeusServicosDTO;
import com.connecta.dto.ServicoCardDTO;
import com.connecta.dto.ServicoDetalheDTO;
import com.connecta.entity.Servico;

public class ServicoDAO {

	public static boolean cadastrar(Servico servico, List<String> fotosBase64) {
	    String sql = "INSERT INTO servicos (id_usuario, nome, descricao, telefone, descricao_detalhada) VALUES (?, ?, ?, ?, ?)";
	    Connection conn = null;

	    try {
	        conn = Conexao.getConnection();
	        conn.setAutoCommit(false);

	        try (PreparedStatement prepare = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
	            prepare.setInt(1, servico.getIdUsuario());
	            prepare.setString(2, servico.getNome());
	            prepare.setString(3, servico.getDescricao());
	            prepare.setString(4, servico.getTelefone());
	            prepare.setString(5, servico.getDescricaoDetalhada());

	            if (prepare.executeUpdate() == 0) {
	                conn.rollback();
	                return false;
	            }

	            try (ResultSet chaves = prepare.getGeneratedKeys()) {
	                if (chaves.next()) {
	                    FotoServicoDAO.salvarFotos(conn, chaves.getInt(1), fotosBase64);
	                }
	            }
	        }

	        conn.commit();
	        return true;

	    } catch (SQLException e) {
	        e.printStackTrace();
	        if (conn != null) {
	            try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
	        }
	    } finally {
	        if (conn != null) {
	            try {
	                conn.setAutoCommit(true);
	                conn.close();
	            } catch (SQLException e) { e.printStackTrace(); }
	        }
	    }
	    return false;
	}

    public static List<ServicoCardDTO> buscarServicosCard(String busca, boolean topAvaliacoes) {
        List<ServicoCardDTO> lista = new ArrayList<>();

        List<String> palavrasChave = new ArrayList<>();
        if (busca != null && !busca.trim().isEmpty()) {
            String[] palavras = busca.toLowerCase().split("\\s+");
            List<String> preposicoes = java.util.Arrays.asList(
                "a", "ante", "após", "ate", "até", "com", "contra", "de", 
                "desde", "em", "entre", "para", "perante", "por", "sem", 
                "sob", "sobre", "tras", "trás"
            );

            for (String palavra : palavras) {
                palavra = palavra.replaceAll("[^a-záéíóúãõç]", "");
                if (!preposicoes.contains(palavra) && palavra.length() > 1) {
                    palavrasChave.add("%" + palavra + "%");
                }
            }
        }

        StringBuilder sql = new StringBuilder();

        if (!palavrasChave.isEmpty()) {
            sql.append("SELECT * FROM ( ");
        }

        sql.append("SELECT s.id, s.nome, s.descricao, f.foto_base64 AS foto_capa, s.avaliacao_media, u.nome AS nome_usuario ");

        if (!palavrasChave.isEmpty()) {
            sql.append(", (");
            for (int i = 0; i < palavrasChave.size(); i++) {
                if (i > 0) sql.append(" + ");
                sql.append("(IF(LOWER(s.nome) LIKE ?, 3, 0) + ")
                   .append("IF(LOWER(s.descricao) LIKE ?, 2, 0) + ")
                   .append("IF(LOWER(s.descricao_detalhada) LIKE ?, 1, 0))");
            }
            sql.append(") AS pontuacao ");
        }

        sql.append("FROM servicos s ")
           .append("JOIN usuarios u ON s.id_usuario = u.id ")
           .append("LEFT JOIN fotos_servico f ON f.id_servico = s.id AND f.is_capa = TRUE");

        if (!palavrasChave.isEmpty()) {
            sql.append(") AS resultado WHERE pontuacao > 0 ");
            
            if (topAvaliacoes) {
                sql.append("ORDER BY pontuacao DESC, avaliacao_media DESC");
            } else {
                sql.append("ORDER BY pontuacao DESC");
            }
        } else {
            sql.append(" WHERE 1=1 "); 
            if (topAvaliacoes) {
                sql.append("ORDER BY s.avaliacao_media DESC");
            } else {
                sql.append("ORDER BY s.id DESC"); 
            }
        }

        try (Connection conn = Conexao.getConnection();
             PreparedStatement prepare = conn.prepareStatement(sql.toString())) {

            if (!palavrasChave.isEmpty()) {
                int paramIndex = 1;
                for (String palavra : palavrasChave) {
                    prepare.setString(paramIndex++, palavra); 
                    prepare.setString(paramIndex++, palavra); 
                    prepare.setString(paramIndex++, palavra); 
                }
            }

            try (ResultSet r = prepare.executeQuery()) {
                while (r.next()) {
                    lista.add(new ServicoCardDTO(
                        r.getInt("id"),
                        r.getString("nome"),
                        r.getString("descricao"),
                        r.getString("foto_capa"),
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
            "SELECT s.id, s.nome, s.descricao, s.descricao_detalhada, s.telefone, " +
            "s.avaliacao_media, s.total_avaliacoes, " +
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
                    List<FotoServicoDTO> fotos = FotoServicoDAO.buscarPorServico(idServico);

                    return new ServicoDetalheDTO(
                        r.getInt("id"),
                        r.getString("nome"),
                        r.getString("descricao"),
                        r.getString("descricao_detalhada"),
                        r.getString("telefone"),
                        fotos,
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
            "f.foto_base64 AS foto_capa, u.nome AS nome_usuario " +
            "FROM servicos s " +
            "JOIN usuarios u ON s.id_usuario = u.id " +
            "LEFT JOIN fotos_servico f ON f.id_servico = s.id AND f.is_capa = TRUE " +
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
                        r.getString("foto_capa"),
                        r.getString("nome_usuario")
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }

    public static boolean atualizar(Servico servico, int idUsuarioDono, List<String> fotosBase64) {
        String sql = "UPDATE servicos SET nome = ?, descricao = ?, telefone = ?, " +
                     "descricao_detalhada = ? " +
                     "WHERE id = ? AND id_usuario = ?";
        Connection conn = null;

        try {
            conn = Conexao.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, servico.getNome());
                ps.setString(2, servico.getDescricao());
                ps.setString(3, servico.getTelefone());
                ps.setString(4, servico.getDescricaoDetalhada());
                ps.setInt(5, servico.getId());
                ps.setInt(6, idUsuarioDono); 

                if (ps.executeUpdate() == 0) {
                    conn.rollback();
                    return false; 
                }
            }

            FotoServicoDAO.deletarFotosPorServico(conn, servico.getId());
            FotoServicoDAO.salvarFotos(conn, servico.getId(), fotosBase64);

            conn.commit();
            return true;

        } catch (SQLException e) {
            e.printStackTrace();
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) { e.printStackTrace(); }
            }
        }
        return false;
    }

    public static boolean deletar(int idServico, int idUsuarioDono) {
        String sql = "DELETE FROM servicos WHERE id = ? AND id_usuario = ?";

        try (Connection conn = Conexao.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, idServico);
            ps.setInt(2, idUsuarioDono); 

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}