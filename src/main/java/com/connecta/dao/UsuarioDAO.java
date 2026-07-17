package com.connecta.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import com.connecta.conexao.Conexao;
import com.connecta.entity.Usuario;

public class UsuarioDAO {
	
    public static void cadastrar(Usuario usuario) {
        String sql = "INSERT INTO usuarios(nome, email, senha) VALUES (?, ?, ?)";
        
        try (Connection conn = Conexao.getConnection(); 
             PreparedStatement prepare = conn.prepareStatement(sql)) {
             
            prepare.setString(1, usuario.getNome());
            prepare.setString(2, usuario.getEmail());
            prepare.setString(3, usuario.getSenha());
            prepare.execute();
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public static Usuario buscarPorEmail(String email) {
    	String sql = "SELECT id, nome, email, senha, tipo_conta FROM usuarios WHERE email = ?";
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
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return usuarioEncontrado;
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
}