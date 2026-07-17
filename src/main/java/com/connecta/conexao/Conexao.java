package com.connecta.conexao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Conexao {
	
	private static final String url= "jdbc:mysql://localhost:3306/data_basero";
	private static final String user = "root";
	private static final String password = "rdyBt9c$ssLnr5SMbKewf$oVt4S#Q&DVMPS@U29Hopp0QPHG13";
	public static final String JWT_SECRET = "c10nnectaP1menta_b@ckend_secret_key_2026_complex_string_!";
	
	public static Connection getConnection() {
        try {
           
            Class.forName("com.mysql.cj.jdbc.Driver");
            
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
