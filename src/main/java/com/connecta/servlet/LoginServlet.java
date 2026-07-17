package com.connecta.servlet;

import java.io.IOException;
import java.util.Date;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.connecta.dao.UsuarioDAO;
import com.connecta.entity.Usuario;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.mindrot.jbcrypt.BCrypt;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
	
    // doPost: RESPONSÁVEL PELO LOGIN DO USUÁRIO
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		res.setContentType("application/json"); 
	    res.setCharacterEncoding("UTF-8");
        
	    try {
	    	String email = req.getParameter("email").trim().toLowerCase();
	        String senha = req.getParameter("senha");

	        Usuario usuarioBanco = UsuarioDAO.buscarPorEmail(email);

		     if (usuarioBanco != null && BCrypt.checkpw(senha, usuarioBanco.getSenha())) {
		         
		    	 Algorithm algoritmo = Algorithm.HMAC256(com.connecta.conexao.Conexao.JWT_SECRET); // Senha do servidor
		    	 String token = JWT.create()
		    	     .withIssuer("connecta-api")
		    	     .withClaim("id", usuarioBanco.getId())
		    	     .withClaim("email", usuarioBanco.getEmail())
		    	     .withExpiresAt(new Date(System.currentTimeMillis() + 2629800000L)) 
		    	     .sign(algoritmo);

		    	 res.setStatus(HttpServletResponse.SC_OK);
		    	 res.getWriter().write("{\"mensagem\": \"Sucesso\", \"token\": \"" + token + "\"}");
		
		     } else {
		         res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		         res.getWriter().write("{\"erro\": \"Credenciais inválidas. Tente novamente.\"}"); 
		     }
	    } catch (Exception e) {
	        e.printStackTrace();
	        res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
	        res.getWriter().write("{\"erro\": \"Erro interno no servidor.\"}");
	    }
        
	 }
}
