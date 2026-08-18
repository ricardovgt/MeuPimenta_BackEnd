package com.connecta.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.connecta.conexao.Conexao;
import com.connecta.dto.AvaliacaoDTO;
import com.connecta.entity.Avaliacao;

public class AvaliacaoDAO {

    private static final DateTimeFormatter FORMATO_DATA =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static boolean usuarioJaAvaliou(int idAnuncio, int idUsuario) {
        String sql = "SELECT 1 FROM avaliacoes WHERE id_anuncio = ? AND id_usuario = ?";

        try (Connection conn = Conexao.getConnection();
             PreparedStatement prepare = conn.prepareStatement(sql)) {

            prepare.setInt(1, idAnuncio);
            prepare.setInt(2, idUsuario);

            try (ResultSet result = prepare.executeQuery()) {
                return result.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean registrar(Avaliacao avaliacao) {
        String sqlUpsert = "INSERT INTO avaliacoes (id_anuncio, id_usuario, nota, comentario, data_avaliacao) "
                + "VALUES (?, ?, ?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE nota = VALUES(nota), comentario = VALUES(comentario), "
                + "data_avaliacao = VALUES(data_avaliacao)";
        String sqlAgregado = "SELECT AVG(nota) AS media, COUNT(*) AS total FROM avaliacoes WHERE id_anuncio = ?";
        String sqlUpdate = "UPDATE anuncios SET avaliacao_media = ?, total_avaliacoes = ? WHERE id = ?";

        Connection conn = null;

        try {
            conn = Conexao.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement psUpsert = conn.prepareStatement(sqlUpsert)) {
                psUpsert.setInt(1, avaliacao.getIdAnuncio());
                psUpsert.setInt(2, avaliacao.getIdUsuario());
                psUpsert.setDouble(3, avaliacao.getNota());
                psUpsert.setString(4, avaliacao.getComentario());
                psUpsert.setObject(5, avaliacao.getDataAvaliacao() != null ? avaliacao.getDataAvaliacao() : LocalDateTime.now());
                psUpsert.executeUpdate();
            }

            double novaMedia = 0;
            int novoTotal = 0;

            try (PreparedStatement psAgregado = conn.prepareStatement(sqlAgregado)) {
                psAgregado.setInt(1, avaliacao.getIdAnuncio());
                try (ResultSet rs = psAgregado.executeQuery()) {
                    if (rs.next()) {
                        novaMedia = rs.getDouble("media");
                        novoTotal = rs.getInt("total");
                    }
                }
            }

            try (PreparedStatement psUpdate = conn.prepareStatement(sqlUpdate)) {
                psUpdate.setDouble(1, novaMedia);
                psUpdate.setInt(2, novoTotal);
                psUpdate.setInt(3, avaliacao.getIdAnuncio());
                psUpdate.executeUpdate();
            }

            conn.commit();
            return true;

        } catch (SQLException e) {
            e.printStackTrace();
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    rollbackEx.printStackTrace();
                }
            }
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException closeEx) {
                    closeEx.printStackTrace();
                }
            }
        }
    }

    public static boolean remover(int idAvaliacao, int idUsuario) {
        String sqlBuscarAnuncio = "SELECT id_anuncio FROM avaliacoes WHERE id = ? AND id_usuario = ? FOR UPDATE";
        String sqlDelete = "DELETE FROM avaliacoes WHERE id = ? AND id_usuario = ?";
        String sqlAgregado = "SELECT AVG(nota) AS media, COUNT(*) AS total FROM avaliacoes WHERE id_anuncio = ?";
        String sqlUpdate = "UPDATE anuncios SET avaliacao_media = ?, total_avaliacoes = ? WHERE id = ?";

        Connection conn = null;

        try {
            conn = Conexao.getConnection();
            conn.setAutoCommit(false);

            int idAnuncio;
            try (PreparedStatement psBuscarAnuncio = conn.prepareStatement(sqlBuscarAnuncio)) {
                psBuscarAnuncio.setInt(1, idAvaliacao);
                psBuscarAnuncio.setInt(2, idUsuario);

                try (ResultSet rs = psBuscarAnuncio.executeQuery()) {
                    if (!rs.next()) {
                        conn.rollback();
                        return false;
                    }
                    idAnuncio = rs.getInt("id_anuncio");
                }
            }

            try (PreparedStatement psDelete = conn.prepareStatement(sqlDelete)) {
                psDelete.setInt(1, idAvaliacao);
                psDelete.setInt(2, idUsuario);
                psDelete.executeUpdate();
            }

            double novaMedia = 0;
            int novoTotal = 0;

            try (PreparedStatement psAgregado = conn.prepareStatement(sqlAgregado)) {
                psAgregado.setInt(1, idAnuncio);
                try (ResultSet rs = psAgregado.executeQuery()) {
                    if (rs.next()) {
                        novaMedia = rs.getDouble("media");
                        novoTotal = rs.getInt("total");
                    }
                }
            }

            try (PreparedStatement psUpdate = conn.prepareStatement(sqlUpdate)) {
                psUpdate.setDouble(1, novaMedia);
                psUpdate.setInt(2, novoTotal);
                psUpdate.setInt(3, idAnuncio);
                psUpdate.executeUpdate();
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    rollbackEx.printStackTrace();
                }
            }
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException closeEx) {
                    closeEx.printStackTrace();
                }
            }
        }
    }

