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
import com.connecta.dto.AnuncioPublicoDTO;
import com.connecta.dto.AnunciosPaginadosDTO;
import com.connecta.entity.Anuncio;

public class AnuncioDAO {

    private static final double MEDIA_GLOBAL_AVALIACOES = 4.0;
    private static final int PESO_MINIMO_AVALIACOES = 10;
    private static final int LIMITE_ANUNCIOS_POR_USUARIO = 5;

    public enum ResultadoCadastro {
        SUCESSO,
        LIMITE_ATINGIDO,
        ERRO
    }

	public static ResultadoCadastro cadastrar(Anuncio anuncio, List<String> fotosBase64) {
	    String sql = "INSERT INTO anuncios (id_usuario, nome, descricao, telefone, descricao_detalhada, tipo) VALUES (?, ?, ?, ?, ?, ?)";
	    Connection conn = null;

	    try {
	        conn = Conexao.getConnection();
	        if (conn == null) {
	            return ResultadoCadastro.ERRO;
	        }
	        conn.setAutoCommit(false);

	        // O bloqueio impede dois cadastros simultâneos de ultrapassarem o limite.
	        try (PreparedStatement ps = conn.prepareStatement(
	                "SELECT id FROM usuarios WHERE id = ? FOR UPDATE")) {
	            ps.setInt(1, anuncio.getIdUsuario());
	            try (ResultSet rs = ps.executeQuery()) {
	                if (!rs.next()) {
	                    conn.rollback();
	                    return ResultadoCadastro.ERRO;
	                }
	            }
	        }

	        try (PreparedStatement ps = conn.prepareStatement(
	                "SELECT COUNT(*) FROM anuncios WHERE id_usuario = ?")) {
	            ps.setInt(1, anuncio.getIdUsuario());
	            try (ResultSet rs = ps.executeQuery()) {
	                if (rs.next() && rs.getInt(1) >= LIMITE_ANUNCIOS_POR_USUARIO) {
	                    conn.rollback();
	                    return ResultadoCadastro.LIMITE_ATINGIDO;
	                }
	            }
	        }

	        try (PreparedStatement prepare = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
	            prepare.setInt(1, anuncio.getIdUsuario());
	            prepare.setString(2, anuncio.getNome());
	            prepare.setString(3, anuncio.getDescricao());
	            prepare.setString(4, anuncio.getTelefone());
	            prepare.setString(5, anuncio.getDescricaoDetalhada());
	            prepare.setString(6, anuncio.getTipo());

	            if (prepare.executeUpdate() == 0) {
	                conn.rollback();
	                return ResultadoCadastro.ERRO;
	            }

	            try (ResultSet chaves = prepare.getGeneratedKeys()) {
	                if (!chaves.next()) {
	                    conn.rollback();
	                    return ResultadoCadastro.ERRO;
	                }
	                FotoAnuncioDAO.salvarFotos(conn, chaves.getInt(1), fotosBase64);
	            }
	        }

	        conn.commit();
	        return ResultadoCadastro.SUCESSO;

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
	    return ResultadoCadastro.ERRO;
	}
	
	public static boolean deletar(int idAnuncio, int idUsuarioDono) {
        String sqlVerificarDono =
                "SELECT id FROM anuncios "
                + "WHERE id = ? AND id_usuario = ? AND status <> 'BANIDO' FOR UPDATE";
        String sqlExcluirDenuncias = "DELETE FROM denuncias WHERE id_anuncio = ?";
        String sqlExcluirAnuncio =
                "DELETE FROM anuncios "
                + "WHERE id = ? AND id_usuario = ? AND status <> 'BANIDO'";
        Connection conn = null;

        try {
            conn = Conexao.getConnection();
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
            "LEFT JOIN (SELECT id_anuncio, MIN(id) AS id_foto " +
            "FROM fotos_anuncio WHERE is_capa = TRUE GROUP BY id_anuncio) capa " +
            "ON capa.id_anuncio = s.id " +
            "LEFT JOIN fotos_anuncio f ON f.id = capa.id_foto " +
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
    
    public static AnunciosPaginadosDTO buscarAnunciosPublicosPaginados(
            int pagina, int limite, String busca, String tipo, boolean topAvaliacoes)
            throws SQLException {
        List<AnuncioPublicoDTO> anuncios = new ArrayList<>();
        String buscaFiltro = busca == null ? null : busca.trim();
        if (buscaFiltro != null && buscaFiltro.isEmpty()) {
            buscaFiltro = null;
        }

        List<String> termosBusca = new ArrayList<>();
        if (buscaFiltro != null) {
            for (String termo : buscaFiltro.split("[^\\p{L}\\p{N}]+")) {
                String termoLimpo = termo.trim();
                if (termoLimpo.length() >= 3 && !termosBusca.contains(termoLimpo)) {
                    termosBusca.add(termoLimpo);
                }
            }

            // Mantém buscas isoladas com um ou dois caracteres funcionando.
            if (termosBusca.isEmpty()) {
                termosBusca.add(buscaFiltro);
            }
        }

        StringBuilder filtros = new StringBuilder(" WHERE s.status = 'ATIVO'");
        if (!termosBusca.isEmpty()) {
            // Esta collation torna a comparação indiferente a caixa e acentos.
            filtros.append(" AND (");
            for (int i = 0; i < termosBusca.size(); i++) {
                if (i > 0) {
                    filtros.append(" OR ");
                }
                filtros.append("(CONVERT(s.nome USING utf8mb4) COLLATE utf8mb4_unicode_ci LIKE ? ")
                       .append("OR CONVERT(s.descricao USING utf8mb4) COLLATE utf8mb4_unicode_ci LIKE ?)");
            }
            filtros.append(")");
        }
        if (tipo != null) {
            filtros.append(" AND s.tipo = ?");
        }

        String sqlContagem = "SELECT COUNT(*) FROM anuncios s" + filtros;
        StringBuilder sqlListagem = new StringBuilder()
                .append("SELECT s.id, s.nome, s.descricao, f.foto_base64 AS foto_capa, ")
                .append("u.nome AS nome_usuario, s.tipo, ")
                .append("COALESCE(s.avaliacao_media, 0) AS avaliacao_media, ")
                .append("COALESCE(s.total_avaliacoes, 0) AS total_avaliacoes ")
                .append("FROM anuncios s ")
                .append("JOIN usuarios u ON u.id = s.id_usuario ")
                .append("LEFT JOIN (SELECT id_anuncio, MIN(id) AS id_foto ")
                .append("FROM fotos_anuncio WHERE is_capa = TRUE GROUP BY id_anuncio) capa ")
                .append("ON capa.id_anuncio = s.id ")
                .append("LEFT JOIN fotos_anuncio f ON f.id = capa.id_foto")
                .append(filtros);

        if (topAvaliacoes) {
            sqlListagem.append(" ORDER BY COALESCE(s.avaliacao_media, 0) DESC, ")
                       .append("COALESCE(s.total_avaliacoes, 0) DESC, s.id DESC");
        } else {
            // O modelo atual não possui data de criação; IDs maiores são os mais recentes.
            sqlListagem.append(" ORDER BY s.id DESC");
        }
        sqlListagem.append(" LIMIT ? OFFSET ?");

        try (Connection conn = Conexao.getConnection()) {
            if (conn == null) {
                throw new SQLException("Não foi possível obter conexão com o banco de dados.");
            }

            long totalAnuncios;
            try (PreparedStatement ps = conn.prepareStatement(sqlContagem)) {
                preencherFiltrosPublicos(ps, termosBusca, tipo);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    totalAnuncios = rs.getLong(1);
                }
            }

            try (PreparedStatement ps = conn.prepareStatement(sqlListagem.toString())) {
                int proximoParametro = preencherFiltrosPublicos(ps, termosBusca, tipo);
                ps.setInt(proximoParametro++, limite);
                ps.setLong(proximoParametro, ((long) pagina - 1L) * limite);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        anuncios.add(new AnuncioPublicoDTO(
                                rs.getInt("id"),
                                rs.getString("nome"),
                                rs.getString("descricao"),
                                rs.getString("foto_capa"),
                                rs.getString("nome_usuario"),
                                rs.getString("tipo"),
                                rs.getDouble("avaliacao_media"),
                                rs.getInt("total_avaliacoes")));
                    }
                }
            }

            return new AnunciosPaginadosDTO(anuncios, pagina, limite, totalAnuncios);
        }
    }

