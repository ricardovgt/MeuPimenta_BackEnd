package com.connecta.servlet;

import java.io.IOException;
import java.util.List;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.connecta.dao.ServicoDAO;
import com.connecta.dao.UsuarioDAO;
import com.connecta.entity.Servico;
import com.connecta.entity.Usuario;
import com.google.gson.Gson;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/servicos")
public class ServicoServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    // RETORNA A LISTA DE SERVIÇOS COM FILTROS
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");
        
        try {
            // Tenta pegar o ID na URL
            String idParam = req.getParameter("id");
            
            // LÓGICA 1: BUSCAR UM ÚNICO SERVIÇO (Se o ID foi passado)
            if (idParam != null && !idParam.trim().isEmpty()) {
                try {
                    int id = Integer.parseInt(idParam);
                    Servico servico = ServicoDAO.pegarServico(id); // O método que corrigimos antes!
                    
                    if (servico != null) {
                        String json = new Gson().toJson(servico);
                        res.setStatus(HttpServletResponse.SC_OK);
                        res.getWriter().print(json);
                    } else {
                        // Se não achou o serviço no banco (retornou null)
                        res.setStatus(HttpServletResponse.SC_NOT_FOUND); // Status 404
                        res.getWriter().print("{\"erro\": \"Serviço não encontrado.\"}");
                    }
                } catch (NumberFormatException e) {
                    // Se o cara passar letras no lugar do ID (ex: ?id=abc)
                    res.setStatus(HttpServletResponse.SC_BAD_REQUEST); // Status 400
                    res.getWriter().print("{\"erro\": \"ID inválido.\"}");
                }
            } 
            // LÓGICA 2: BUSCAR LISTA DE SERVIÇOS (Se o ID não foi passado)
            else {
                String bairro = req.getParameter("bairro");
                String topParam = req.getParameter("top");
                boolean topAvaliacoes = topParam != null && topParam.equalsIgnoreCase("true");
                
                List<Servico> servicos = ServicoDAO.buscarServicos(bairro, topAvaliacoes);
                
                String json = new Gson().toJson(servicos);
                res.setStatus(HttpServletResponse.SC_OK);
                res.getWriter().print(json);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            res.getWriter().print("{\"erro\": \"Erro interno do servidor ao buscar serviços.\"}");
        }
    }

    // CADASTRA UM NOVO SERVIÇO (EXIGE TOKEN E CONTA COMERCIAL)
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");

        String authHeader = req.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            res.getWriter().print("{\"erro\": \"Autenticação necessária.\"}");
            return;
        }

        try {
            String token = authHeader.substring(7);
            Algorithm algoritmo = Algorithm.HMAC256(com.connecta.conexao.Conexao.JWT_SECRET);
            DecodedJWT jwt = JWT.require(algoritmo).withIssuer("connecta-api").build().verify(token);

            String emailDoToken = jwt.getClaim("email").asString();
            int idDoToken = jwt.getClaim("id").asInt();
            
            // Verifica se o usuário existe e se é COMERCIAL
            Usuario usuarioReq = UsuarioDAO.buscarPorEmail(emailDoToken);
            if (usuarioReq == null) {
                res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                res.getWriter().print("{\"erro\": \"Usuário inválido.\"}");
                return;
            }
            
            if (usuarioReq.getTipoConta() == null || !usuarioReq.getTipoConta().equalsIgnoreCase("COMERCIAL")) {
                res.setStatus(HttpServletResponse.SC_FORBIDDEN);
                res.getWriter().print("{\"erro\": \"Apenas contas do tipo COMERCIAL podem anunciar serviços.\"}");
                return;
            }

            // Coleta os parâmetros
            String nome = req.getParameter("nome");
            String descricao = req.getParameter("descricao");
            String telefone = req.getParameter("telefone");
            String bairro = req.getParameter("bairro");
            String fotoUrl = req.getParameter("fotoUrl"); // Pode ser nulo
            String descricaoDetalhada = req.getParameter("descricaoDetalhada");
            
            if (nome == null || telefone == null || bairro == null || 
                nome.trim().isEmpty() || telefone.trim().isEmpty() || bairro.trim().isEmpty()) {
                res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                res.getWriter().print("{\"erro\": \"Nome, telefone e bairro são obrigatórios.\"}");
                return;
            }

            Servico servico = new Servico();
            servico.setIdUsuario(idDoToken);
            servico.setNome(nome.trim());
            servico.setDescricao(descricao != null ? descricao.trim() : "");
            servico.setTelefone(telefone.trim());
            servico.setBairro(bairro.trim());
            servico.setFotoUrl(fotoUrl != null ? fotoUrl.trim() : "");
            servico.setDescricaoDetalhada(descricaoDetalhada != null ? descricaoDetalhada.trim() : "");
            
            if (ServicoDAO.cadastrar(servico)) {
                res.setStatus(HttpServletResponse.SC_CREATED);
                res.getWriter().print("{\"mensagem\": \"Serviço cadastrado com sucesso!\"}");
            } else {
                res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                res.getWriter().print("{\"erro\": \"Falha ao salvar o serviço no banco de dados.\"}");
            }

        } catch (com.auth0.jwt.exceptions.JWTVerificationException e) {
            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            res.getWriter().print("{\"erro\": \"Token inválido ou expirado.\"}");
        } catch (Exception e) {
            e.printStackTrace();
            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            res.getWriter().print("{\"erro\": \"Erro interno no processamento do serviço.\"}");
        }
    }
}