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

    public static boolean usuarioJaAvaliou(int idServico, int idUsuario) {
        String sql = "SELECT 1 FROM avaliacoes WHERE id_servico = ? AND id_usuario = ?";

        try (Connection conn = Conexao.getConnection();
             PreparedStatement prepare = conn.prepareStatement(sql)) {

            prepare.setInt(1, idServico);
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
        String sqlUpsert = "INSERT INTO avaliacoes (id_servico, id_usuario, nota, comentario, data_avaliacao) "
                + "VALUES (?, ?, ?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE nota = VALUES(nota), comentario = VALUES(comentario), "
                + "data_avaliacao = VALUES(data_avaliacao)";
        String sqlAgregado = "SELECT AVG(nota) AS media, COUNT(*) AS total FROM avaliacoes WHERE id_servico = ?";
        String sqlUpdate = "UPDATE servicos SET avaliacao_media = ?, total_avaliacoes = ? WHERE id = ?";

        Connection conn = null;

        try {
            conn = Conexao.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement psUpsert = conn.prepareStatement(sqlUpsert)) {
                psUpsert.setInt(1, avaliacao.getIdServico());
                psUpsert.setInt(2, avaliacao.getIdUsuario());
                psUpsert.setDouble(3, avaliacao.getNota());
                psUpsert.setString(4, avaliacao.getComentario());
                psUpsert.setObject(5, avaliacao.getDataAvaliacao() != null ? avaliacao.getDataAvaliacao() : LocalDateTime.now());
                psUpsert.executeUpdate();
            }

            double novaMedia = 0;
            int novoTotal = 0;

            try (PreparedStatement psAgregado = conn.prepareStatement(sqlAgregado)) {
                psAgregado.setInt(1, avaliacao.getIdServico());
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
                psUpdate.setInt(3, avaliacao.getIdServico());
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
        String sqlBuscarServico = "SELECT id_servico FROM avaliacoes WHERE id = ? AND id_usuario = ? FOR UPDATE";
        String sqlDelete = "DELETE FROM avaliacoes WHERE id = ? AND id_usuario = ?";
        String sqlAgregado = "SELECT AVG(nota) AS media, COUNT(*) AS total FROM avaliacoes WHERE id_servico = ?";
        String sqlUpdate = "UPDATE servicos SET avaliacao_media = ?, total_avaliacoes = ? WHERE id = ?";

        Connection conn = null;

        try {
            conn = Conexao.getConnection();
            conn.setAutoCommit(false);

            int idServico;
            try (PreparedStatement psBuscarServico = conn.prepareStatement(sqlBuscarServico)) {
                psBuscarServico.setInt(1, idAvaliacao);
                psBuscarServico.setInt(2, idUsuario);

                try (ResultSet rs = psBuscarServico.executeQuery()) {
                    if (!rs.next()) {
                        conn.rollback();
                        return false;
                    }
                    idServico = rs.getInt("id_servico");
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
                psAgregado.setInt(1, idServico);
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
                psUpdate.setInt(3, idServico);
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

//     Retorna a lista paginada de avaliacoes (com nome do autor) para um servico,
//     ordenada da mais recente para a mais antiga.
    public static List<AvaliacaoDTO> listarPorServicoPaginado(int idServico, int pagina, int limite) {
        List<AvaliacaoDTO> lista = new ArrayList<>();

        int paginaSegura = Math.max(pagina, 1);
        int limiteSeguro = Math.max(limite, 1);
        int offset = (paginaSegura - 1) * limiteSeguro;

        String sql = "SELECT a.id, a.nota, a.comentario, a.data_avaliacao, "
                + "u.id AS id_usuario, u.nome AS nome_usuario, u.foto_perfil AS foto_perfil_usuario " 
                + "FROM avaliacoes a "
                + "JOIN usuarios u ON a.id_usuario = u.id "
                + "WHERE a.id_servico = ? "
                + "ORDER BY a.data_avaliacao DESC "
                + "LIMIT ? OFFSET ?";

        try (Connection conn = Conexao.getConnection();
             PreparedStatement prepare = conn.prepareStatement(sql)) {

            prepare.setInt(1, idServico);
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

//     Conta o total de avaliacoes cadastradas para um servico, usado para
//     calcular o total de paginas na rolagem continua.
    public static int contarTotalAvaliacoes(int idServico) {
        String sql = "SELECT COUNT(*) FROM avaliacoes WHERE id_servico = ?";

        try (Connection conn = Conexao.getConnection();
             PreparedStatement prepare = conn.prepareStatement(sql)) {

            prepare.setInt(1, idServico);

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
