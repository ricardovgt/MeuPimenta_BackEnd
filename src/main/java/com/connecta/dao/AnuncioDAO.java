package com.connecta.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.connecta.conexao.Conexao;
import com.connecta.dto.FotoAnuncioDTO;
import com.connecta.dto.MeusAnunciosDTO;
import com.connecta.dto.AnuncioCardDTO;
import com.connecta.dto.AnuncioDetalheDTO;
import com.connecta.entity.Anuncio;

public class AnuncioDAO {

    private static final double MEDIA_GLOBAL_AVALIACOES = 4.0;
    private static final int PESO_MINIMO_AVALIACOES = 10;

	public static boolean cadastrar(Anuncio anuncio, List<String> fotosBase64) {
	    String sql = "INSERT INTO anuncios (id_usuario, nome, descricao, telefone, descricao_detalhada, tipo) VALUES (?, ?, ?, ?, ?, ?)";
	    Connection conn = null;

	    try {
	        conn = Conexao.getConnection();
	        conn.setAutoCommit(false);

	        try (PreparedStatement prepare = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
	            prepare.setInt(1, anuncio.getIdUsuario());
	            prepare.setString(2, anuncio.getNome());
	            prepare.setString(3, anuncio.getDescricao());
	            prepare.setString(4, anuncio.getTelefone());
	            prepare.setString(5, anuncio.getDescricaoDetalhada());
	            prepare.setString(6, anuncio.getTipo());

	            if (prepare.executeUpdate() == 0) {
	                conn.rollback();
	                return false;
	            }

	            try (ResultSet chaves = prepare.getGeneratedKeys()) {
	                if (chaves.next()) {
	                    FotoAnuncioDAO.salvarFotos(conn, chaves.getInt(1), fotosBase64);
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
	
	public static boolean deletar(int idAnuncio, int idUsuarioDono) {
        String sqlVerificarDono =
                "SELECT id FROM anuncios WHERE id = ? AND id_usuario = ? FOR UPDATE";
        String sqlExcluirDenuncias = "DELETE FROM denuncias WHERE id_anuncio = ?";
        String sqlExcluirAnuncio = "DELETE FROM anuncios WHERE id = ? AND id_usuario = ?";
        Connection conn = null;

        try {
            conn = Conexao.getConnection();
            if (conn == null) return false;
            conn.setAutoCommit(false);

            try (PreparedStatement ps = conn.prepareStatement(sqlVerificarDono)) {
                ps.setInt(1, idAnuncio);
                ps.setInt(2, idUsuarioDono);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        conn.rollback();
                        return false;
                    }
                }
            }

            try (PreparedStatement ps = conn.prepareStatement(sqlExcluirDenuncias)) {
                ps.setInt(1, idAnuncio);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = conn.prepareStatement(sqlExcluirAnuncio)) {
                ps.setInt(1, idAnuncio);
                ps.setInt(2, idUsuarioDono);
                if (ps.executeUpdate() == 0) {
                    conn.rollback();
                    return false;
                }
            }

            conn.commit();
            return true;

        } catch (SQLException e) {
            e.printStackTrace();
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) { e.printStackTrace(); }
            }
        }
    }
	
    //CONSULTAS
    
    // idUsuarioRequisitante: id de quem está pedindo o detalhe (ou -1/0 se anônimo/não logado).
    // Anúncios não ativos só são retornados se o requisitante for o próprio dono.
    public static AnuncioDetalheDTO pegarAnuncioDetalhe(int idAnuncio, int idUsuarioRequisitante) {
    String sql =
        "SELECT s.id, s.nome, s.descricao, s.descricao_detalhada, s.telefone, s.tipo, s.status, " +
        "s.avaliacao_media, s.total_avaliacoes, " +
        "u.id AS id_usuario, u.nome AS nome_usuario, u.foto_perfil, " +
        "SUM(CASE WHEN a.nota = 5 THEN 1 ELSE 0 END) AS total_5, " +
        "SUM(CASE WHEN a.nota = 4 THEN 1 ELSE 0 END) AS total_4, " +
        "SUM(CASE WHEN a.nota = 3 THEN 1 ELSE 0 END) AS total_3, " +
        "SUM(CASE WHEN a.nota = 2 THEN 1 ELSE 0 END) AS total_2, " +
        "SUM(CASE WHEN a.nota = 1 THEN 1 ELSE 0 END) AS total_1 " +
        "FROM anuncios s " +
        "INNER JOIN usuarios u ON s.id_usuario = u.id " +
        "LEFT JOIN avaliacoes a ON a.id_anuncio = s.id " +
        "WHERE s.id = ? AND (s.status = 'ATIVO' OR s.id_usuario = ?) " +
        "GROUP BY s.id, s.nome, s.descricao, s.descricao_detalhada, s.telefone, s.tipo, s.status, " +
        "s.avaliacao_media, s.total_avaliacoes, u.id, u.nome, u.foto_perfil";

    try (Connection conn = Conexao.getConnection();
         PreparedStatement prepare = conn.prepareStatement(sql)) {

        prepare.setInt(1, idAnuncio);
        prepare.setInt(2, idUsuarioRequisitante);

        try (ResultSet r = prepare.executeQuery()) {
            if (r.next()) {
                List<FotoAnuncioDTO> fotos = FotoAnuncioDAO.buscarPorAnuncio(idAnuncio);

                return new AnuncioDetalheDTO(
                    r.getInt("id"),
                    r.getString("nome"),
                    r.getString("descricao"),
                    r.getString("descricao_detalhada"),
                    r.getString("telefone"),
                    r.getString("tipo"),
                    r.getString("status"),
                    fotos,
                    r.getDouble("avaliacao_media"),
                    r.getInt("total_avaliacoes"),
                    r.getInt("total_5"),
                    r.getInt("total_4"),
                    r.getInt("total_3"),
                    r.getInt("total_2"),
                    r.getInt("total_1"),
                    r.getInt("id_usuario"),
                    r.getString("nome_usuario"),
                    r.getString("foto_perfil")
                );
            }
        }
    } catch (SQLException e) {
        e.printStackTrace();
    }
    return null;
    }
    
    // Retorna todos os anúncios do usuário; BANIDO é exibido como estado somente leitura.
    public static List<MeusAnunciosDTO> listarPorUsuario(int idUsuario) {
        List<MeusAnunciosDTO> lista = new ArrayList<>();

        String sql =
            "SELECT s.id, s.nome, s.descricao, s.descricao_detalhada, s.telefone, s.tipo, s.status, " +
            "f.foto_base64 AS foto_capa, u.nome AS nome_usuario " +
            "FROM anuncios s " +
            "JOIN usuarios u ON s.id_usuario = u.id " +
            "LEFT JOIN fotos_anuncio f ON f.id_anuncio = s.id AND f.is_capa = TRUE " +
            "WHERE s.id_usuario = ? " +
            "ORDER BY s.id DESC";

        try (Connection conn = Conexao.getConnection();
             PreparedStatement prepare = conn.prepareStatement(sql)) {

            prepare.setInt(1, idUsuario);

            try (ResultSet r = prepare.executeQuery()) {
                while (r.next()) {
                    lista.add(new MeusAnunciosDTO(
                        r.getInt("id"),
                        r.getString("nome"),
                        r.getString("descricao"),
                        r.getString("descricao_detalhada"),
                        r.getString("telefone"),
                        r.getString("tipo"),
                        r.getString("status"),
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
    
    public static List<AnuncioCardDTO> buscarAnunciosCard(String busca, boolean topAvaliacoes) {
        return buscarAnunciosCard(busca, topAvaliacoes, null);
    }

    public static List<AnuncioCardDTO> buscarAnunciosCard(
            String busca, boolean topAvaliacoes, String tipo) {
        List<AnuncioCardDTO> lista = new ArrayList<>();

        String tipoFiltro = null;
        if ("SERVICO".equalsIgnoreCase(tipo)) {
            tipoFiltro = "SERVICO";
        } else if ("COMERCIO".equalsIgnoreCase(tipo)) {
            tipoFiltro = "COMERCIO";
        }

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

        sql.append("SELECT s.id, s.nome, s.descricao, f.foto_base64 AS foto_capa, ")
           .append("s.avaliacao_media, u.nome AS nome_usuario, ")
           .append("((COALESCE(s.total_avaliacoes, 0) * COALESCE(s.avaliacao_media, 0)) + ")
           .append(PESO_MINIMO_AVALIACOES).append(" * ").append(MEDIA_GLOBAL_AVALIACOES)
           .append(") / (COALESCE(s.total_avaliacoes, 0) + ")
           .append(PESO_MINIMO_AVALIACOES).append(") AS nota_ponderada ");

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

        sql.append("FROM anuncios s ")
           .append("JOIN usuarios u ON s.id_usuario = u.id ")
           .append("LEFT JOIN fotos_anuncio f ON f.id_anuncio = s.id AND f.is_capa = TRUE");

        if (!palavrasChave.isEmpty()) {
            sql.append(" WHERE s.status = 'ATIVO'");
            if (tipoFiltro != null) {
                sql.append(" AND s.tipo = ?");
            }
            sql.append(") AS resultado WHERE pontuacao > 0 ");
            
            if (topAvaliacoes) {
                sql.append("ORDER BY pontuacao DESC, nota_ponderada DESC");
            } else {
                sql.append("ORDER BY pontuacao DESC");
            }
        } else {
            sql.append(" WHERE s.status = 'ATIVO'");
            if (tipoFiltro != null) {
                sql.append(" AND s.tipo = ?");
            }
            sql.append(" ");
            if (topAvaliacoes) {
                sql.append("ORDER BY nota_ponderada DESC");
            } else {
                sql.append("ORDER BY s.id DESC"); 
            }
        }

        try (Connection conn = Conexao.getConnection();
             PreparedStatement prepare = conn.prepareStatement(sql.toString())) {

            int paramIndex = 1;
            if (!palavrasChave.isEmpty()) {
                for (String palavra : palavrasChave) {
                    prepare.setString(paramIndex++, palavra); 
                    prepare.setString(paramIndex++, palavra); 
                    prepare.setString(paramIndex++, palavra); 
                }
            }

            if (tipoFiltro != null) {
                prepare.setString(paramIndex, tipoFiltro);
            }

            try (ResultSet r = prepare.executeQuery()) {
                while (r.next()) {
                    lista.add(new AnuncioCardDTO(
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
    
    //MODIFCAÇÕES DE DADOS

    public static boolean atualizar(Anuncio anuncio, int idUsuarioDono,
                                    List<String> fotosBase64, String novoStatus) {
        String sql = "UPDATE anuncios SET nome = ?, descricao = ?, telefone = ?, " +
                     "descricao_detalhada = ?, tipo = COALESCE(?, tipo), " +
                     "status = COALESCE(?, status) " +
                     "WHERE id = ? AND id_usuario = ? AND status <> 'BANIDO'";
        Connection conn = null;

        try {
            conn = Conexao.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, anuncio.getNome());
                ps.setString(2, anuncio.getDescricao());
                ps.setString(3, anuncio.getTelefone());
                ps.setString(4, anuncio.getDescricaoDetalhada());
                ps.setString(5, anuncio.getTipo());
                ps.setString(6, novoStatus);
                ps.setInt(7, anuncio.getId());
                ps.setInt(8, idUsuarioDono);

                if (ps.executeUpdate() == 0) {
                    conn.rollback();
                    return false; 
                }
            }

            // Campo ausente no PUT significa preservar as fotos atuais.
            if (fotosBase64 != null) {
                FotoAnuncioDAO.deletarFotosPorAnuncio(conn, anuncio.getId());
                FotoAnuncioDAO.salvarFotos(conn, anuncio.getId(), fotosBase64);
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
    
    public static boolean excluirTodosPorUsuario(int idUsuario) {
        String sqlExcluirDenunciasRecebidas =
                "DELETE d FROM denuncias d INNER JOIN anuncios a ON a.id = d.id_anuncio " +
                "WHERE a.id_usuario = ?";
        String sqlExcluirAnuncios = "DELETE FROM anuncios WHERE id_usuario = ?";
        Connection conn = null;

        try {
            conn = Conexao.getConnection();
            if (conn == null) return false;
            conn.setAutoCommit(false);

            try (PreparedStatement ps = conn.prepareStatement(sqlExcluirDenunciasRecebidas)) {
                ps.setInt(1, idUsuario);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = conn.prepareStatement(sqlExcluirAnuncios)) {
                ps.setInt(1, idUsuario);
                ps.executeUpdate();
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) { e.printStackTrace(); }
            }
        }
    }

    // Dono alterna o próprio anúncio entre ATIVO e OCULTO.
    public static boolean alterarStatus(int idAnuncio, int idUsuario, String novoStatus) {
        if (!"ATIVO".equals(novoStatus) && !"OCULTO".equals(novoStatus)) {
            return false;
        }

        String sql = "UPDATE anuncios SET status = ? WHERE id = ? AND id_usuario = ? AND status <> 'BANIDO'";

        try (Connection conn = Conexao.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, novoStatus);
            ps.setInt(2, idAnuncio);
            ps.setInt(3, idUsuario);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // Gatilho de moderação automática:
    // 1) incrementa denuncias do anúncio;
    // 2) se atingir 5, bane o anúncio;
    // 3) se o dono acumular 3 anúncios BANIDO, bane o usuário também.
    public static String registrarDenuncia(int idAnuncio, int idUsuario) {
        String sqlBuscarAnuncio =
                "SELECT id_usuario, status FROM anuncios WHERE id = ? FOR UPDATE";
        String sqlRegistrar = "INSERT INTO denuncias (id_anuncio, id_usuario) VALUES (?, ?)";
        String sqlAtualizarContagem = "UPDATE anuncios SET denuncias = "
                + "(SELECT COUNT(*) FROM denuncias WHERE id_anuncio = ?) WHERE id = ?";
        String sqlChecarDenuncias = "SELECT denuncias, id_usuario FROM anuncios WHERE id = ?";
        String sqlBanirAnuncio = "UPDATE anuncios SET status = 'BANIDO' WHERE id = ?";
        String sqlContarAnunciosBanidos = "SELECT COUNT(*) FROM anuncios WHERE id_usuario = ? AND status = 'BANIDO'";

        Connection conn = null;

        try {
            conn = Conexao.getConnection();
            if (conn == null) return "ERRO";
            conn.setAutoCommit(false);

            int idUsuarioDono;
            String statusAnuncio;

            // O bloqueio serializa denúncias simultâneas do mesmo anúncio.
            try (PreparedStatement ps = conn.prepareStatement(sqlBuscarAnuncio)) {
                ps.setInt(1, idAnuncio);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        conn.rollback();
                        return "NAO_ENCONTRADO";
                    }
                    idUsuarioDono = rs.getInt("id_usuario");
                    statusAnuncio = rs.getString("status");
                }
            }

            if (idUsuarioDono == idUsuario) {
                conn.rollback();
                return "PROPRIO";
            }

            if (!"ATIVO".equalsIgnoreCase(statusAnuncio)) {
                conn.rollback();
                return "INDISPONIVEL";
            }

            try {
                try (PreparedStatement ps = conn.prepareStatement(sqlRegistrar)) {
                    ps.setInt(1, idAnuncio);
                    ps.setInt(2, idUsuario);
                    ps.executeUpdate();
                }
            } catch (SQLException e) {
                if (e.getErrorCode() == 1062) {
                    conn.rollback();
                    return "DUPLICADA";
                }
                throw e;
            }

            try (PreparedStatement ps = conn.prepareStatement(sqlAtualizarContagem)) {
                ps.setInt(1, idAnuncio);
                ps.setInt(2, idAnuncio);
                ps.executeUpdate();
            }

            int denuncias = 0;
            int idUsuarioDonoConsultado = -1;

            try (PreparedStatement ps = conn.prepareStatement(sqlChecarDenuncias)) {
                ps.setInt(1, idAnuncio);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        denuncias = rs.getInt("denuncias");
                        idUsuarioDonoConsultado = rs.getInt("id_usuario");
                    }
                }
            }

            if (denuncias >= 5 && idUsuarioDonoConsultado != -1) {
                try (PreparedStatement ps = conn.prepareStatement(sqlBanirAnuncio)) {
                    ps.setInt(1, idAnuncio);
                    ps.executeUpdate();
                }

                int totalBanidos = 0;
                try (PreparedStatement ps = conn.prepareStatement(sqlContarAnunciosBanidos)) {
                    ps.setInt(1, idUsuarioDonoConsultado);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            totalBanidos = rs.getInt(1);
                        }
                    }
                }

                if (totalBanidos >= 3) {
                    try (PreparedStatement ps = conn.prepareStatement("UPDATE usuarios SET status = 'BANIDO' WHERE id = ?")) {
                        ps.setInt(1, idUsuarioDonoConsultado);
                        ps.executeUpdate();
                    }
                    try (PreparedStatement ps = conn.prepareStatement("UPDATE anuncios SET status = 'BANIDO' WHERE id_usuario = ?")) {
                        ps.setInt(1, idUsuarioDonoConsultado);
                        ps.executeUpdate();
                    }
                }
            }

            conn.commit();
            return "SUCESSO";

        } catch (SQLException e) {
            e.printStackTrace();
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            return "ERRO";
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) { e.printStackTrace(); }
            }
        }
    }
}