    private static int preencherFiltrosPublicos(
            PreparedStatement ps, List<String> termosBusca, String tipo) throws SQLException {
        int indice = 1;
        for (String busca : termosBusca) {
            String termo = "%" + busca + "%";
            ps.setString(indice++, termo);
            ps.setString(indice++, termo);
        }
        if (tipo != null) {
            ps.setString(indice++, tipo);
        }
        return indice;
    }

    public static List<AnuncioCardDTO> buscarAnunciosDestaque() {
        List<AnuncioCardDTO> lista = new ArrayList<>();

        StringBuilder sql = new StringBuilder();

        sql.append("SELECT s.id, s.nome, s.descricao, f.foto_base64 AS foto_capa, ")
           .append("s.avaliacao_media, u.nome AS nome_usuario, ")
           .append("((COALESCE(s.total_avaliacoes, 0) * COALESCE(s.avaliacao_media, 0)) + ")
           .append(PESO_MINIMO_AVALIACOES).append(" * ").append(MEDIA_GLOBAL_AVALIACOES)
           .append(") / (COALESCE(s.total_avaliacoes, 0) + ")
           .append(PESO_MINIMO_AVALIACOES).append(") AS nota_ponderada ")
           .append("FROM anuncios s ")
           .append("JOIN usuarios u ON s.id_usuario = u.id ")
           .append("LEFT JOIN (SELECT id_anuncio, MIN(id) AS id_foto ")
           .append("FROM fotos_anuncio WHERE is_capa = TRUE GROUP BY id_anuncio) capa ")
           .append("ON capa.id_anuncio = s.id ")
           .append("LEFT JOIN fotos_anuncio f ON f.id = capa.id_foto ")
           .append("WHERE s.status = 'ATIVO' ")
           .append("ORDER BY nota_ponderada DESC, s.id DESC LIMIT 3");

        try (Connection conn = Conexao.getConnection();
             PreparedStatement prepare = conn.prepareStatement(sql.toString())) {

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