//     Retorna a lista paginada de avaliacoes (com nome do autor) para um anúncio,
//     ordenada da mais recente para a mais antiga.
    public static List<AvaliacaoDTO> listarPorAnuncioPaginado(int idAnuncio, int pagina, int limite) {
        List<AvaliacaoDTO> lista = new ArrayList<>();

        int paginaSegura = Math.max(pagina, 1);
        int limiteSeguro = Math.max(limite, 1);
        int offset = (paginaSegura - 1) * limiteSeguro;

        String sql = "SELECT a.id, a.nota, a.comentario, a.data_avaliacao, "
                + "u.id AS id_usuario, u.nome AS nome_usuario, u.foto_perfil AS foto_perfil_usuario " 
                + "FROM avaliacoes a "
                + "JOIN usuarios u ON a.id_usuario = u.id "
                + "WHERE a.id_anuncio = ? "
                + "ORDER BY a.data_avaliacao DESC "
                + "LIMIT ? OFFSET ?";

        try (Connection conn = Conexao.getConnection();
             PreparedStatement prepare = conn.prepareStatement(sql)) {

            prepare.setInt(1, idAnuncio);
            prepare.setInt(2, limiteSeguro);
            prepare.setInt(3, offset);

            try (ResultSet result = prepare.executeQuery()) {
                while (result.next()) {
                    int id = result.getInt("id");
                    double nota = result.getDouble("nota");
                    String comentario = result.getString("comentario");

                    java.sql.Timestamp timestamp = result.getTimestamp("data_avaliacao");
                    String dataFormatada = (timestamp != null)
                            ? timestamp.toLocalDateTime().format(FORMATO_DATA)
                            : null;

                    int idUsuario = result.getInt("id_usuario");
                    String nomeUsuario = result.getString("nome_usuario");
                    String fotoPerfilUsuario = result.getString("foto_perfil_usuario");

                    lista.add(new AvaliacaoDTO(id, nota, comentario, dataFormatada, idUsuario, nomeUsuario, fotoPerfilUsuario)); 
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return lista;
    }

//     Conta o total de avaliacoes cadastradas para um anúncio, usado para
//     calcular o total de paginas na rolagem continua.
    public static int contarTotalAvaliacoes(int idAnuncio) {
        String sql = "SELECT COUNT(*) FROM avaliacoes WHERE id_anuncio = ?";

        try (Connection conn = Conexao.getConnection();
             PreparedStatement prepare = conn.prepareStatement(sql)) {

            prepare.setInt(1, idAnuncio);

            try (ResultSet result = prepare.executeQuery()) {
                if (result.next()) {
                    return result.getInt(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return 0;
    }
}