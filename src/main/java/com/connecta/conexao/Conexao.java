package com.connecta.conexao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Conexao {
	
	private static final String URL = System.getenv("DB_URL");
	private static final String USER = System.getenv("DB_USER");
	private static final String PASSWORD = System.getenv("DB_PASSWORD");
	public static final String JWT_SECRET = System.getenv("JWT_SECRET");
	
	public static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            
            if (URL == null || USER == null || PASSWORD == null) {
                throw new SQLException("Variáveis de ambiente do banco de dados não configuradas.");
            }
            
            return DriverManager.getConnection(URL, USER, PASSWORD);
            
        } catch (ClassNotFoundException e) {
            throw new SQLException("Driver do MySQL não encontrado.", e);
        }
    }
}
