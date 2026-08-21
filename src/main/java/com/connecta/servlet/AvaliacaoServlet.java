package com.connecta.servlet;

import static com.connecta.utils.ServletUtil.lerInteiroOuPadrao;
import static com.connecta.utils.ServletUtil.obterIdUsuarioToken;
import static com.connecta.utils.ServletUtil.prepararResposta;
import static com.connecta.utils.ServletUtil.responderContaBanida;
import static com.connecta.utils.ServletUtil.usuarioEstaBanido;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import com.connecta.dao.AnuncioDAO;
import com.connecta.dao.AvaliacaoDAO;
import com.connecta.dao.UsuarioDAO;
import com.connecta.dto.AnuncioDetalheDTO;
import com.connecta.dto.AvaliacaoDTO;
import com.connecta.dto.RespostaPaginadaDTO;
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
    private static final int LIMITE_COMENTARIO = 1000;

    private static final Gson GSON = new Gson();

    // LISTA AS AVALIAÇÕES DE UM ANÚNCIO DE FORMA PAGINADA
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        prepararResposta(res);

        try {
            String idAnuncioParam = req.getParameter("idAnuncio");

            if (idAnuncioParam == null || idAnuncioParam.trim().isEmpty()) {
                res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                res.getWriter().print(
                        "{\"erro\": \"O parâmetro idAnuncio é obrigatório.\"}");
                return;
            }

            int idAnuncio;
            try {
                idAnuncio = Integer.parseInt(idAnuncioParam.trim());
            } catch (NumberFormatException e) {
                res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                res.getWriter().print("{\"erro\": \"idAnuncio inválido.\"}");
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
            limite = Math.min(limite, 50);

            List<AvaliacaoDTO> avaliacoes =
                    AvaliacaoDAO.listarPorAnuncioPaginado(idAnuncio, pagina, limite);

            int totalAvaliacoes = AvaliacaoDAO.contarTotalAvaliacoes(idAnuncio);
            int totalPaginas = (int) Math.ceil(totalAvaliacoes / (double) limite);

            RespostaPaginadaDTO resposta = new RespostaPaginadaDTO(
                    pagina,
                    limite,
                    totalAvaliacoes,
                    totalPaginas,
                    avaliacoes
            );

            res.setStatus(HttpServletResponse.SC_OK);
            res.getWriter().print(GSON.toJson(resposta));

        } catch (Exception e) {
            e.printStackTrace();
            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            res.getWriter().print("{\"erro\": \"Erro interno ao buscar avaliações.\"}");
        }
    }

    // REGISTRA OU ATUALIZA UMA AVALIAÇÃO
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        prepararResposta(res);

        try {
            Integer idUsuarioToken = obterIdUsuarioToken(req);

            if (idUsuarioToken == null) {
                res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                res.getWriter().print("{\"erro\": \"Token inválido ou ausente.\"}");
                return;
            }

            Usuario usuarioReq = UsuarioDAO.buscarPorId(idUsuarioToken);

            if (usuarioReq == null) {
                res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                res.getWriter().print("{\"erro\": \"Usuário inválido.\"}");
                return;
            }

            if (usuarioEstaBanido(usuarioReq)) {
                responderContaBanida(res);
                return;
            }

            String idAnuncioParam = req.getParameter("idAnuncio");
            String notaParam = req.getParameter("nota");
            String comentarioParam = req.getParameter("comentario");

            if (idAnuncioParam == null || notaParam == null) {
                res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                res.getWriter().print("{\"erro\": \"idAnuncio e nota são obrigatórios.\"}");
                return;
            }

            int idAnuncio;
            double nota;

            try {
                idAnuncio = Integer.parseInt(idAnuncioParam.trim());
                nota = Double.parseDouble(notaParam.trim());
            } catch (NumberFormatException e) {
                res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                res.getWriter().print(
                        "{\"erro\": \"idAnuncio ou nota em formato inválido.\"}");
                return;
            }

            if (idAnuncio < 1) {
                res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                res.getWriter().print("{\"erro\": \"idAnuncio inválido.\"}");
                return;
            }

            if (!Double.isFinite(nota) || nota < 1 || nota > 5 || nota != Math.rint(nota)) {
                res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                res.getWriter().print(
                        "{\"erro\": \"A nota deve ser um número inteiro entre 1 e 5.\"}");
                return;
            }

            String comentario =
                    (comentarioParam == null || comentarioParam.trim().isEmpty())
                            ? null
                            : comentarioParam.trim();

            if (comentario != null && comentario.length() > LIMITE_COMENTARIO) {
                res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                res.getWriter().print(
                        "{\"erro\": \"O comentário deve ter no máximo 1000 caracteres.\"}");
                return;
            }

            AnuncioDetalheDTO anuncio =
                    AnuncioDAO.pegarAnuncioDetalhe(idAnuncio, idUsuarioToken);

            if (anuncio == null) {
                res.setStatus(HttpServletResponse.SC_NOT_FOUND);
                res.getWriter().print("{\"erro\": \"Anúncio não encontrado.\"}");
                return;
            }

            if (anuncio.getIdUsuario() == idUsuarioToken) {
                res.setStatus(HttpServletResponse.SC_FORBIDDEN);
                res.getWriter().print(
                        "{\"erro\": \"Você não pode avaliar seu próprio anúncio.\"}");
                return;
            }

            boolean jaAvaliouAntes =
                    AvaliacaoDAO.usuarioJaAvaliou(idAnuncio, idUsuarioToken);

            Avaliacao avaliacao = new Avaliacao();
            avaliacao.setIdAnuncio(idAnuncio);
            avaliacao.setIdUsuario(idUsuarioToken);
            avaliacao.setNota(nota);
            avaliacao.setComentario(comentario);
            avaliacao.setDataAvaliacao(LocalDateTime.now());

            if (AvaliacaoDAO.registrar(avaliacao)) {
                if (jaAvaliouAntes) {
                    res.setStatus(HttpServletResponse.SC_OK);
                    res.getWriter().print(
                            "{\"mensagem\": \"Avaliação atualizada com sucesso!\"}");
                } else {
                    res.setStatus(HttpServletResponse.SC_CREATED);
                    res.getWriter().print(
                            "{\"mensagem\": \"Avaliação registrada com sucesso!\"}");
                }
            } else {
                res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                res.getWriter().print("{\"erro\": \"Falha ao salvar avaliação no banco.\"}");
            }

        } catch (Exception e) {
            e.printStackTrace();
            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            res.getWriter().print(
                    "{\"erro\": \"Erro interno no processamento da avaliação.\"}");
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        prepararResposta(res);

        try {
            Integer idUsuarioToken = obterIdUsuarioToken(req);

            if (idUsuarioToken == null) {
                res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                res.getWriter().print("{\"erro\": \"Token inválido ou ausente.\"}");
                return;
            }

            Usuario usuarioReq = UsuarioDAO.buscarPorId(idUsuarioToken);

            if (usuarioReq == null) {
                res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                res.getWriter().print("{\"erro\": \"Usuário inválido.\"}");
                return;
            }

            if (usuarioEstaBanido(usuarioReq)) {
                responderContaBanida(res);
                return;
            }

            JsonObject json;

            try {
                json = GSON.fromJson(req.getReader(), JsonObject.class);
            } catch (RuntimeException e) {
                res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                res.getWriter().print("{\"erro\": \"JSON inválido.\"}");
                return;
            }

            if (json == null
                    || !json.has("idAvaliacao")
                    || json.get("idAvaliacao").isJsonNull()) {
                res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                res.getWriter().print(
                        "{\"erro\": \"O campo idAvaliacao é obrigatório.\"}");
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
                res.getWriter().print(
                        "{\"mensagem\": \"Avaliação removida com sucesso!\"}");
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

}
