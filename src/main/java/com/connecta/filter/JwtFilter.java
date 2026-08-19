package com.connecta.filter;

import java.io.IOException;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebFilter(urlPatterns = {"/anuncios", "/avaliacoes", "/usuario"})
public class JwtFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        // Garante os headers de CORS mesmo quando este filtro responde direto (401),
        // sem depender da ordem de execução em relação ao CorsFilter.
        res.setHeader("Access-Control-Allow-Origin", "*");
        res.setHeader("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT, OPTIONS");
        res.setHeader("Access-Control-Allow-Headers", "Authorization, Content-Type");

        String path = req.getServletPath();
        String method = req.getMethod();

        // Identifica quais métodos de quais rotas exigem Token JWT
        boolean precisaAutenticacao = false;

        if ("/anuncios".equals(path) && ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method) || "DELETE".equalsIgnoreCase(method) 
        		|| "GET".equalsIgnoreCase(method) && ("true".equalsIgnoreCase(req.getParameter("meus"))))) {
        	
            precisaAutenticacao = true;
        } else if ("/avaliacoes".equals(path) && "POST".equalsIgnoreCase(method) || "DELETE".equalsIgnoreCase(method)) {
            precisaAutenticacao = true;
        } else if ("/usuario".equals(path) && ("GET".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method))) {
            precisaAutenticacao = true;
        }

        if (precisaAutenticacao) {
            String authHeader = req.getHeader("Authorization");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                res.setContentType("application/json");
                res.setCharacterEncoding("UTF-8");
                res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                res.getWriter().print("{\"erro\": \"Autenticação necessária.\"}");
                return;
            }

            try {
                String token = authHeader.substring(7);
                Algorithm algoritmo = Algorithm.HMAC256(com.connecta.conexao.Conexao.JWT_SECRET);
                DecodedJWT jwt = JWT.require(algoritmo).withIssuer("connecta-api").build().verify(token);

                // Injeta as informações extraídas do token diretamente nos atributos da requisição
                req.setAttribute("idUsuarioToken", jwt.getClaim("id").asInt());
                req.setAttribute("emailUsuarioToken", jwt.getClaim("email").asString());

            } catch (Exception e) {
                res.setContentType("application/json");
                res.setCharacterEncoding("UTF-8");
                res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                res.getWriter().print("{\"erro\": \"Token inválido ou expirado.\"}");
                return;
            }
        }

        chain.doFilter(request, response);
    }
}