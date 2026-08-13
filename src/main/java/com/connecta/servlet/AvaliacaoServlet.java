package com.connecta.servlet;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import com.connecta.dao.AvaliacaoDAO;
import com.connecta.dao.ServicoDAO;
import com.connecta.dao.UsuarioDAO;
import com.connecta.dto.AvaliacaoDTO;
import com.connecta.dto.RespostaPaginadaDTO;
import com.connecta.dto.ServicoDetalheDTO;
import com.connecta.entity.Avaliacao;
import com.connecta.entity.Usuario;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/avaliacoes")
public class AvaliacaoServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private static final int LIMITE_PADRAO = 10;
    private static final int PAGINA_PADRAO = 1;

    // LISTA AS AVALIAÇÕES DE UM SERVIÇO, DE FORMA PAGINADA (ROLAGEM CONTÍNUA)
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

            int idServico;
            try {
                idServico = Integer.parseInt(idServicoParam);
            } catch (NumberFormatException e) {
                res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                res.getWriter().print("{\"erro\": \"idServico inválido.\"}");
                return;
            }

            int pagina = lerInteiroOuPadrao(req.getParameter("pagina"), PAGINA_PADRAO);
            int limite = lerInteiroOuPadrao(req.getParameter("limite"), LIMITE_PADRAO);

            if (pagina < 1) {
                pagina = PAGINA_PADRAO;
            }
            if (limite < 1) {
                limite = LIMITE_PADRAO;
            }

            List<AvaliacaoDTO> avaliacoes = AvaliacaoDAO.listarPorServicoPaginado(idServico, pagina, limite);
            int totalAvaliacoes = AvaliacaoDAO.contarTotalAvaliacoes(idServico);
            int totalPaginas = (int) Math.ceil(totalAvaliacoes / (double) limite);

            RespostaPaginadaDTO resposta = new RespostaPaginadaDTO(
                    pagina, limite, totalAvaliacoes, totalPaginas, avaliacoes);

            String json = new Gson().toJson(resposta);
            res.setStatus(HttpServletResponse.SC_OK);
            res.getWriter().print(json);

        } catch (Exception e) {
            e.printStackTrace();
            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            res.getWriter().print("{\"erro\": \"Erro interno ao buscar avaliações.\"}");
        }
    }

    // REGISTRA UMA NOVA AVALIAÇÃO, COM NOTA E COMENTÁRIO OPCIONAL (EXIGE TOKEN VÁLIDO)
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");

        try {
            int idUsuarioToken = (int) req.getAttribute("idUsuarioToken");
            String emailDoToken = (String) req.getAttribute("emailUsuarioToken");

            Usuario usuarioReq = UsuarioDAO.buscarPorEmail(emailDoToken);
            if (usuarioReq == null) {
                res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                res.getWriter().print("{\"erro\": \"Usuário inválido.\"}");
                return;
            }

            // Coleta e valida os parâmetros
            String idServicoParam = req.getParameter("idServico");
            String notaParam = req.getParameter("nota");
            String comentarioParam = req.getParameter("comentario");

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

            // Se o usuário enviar apenas a nota, sem texto, o comentário é gravado como null
            String comentario = (comentarioParam == null || comentarioParam.trim().isEmpty())
                    ? null
                    : comentarioParam.trim();

            // Confirma que o serviço existe antes de tentar avaliar
            ServicoDetalheDTO servico = ServicoDAO.pegarServicoDetalhe(idServico);
            if (servico == null) {
                res.setStatus(HttpServletResponse.SC_NOT_FOUND);
                res.getWriter().print("{\"erro\": \"Serviço não encontrado.\"}");
                return;
            }

            // Impede que o dono do serviço avalie o próprio serviço
            if (servico.getIdUsuario() == idUsuarioToken) {
                res.setStatus(HttpServletResponse.SC_FORBIDDEN);
                res.getWriter().print("{\"erro\": \"Você não pode avaliar seu próprio serviço.\"}");
                return;
            }

            boolean jaAvaliouAntes = AvaliacaoDAO.usuarioJaAvaliou(idServico, idUsuarioToken);
            
            Avaliacao avaliacao = new Avaliacao();
            avaliacao.setIdServico(idServico);
            avaliacao.setIdUsuario(idUsuarioToken);
            avaliacao.setNota(nota);
            avaliacao.setComentario(comentario);
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

        } catch (Exception e) {
            e.printStackTrace();
            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            res.getWriter().print("{\"erro\": \"Erro interno no processamento da avaliação.\"}");
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");

        try {
            int idUsuarioToken = (int) req.getAttribute("idUsuarioToken");
            JsonObject json;

            try {
                json = new Gson().fromJson(req.getReader(), JsonObject.class);
            } catch (RuntimeException e) {
                res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                res.getWriter().print("{\"erro\": \"JSON inválido.\"}");
                return;
            }

            if (json == null || !json.has("idAvaliacao") || json.get("idAvaliacao").isJsonNull()) {
                res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                res.getWriter().print("{\"erro\": \"O campo idAvaliacao é obrigatório.\"}");
                return;
            }

            int idAvaliacao;
            try {
                idAvaliacao = json.get("idAvaliacao").getAsInt();
            } catch (RuntimeException e) {
                res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                res.getWriter().print("{\"erro\": \"idAvaliacao inválido.\"}");
                return;
            }

            if (idAvaliacao < 1) {
                res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                res.getWriter().print("{\"erro\": \"idAvaliacao inválido.\"}");
                return;
            }

            if (AvaliacaoDAO.remover(idAvaliacao, idUsuarioToken)) {
                res.setStatus(HttpServletResponse.SC_OK);
                res.getWriter().print("{\"mensagem\": \"Avaliação removida com sucesso!\"}");
            } else {
                res.setStatus(HttpServletResponse.SC_NOT_FOUND);
                res.getWriter().print("{\"erro\": \"Avaliação não encontrada.\"}");
            }
        } catch (Exception e) {
            e.printStackTrace();
            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            res.getWriter().print("{\"erro\": \"Erro interno ao remover avaliação.\"}");
        }
    }

    private int lerInteiroOuPadrao(String valor, int padrao) {
        if (valor == null || valor.trim().isEmpty()) {
            return padrao;
        }
        try {
            return Integer.parseInt(valor.trim());
        } catch (NumberFormatException e) {
            return padrao;
        }
    }
}
