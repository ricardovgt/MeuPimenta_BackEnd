package com.connecta.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.connecta.conexao.Conexao;
import com.connecta.entity.Avaliacao;

public class AvaliacaoDAO {

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
        String sqlUpsert = "INSERT INTO avaliacoes (id_servico, id_usuario, nota, data_avaliacao) "
                + "VALUES (?, ?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE nota = VALUES(nota), data_avaliacao = VALUES(data_avaliacao)";
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
                psUpsert.setObject(4, avaliacao.getDataAvaliacao() != null ? avaliacao.getDataAvaliacao() : LocalDateTime.now());
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

    public static List<Avaliacao> listarPorServico(int idServico) {
        List<Avaliacao> lista = new ArrayList<>();
        String sql = "SELECT * FROM avaliacoes WHERE id_servico = ? ORDER BY data_avaliacao DESC";

        try (Connection conn = Conexao.getConnection();
             PreparedStatement prepare = conn.prepareStatement(sql)) {

            prepare.setInt(1, idServico);

            try (ResultSet result = prepare.executeQuery()) {
                while (result.next()) {
                    Avaliacao avaliacao = new Avaliacao();
                    avaliacao.setId(result.getInt("id"));
                    avaliacao.setIdServico(result.getInt("id_servico"));
                    avaliacao.setIdUsuario(result.getInt("id_usuario"));
                    avaliacao.setNota(result.getDouble("nota"));
                    avaliacao.setDataAvaliacao(result.getTimestamp("data_avaliacao").toLocalDateTime());
                    lista.add(avaliacao);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return lista;
    }
}