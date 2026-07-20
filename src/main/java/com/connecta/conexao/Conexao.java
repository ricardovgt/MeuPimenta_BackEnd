package com.connecta.conexao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Conexao {
	
	// Agora as variáveis buscam os valores do ambiente do sistema
	private static final String url = System.getenv("DB_URL");
	private static final String user = System.getenv("DB_USER");
	private static final String password = System.getenv("DB_PASSWORD");
	public static final String JWT_SECRET = System.getenv("JWT_SECRET");
	
	public static Connection getConnection() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            
            // Uma boa prática é checar se as variáveis foram carregadas
            if (url == null || user == null || password == null) {
                System.err.println("ERRO: Variáveis de ambiente do banco de dados não configuradas!");
                return null;
            }
            
            return DriverManager.getConnection(url, user, password);
            
        } catch (ClassNotFoundException e) {
            System.err.println("ERRO: O Driver do MySQL não foi encontrado pelo Tomcat");
            e.printStackTrace();
        } catch (SQLException e) {
            System.err.println("ERRO: Falha na conexão! Verifique se o MySQL está ligado, se a senha/usuário estão certos ou se o banco existe");
            e.printStackTrace();
        }
        
        return null;
    }
}