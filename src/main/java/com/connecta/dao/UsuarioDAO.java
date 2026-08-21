package com.connecta.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import com.connecta.conexao.Conexao;
import com.connecta.entity.Usuario;

public class UsuarioDAO {
	
	public static void cadastrar(Usuario usuario) {
	    String sql = "INSERT INTO usuarios(nome, email, senha, tipo_conta, status) VALUES (?, ?, ?, ?, 'ATIVO')";
	    
	    try (Connection conn = Conexao.getConnection(); 
	         PreparedStatement prepare = conn.prepareStatement(sql)) {
	         
	        prepare.setString(1, usuario.getNome());
	        prepare.setString(2, usuario.getEmail());
	        prepare.setString(3, usuario.getSenha());
	        prepare.setString(4, usuario.getTipoConta());
	        prepare.execute();
	        
	    } catch (SQLException e) {
	        e.printStackTrace();
	    }
	}
    
	//CONSULTAS
	public static Usuario buscarPorEmail(String email) {
	    String sql = "SELECT id, nome, email, senha, tipo_conta, foto_perfil, status FROM usuarios WHERE email = ?";
	    Usuario usuarioEncontrado = null;
	    
	    try (Connection conn = Conexao.getConnection(); 
	         PreparedStatement prepare = conn.prepareStatement(sql)) {
	         
	        prepare.setString(1, email);
	        
	        try (ResultSet result = prepare.executeQuery()) {
	            if (result.next()) {
	                usuarioEncontrado = new Usuario();
	                usuarioEncontrado.setId(result.getInt("id"));
	                usuarioEncontrado.setNome(result.getString("nome"));
	                usuarioEncontrado.setEmail(result.getString("email"));
	                usuarioEncontrado.setSenha(result.getString("senha"));
	                usuarioEncontrado.setTipoConta(result.getString("tipo_conta"));
	                usuarioEncontrado.setFotoPerfil(result.getString("foto_perfil"));
	                usuarioEncontrado.setStatus(result.getString("status")); // <-- Adicionado
	            }
	        }
	    } catch (SQLException e) {
	        e.printStackTrace();
	    }
	    return usuarioEncontrado;
	}

	public static Usuario buscarPorId(int idUsuario) {
	    String sql = "SELECT id, nome, email, tipo_conta, foto_perfil, status FROM usuarios WHERE id = ?";
	    Usuario usuarioEncontrado = null;
	    
	    try (Connection conn = Conexao.getConnection(); 
	         PreparedStatement prepare = conn.prepareStatement(sql)) {
	         
	        prepare.setInt(1, idUsuario);
	        
	        try (ResultSet result = prepare.executeQuery()) {
	            if (result.next()) {
	                usuarioEncontrado = new Usuario();
	                usuarioEncontrado.setId(result.getInt("id"));
	                usuarioEncontrado.setNome(result.getString("nome"));
	                usuarioEncontrado.setEmail(result.getString("email"));
	                usuarioEncontrado.setTipoConta(result.getString("tipo_conta"));
	                usuarioEncontrado.setFotoPerfil(result.getString("foto_perfil"));
	                usuarioEncontrado.setStatus(result.getString("status")); // <-- Adicionado
	            }
	        }
	    } catch (SQLException e) {
	        e.printStackTrace();
	    }
	    return usuarioEncontrado;
	}
	
	public static String buscarSenhaHashPorId(int idUsuario) {
	    String sql = "SELECT senha FROM usuarios WHERE id = ?";
	    String senhaHash = null;

	    try (Connection conn = Conexao.getConnection();
	         PreparedStatement ps = conn.prepareStatement(sql)) {

	        ps.setInt(1, idUsuario);

	        try (ResultSet result = ps.executeQuery()) {
	            if (result.next()) {
	                senhaHash = result.getString("senha");
	            }
	        }
	    } catch (SQLException e) {
	        e.printStackTrace();
	    }
	    return senhaHash;
	}

	//MODIFICAÇÕES DE DADOS
	public static boolean atualizarFotoPerfil(int idUsuario, String fotoPerfil) {
	    String sql = "UPDATE usuarios SET foto_perfil = ? WHERE id = ?";
	    
	    try (Connection conn = Conexao.getConnection();
	         PreparedStatement ps = conn.prepareStatement(sql)) {
	        
	        ps.setString(1, fotoPerfil);
	        ps.setInt(2, idUsuario);
	        return ps.executeUpdate() > 0;
	        
	    } catch (SQLException e) {
	        e.printStackTrace();
	    }
	    return false;
	}
    
    public static boolean atualizarTipoContaPreservandoAnuncios(
            int idUsuario, String novoTipo) {
        String sqlOcultarAnunciosAtivos =
                "UPDATE anuncios SET status = 'OCULTO' "
                + "WHERE id_usuario = ? AND status = 'ATIVO'";
        String sqlAtualizarTipo = "UPDATE usuarios SET tipo_conta = ? WHERE id = ?";
        Connection conn = null;

        try {
            conn = Conexao.getConnection();
            conn.setAutoCommit(false);

            // Ao deixar de ser comercial, preserva todo o histórico. Anúncios
            // BANIDO continuam banidos e os já OCULTO permanecem inalterados.
            if ("COMUM".equals(novoTipo)) {
                try (PreparedStatement ps = conn.prepareStatement(sqlOcultarAnunciosAtivos)) {
                    ps.setInt(1, idUsuario);
                    ps.executeUpdate();
                }
            }

            try (PreparedStatement ps = conn.prepareStatement(sqlAtualizarTipo)) {
                ps.setString(1, novoTipo);
                ps.setInt(2, idUsuario);
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
                try { conn.rollback(); } catch (SQLException rollbackEx) {
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

    public static boolean descadastrarComAnuncios(int idUsuario) {
        String sqlExcluirDenunciasRecebidas =
                "DELETE d FROM denuncias d INNER JOIN anuncios a ON a.id = d.id_anuncio "
                + "WHERE a.id_usuario = ?";
        String sqlExcluirAnuncios = "DELETE FROM anuncios WHERE id_usuario = ?";
        String sqlExcluirUsuario = "DELETE FROM usuarios WHERE id = ?";
        Connection conn = null;

        try {
            conn = Conexao.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement ps = conn.prepareStatement(sqlExcluirDenunciasRecebidas)) {
                ps.setInt(1, idUsuario);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = conn.prepareStatement(sqlExcluirAnuncios)) {
                ps.setInt(1, idUsuario);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = conn.prepareStatement(sqlExcluirUsuario)) {
                ps.setInt(1, idUsuario);
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
                try { conn.rollback(); } catch (SQLException rollbackEx) {
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
    
    public static boolean atualizarNome(int idUsuario, String novoNome) {
        String sql = "UPDATE usuarios SET nome = ? WHERE id = ?";
        try (Connection conn = Conexao.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, novoNome);
            ps.setInt(2, idUsuario);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    public static boolean atualizarEmail(int idUsuario, String novoEmail) {
        String sql = "UPDATE usuarios SET email = ? WHERE id = ?";
        try (Connection conn = Conexao.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, novoEmail);
            ps.setInt(2, idUsuario);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    public static boolean atualizarSenha(int idUsuario, String novaSenhaHash) {
        String sql = "UPDATE usuarios SET senha = ? WHERE id = ?";
        try (Connection conn = Conexao.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, novaSenhaHash);
            ps.setInt(2, idUsuario);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

}
