package com.connecta.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import com.connecta.conexao.Conexao;
import com.connecta.entity.Usuario;

public class UsuarioDAO {
	
	public static void cadastrar(Usuario usuario) {
	    String sql = "INSERT INTO usuarios(nome, email, senha, tipo_conta) VALUES (?, ?, ?, ?)";
	    
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
    
	public static boolean descadastrar(int idUsuario) {
        String sql = "DELETE FROM usuarios WHERE id = ?";
        try (Connection conn = Conexao.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, idUsuario);
            return ps.executeUpdate() > 0; 
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
	
	//CONSULTAS
	public static Usuario buscarPorEmail(String email) {
	    String sql = "SELECT id, nome, email, senha, tipo_conta, foto_perfil FROM usuarios WHERE email = ?";
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
	                usuarioEncontrado.setFotoPerfil(result.getString("foto_perfil")); // <-- Adicionado
	            }
	        }
	    } catch (SQLException e) {
	        e.printStackTrace();
	    }
	    return usuarioEncontrado;
	}

	public static Usuario buscarPorId(int idUsuario) {
	    String sql = "SELECT id, nome, email, tipo_conta, foto_perfil FROM usuarios WHERE id = ?";
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
	                usuarioEncontrado.setFotoPerfil(result.getString("foto_perfil")); // <-- Adicionado
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
    
    public static boolean atualizarTipoConta(int idUsuario, String novoTipo) {
        String sql = "UPDATE usuarios SET tipo_conta = ? WHERE id = ?";
        try (Connection conn = Conexao.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, novoTipo);
            ps.setInt(2, idUsuario);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
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