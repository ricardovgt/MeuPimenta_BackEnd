package com.connecta.servlet;

import java.io.IOException;
import java.util.Date;

import org.mindrot.jbcrypt.BCrypt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.connecta.dao.UsuarioDAO;
import com.connecta.entity.Usuario;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");

        try {
            String emailParam = req.getParameter("email");
            String senha = req.getParameter("senha");

            if (emailParam == null || emailParam.trim().isEmpty()
                    || senha == null || senha.isEmpty()) {
                res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                res.getWriter().write("{\"erro\": \"E-mail e senha são obrigatórios.\"}");
                return;
            }

            String email = emailParam.trim().toLowerCase();
            Usuario usuarioBanco = UsuarioDAO.buscarPorEmail(email);

            if (usuarioBanco == null
                    || !BCrypt.checkpw(senha, usuarioBanco.getSenha())) {
                res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                res.getWriter().write(
                        "{\"erro\": \"Credenciais inválidas. Tente novamente.\"}");
                return;
            }

            // Senha válida, mas uma conta banida não recebe um novo JWT.
            if (usuarioBanco.getStatus() != null
                    && "BANIDO".equalsIgnoreCase(usuarioBanco.getStatus().trim())) {
                res.setStatus(HttpServletResponse.SC_FORBIDDEN);
                res.getWriter().write(
                        "{\"erro\": \"Sua conta foi banida por violação das regras de conduta.\"}");
                return;
            }

            Algorithm algoritmo =
                    Algorithm.HMAC256(com.connecta.conexao.Conexao.JWT_SECRET);

            String token = JWT.create()
                    .withIssuer("connecta-api")
                    .withClaim("id", usuarioBanco.getId())
                    .withClaim("email", usuarioBanco.getEmail())
                    .withExpiresAt(new Date(System.currentTimeMillis() + 2629800000L))
                    .sign(algoritmo);

            res.setStatus(HttpServletResponse.SC_OK);
            res.getWriter().write(
                    "{\"mensagem\": \"Sucesso\", \"token\": \"" + token + "\"}");

        } catch (Exception e) {
            e.printStackTrace();
            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            res.getWriter().write("{\"erro\": \"Erro interno no servidor.\"}");
        }
    }
}