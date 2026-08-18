package com.connecta.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.connecta.conexao.Conexao;
import com.connecta.dto.FotoAnuncioDTO;

public class FotoAnuncioDAO {

    private static final int LIMITE_FOTOS = 5;

    // Insere as fotos do anúncio, marcando a primeira (índice 0) como capa.
    // Recebe a Connection já aberta pelo AnuncioDAO para participar da mesma transação
    // (cadastro/edição fazem commit ou rollback dos dois inserts juntos).
    public static void salvarFotos(Connection conn, int idAnuncio, List<String> fotosBase64) throws SQLException {
        if (fotosBase64 == null || fotosBase64.isEmpty()) {
            return;
        }

        String sql = "INSERT INTO fotos_anuncio (id_anuncio, foto_base64, is_capa) VALUES (?, ?, ?)";

        // Trava o limite de 5 fotos também no servidor, não só no front-end
        int total = Math.min(fotosBase64.size(), LIMITE_FOTOS);

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < total; i++) {
                ps.setInt(1, idAnuncio);
                ps.setString(2, fotosBase64.get(i));
                ps.setBoolean(3, i == 0); // primeira foto enviada = capa
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    // Remove todas as fotos do anúncio. Usado no fluxo "wipe and replace" da edição,
    // sempre chamado antes de salvarFotos, na mesma transação.
    public static void deletarFotosPorAnuncio(Connection conn, int idAnuncio) throws SQLException {
        String sql = "DELETE FROM fotos_anuncio WHERE id_anuncio = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idAnuncio);
            ps.executeUpdate();
        }
    }

    // Busca todas as fotos de um anúncio, capa primeiro. Usado na tela de detalhes.
    // Abre sua própria conexão pois é uma leitura isolada, fora de transação de escrita.
    public static List<FotoAnuncioDTO> buscarPorAnuncio(int idAnuncio) {
        List<FotoAnuncioDTO> lista = new ArrayList<>();
        String sql = "SELECT id, foto_base64, is_capa FROM fotos_anuncio WHERE id_anuncio = ? ORDER BY is_capa DESC, id ASC";

        try (Connection conn = Conexao.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, idAnuncio);

            try (ResultSet r = ps.executeQuery()) {
                while (r.next()) {
                    lista.add(new FotoAnuncioDTO(
                        r.getInt("id"),
                        r.getString("foto_base64"),
                        r.getBoolean("is_capa")
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }
}