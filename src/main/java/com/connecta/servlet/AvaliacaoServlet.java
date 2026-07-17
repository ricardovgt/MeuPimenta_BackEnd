package com.connecta.servlet;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.connecta.dao.AvaliacaoDAO;
import com.connecta.dao.ServicoDAO;
import com.connecta.dao.UsuarioDAO;
import com.connecta.entity.Avaliacao;
import com.connecta.entity.Servico;
import com.connecta.entity.Usuario;
import com.google.gson.Gson;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/avaliacoes")
public class AvaliacaoServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    // LISTA AS AVALIAÇÕES DE UM SERVIÇO
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");

        try {
            String idServicoParam = req.getParameter("idServico");

            if (idServicoParam == null || idServicoParam.trim().isEmpty()) {
                res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                res.getWriter().print("{\"erro\": \"O parâmetro idServico é obrigatório.\"}");
                return;
            }

            int idServico = Integer.parseInt(idServicoParam);
            List<Avaliacao> avaliacoes = AvaliacaoDAO.listarPorServico(idServico);

            String json = new Gson().toJson(avaliacoes);
            res.setStatus(HttpServletResponse.SC_OK);
            res.getWriter().print(json);

        } catch (NumberFormatException e) {
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            res.getWriter().print("{\"erro\": \"idServico inválido.\"}");
        } catch (Exception e) {
            e.printStackTrace();
            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            res.getWriter().print("{\"erro\": \"Erro interno ao buscar avaliações.\"}");
        }
    }

    // REGISTRA UMA NOVA AVALIAÇÃO (EXIGE TOKEN VÁLIDO)
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
            // Validação real do token (igual ao doPost do ServicoServlet)
            String token = authHeader.substring(7);
            Algorithm algoritmo = Algorithm.HMAC256(com.connecta.conexao.Conexao.JWT_SECRET);
            DecodedJWT jwt = JWT.require(algoritmo).withIssuer("connecta-api").build().verify(token);

            String emailDoToken = jwt.getClaim("email").asString();
            int idUsuarioToken = jwt.getClaim("id").asInt();

            Usuario usuarioReq = UsuarioDAO.buscarPorEmail(emailDoToken);
            if (usuarioReq == null) {
                res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                res.getWriter().print("{\"erro\": \"Usuário inválido.\"}");
                return;
            }

            // Coleta e valida os parâmetros
            String idServicoParam = req.getParameter("idServico");
            String notaParam = req.getParameter("nota");

            if (idServicoParam == null || notaParam == null) {
                res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                res.getWriter().print("{\"erro\": \"idServico e nota são obrigatórios.\"}");
                return;
            }

            int idServico;
            double nota;
            try {
                idServico = Integer.parseInt(idServicoParam);
                nota = Double.parseDouble(notaParam);
            } catch (NumberFormatException e) {
                res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                res.getWriter().print("{\"erro\": \"idServico ou nota em formato inválido.\"}");
                return;
            }

            if (nota < 1 || nota > 5) {
                res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                res.getWriter().print("{\"erro\": \"A nota deve ser entre 1 e 5.\"}");
                return;
            }

            // Confirma que o serviço existe antes de tentar avaliar
            Servico servico = ServicoDAO.pegarServico(idServico);
            if (servico == null) {
                res.setStatus(HttpServletResponse.SC_NOT_FOUND);
                res.getWriter().print("{\"erro\": \"Serviço não encontrado.\"}");
                return;
            }

            // Só serve para saber se é criação ou atualização, não bloqueia mais nada
            boolean jaAvaliouAntes = AvaliacaoDAO.usuarioJaAvaliou(idServico, idUsuarioToken);

            Avaliacao avaliacao = new Avaliacao();
            avaliacao.setIdServico(idServico);
            avaliacao.setIdUsuario(idUsuarioToken);
            avaliacao.setNota(nota);
            avaliacao.setDataAvaliacao(LocalDateTime.now());

            if (AvaliacaoDAO.registrar(avaliacao)) {
                if (jaAvaliouAntes) {
                    res.setStatus(HttpServletResponse.SC_OK);
                    res.getWriter().print("{\"mensagem\": \"Avaliação atualizada com sucesso!\"}");
                } else {
                    res.setStatus(HttpServletResponse.SC_CREATED);
                    res.getWriter().print("{\"mensagem\": \"Avaliação registrada com sucesso!\"}");
                }
            } else {
                res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                res.getWriter().print("{\"erro\": \"Falha ao salvar avaliação no banco.\"}");
            }

        } catch (com.auth0.jwt.exceptions.JWTVerificationException e) {
            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            res.getWriter().print("{\"erro\": \"Token inválido ou expirado.\"}");
        } catch (Exception e) {
            e.printStackTrace();
            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            res.getWriter().print("{\"erro\": \"Erro interno no processamento da avaliação.\"}");
        }
    }
}