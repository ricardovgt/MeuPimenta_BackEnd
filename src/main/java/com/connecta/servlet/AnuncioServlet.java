package com.connecta.servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;

import com.connecta.dao.AnuncioDAO;
import com.connecta.dao.UsuarioDAO;
import com.connecta.dto.AnuncioCardDTO;
import com.connecta.dto.AnuncioDetalheDTO;
import com.connecta.dto.AnuncioRequestDTO;
import com.connecta.dto.MeusAnunciosDTO;
import com.connecta.entity.Anuncio;
import com.connecta.entity.Usuario;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/anuncios")
public class AnuncioServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        prepararResposta(res);

        try {
            String idParam = req.getParameter("id");

            // 1. BUSCAR DETALHE DE UM ANÚNCIO
            if (idParam != null && !idParam.trim().isEmpty()) {
                try {
                    int idAnuncio = Integer.parseInt(idParam.trim());
                    int idUsuarioRequisitante = obterIdUsuarioTokenOuPadrao(req, -1);

                    AnuncioDetalheDTO anuncio =
                            AnuncioDAO.pegarAnuncioDetalhe(idAnuncio, idUsuarioRequisitante);

                    if (anuncio != null) {
                        res.setStatus(HttpServletResponse.SC_OK);
                        res.getWriter().print(gson.toJson(anuncio));
                    } else {
                        res.setStatus(HttpServletResponse.SC_NOT_FOUND);
                        res.getWriter().print("{\"erro\": \"Anúncio não encontrado.\"}");
                    }
                } catch (NumberFormatException e) {
                    res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    res.getWriter().print("{\"erro\": \"ID inválido.\"}");
                }

                return;
            }

            // 2. LISTAR OS ANÚNCIOS DO USUÁRIO LOGADO
            if ("true".equalsIgnoreCase(req.getParameter("meus"))) {
                Integer idUsuarioToken = obterIdUsuarioToken(req);

                if (idUsuarioToken == null) {
                    res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    res.getWriter().print("{\"erro\": \"Token inválido ou ausente.\"}");
                    return;
                }

                List<MeusAnunciosDTO> meusAnuncios = AnuncioDAO.listarPorUsuario(idUsuarioToken);

                res.setStatus(HttpServletResponse.SC_OK);
                res.getWriter().print(gson.toJson(meusAnuncios));
                return;
            }

            // 3. BUSCA GERAL DE ANÚNCIOS
            String busca = req.getParameter("busca");
            String topParam = req.getParameter("top");
            String tipoParam = req.getParameter("tipo");
            boolean topAvaliacoes = "true".equalsIgnoreCase(topParam);

            String tipo = normalizarTipo(tipoParam);
            if (tipoParam != null && !tipoParam.trim().isEmpty() && tipo == null) {
                res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                res.getWriter().print(
                        "{\"erro\": \"Tipo inválido. Use SERVICO ou COMERCIO.\"}");
                return;
            }

            List<AnuncioCardDTO> anuncios =
                    AnuncioDAO.buscarAnunciosCard(busca, topAvaliacoes, tipo);

            res.setStatus(HttpServletResponse.SC_OK);
            res.getWriter().print(gson.toJson(anuncios));

        } catch (Exception e) {
            e.printStackTrace();
            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            res.getWriter().print("{\"erro\": \"Erro interno do servidor ao buscar anúncios.\"}");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
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

            String corpo = lerCorpo(req);
            JsonObject jsonBody = tentarLerJsonObject(corpo);

            // AÇÃO DE DENÚNCIA
            String acao = req.getParameter("acao");

            if ((acao == null || acao.trim().isEmpty())
                    && jsonBody != null
                    && jsonBody.has("acao")
                    && !jsonBody.get("acao").isJsonNull()) {
                acao = jsonBody.get("acao").getAsString();
            }

            if ("DENUNCIAR".equalsIgnoreCase(acao) || "DENUNCIA".equalsIgnoreCase(acao)) {
                Integer idAnuncio = extrairIdAnuncioDenuncia(req, jsonBody);

                if (idAnuncio == null || idAnuncio < 1) {
                    res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    res.getWriter().print("{\"erro\": \"idAnuncio inválido ou não informado.\"}");
                    return;
                }

                String resultado = AnuncioDAO.registrarDenuncia(idAnuncio, idUsuarioToken);

                if ("NAO_ENCONTRADO".equals(resultado)) {
                    res.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    res.getWriter().print("{\"erro\": \"Anúncio não encontrado.\"}");
                    return;
                }

                if ("PROPRIO".equals(resultado)) {
                    res.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    res.getWriter().print("{\"erro\": \"Você não pode denunciar seu próprio anúncio.\"}");
                    return;
                }

                if ("INDISPONIVEL".equals(resultado)) {
                    res.setStatus(HttpServletResponse.SC_CONFLICT);
                    res.getWriter().print("{\"erro\": \"Este anúncio não está disponível para denúncia.\"}");
                    return;
                }

                if ("DUPLICADA".equals(resultado)) {
                    res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    res.getWriter().print("{\"erro\": \"Você já denunciou este anúncio.\"}");
                    return;
                }

                if (!"SUCESSO".equals(resultado)) {
                    res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    res.getWriter().print("{\"erro\": \"Erro interno ao registrar denúncia.\"}");
                    return;
                }

                res.setStatus(HttpServletResponse.SC_OK);
                res.getWriter().print("{\"mensagem\": \"Denúncia registrada com sucesso.\"}");
                return;
            }

            // CADASTRO DE ANÚNCIO
            if (usuarioReq.getTipoConta() == null
                    || !usuarioReq.getTipoConta().equalsIgnoreCase("COMERCIAL")) {
                res.setStatus(HttpServletResponse.SC_FORBIDDEN);
                res.getWriter().print(
                        "{\"erro\": \"Apenas contas do tipo COMERCIAL podem publicar anúncios.\"}");
                return;
            }

            AnuncioRequestDTO dadosRecebidos;
            try {
                dadosRecebidos = gson.fromJson(corpo, AnuncioRequestDTO.class);
            } catch (Exception e) {
                res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                res.getWriter().print(
                        "{\"erro\": \"Corpo da requisição em formato JSON inválido.\"}");
                return;
            }

            if (dadosRecebidos == null) {
                res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                res.getWriter().print("{\"erro\": \"Dados do anúncio não informados.\"}");
                return;
            }

            String nome = dadosRecebidos.getNome();
            String descricao = dadosRecebidos.getDescricao();
            String telefone = dadosRecebidos.getTelefone();
            String descricaoDetalhada = dadosRecebidos.getDescricaoDetalhada();
            String tipo = normalizarTipo(dadosRecebidos.getTipo());

            if (nome == null || telefone == null
                    || nome.trim().isEmpty() || telefone.trim().isEmpty()) {
                res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                res.getWriter().print("{\"erro\": \"Nome e telefone são obrigatórios.\"}");
                return;
            }

            if (tipo == null) {
                res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                res.getWriter().print(
                        "{\"erro\": \"Tipo inválido. Use SERVICO ou COMERCIO.\"}");
                return;
            }

            Anuncio anuncio = new Anuncio();
            anuncio.setIdUsuario(idUsuarioToken);
            anuncio.setNome(nome.trim());
            anuncio.setDescricao(descricao != null ? descricao.trim() : "");
            anuncio.setTelefone(telefone.trim());
            anuncio.setDescricaoDetalhada(
                    descricaoDetalhada != null ? descricaoDetalhada.trim() : "");
            anuncio.setTipo(tipo);

            if (AnuncioDAO.cadastrar(anuncio, dadosRecebidos.getFotos())) {
                res.setStatus(HttpServletResponse.SC_CREATED);
                res.getWriter().print("{\"mensagem\": \"Anúncio cadastrado com sucesso!\"}");
            } else {
                res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                res.getWriter().print(
                        "{\"erro\": \"Falha ao salvar o anúncio no banco de dados.\"}");
            }

        } catch (Exception e) {
            e.printStackTrace();
            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            res.getWriter().print(
                    "{\"erro\": \"Erro interno no processamento do anúncio.\"}");
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
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

            String corpo = lerCorpo(req);

            AnuncioRequestDTO dadosRecebidos;
            try {
                dadosRecebidos = gson.fromJson(corpo, AnuncioRequestDTO.class);
            } catch (Exception e) {
                res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                res.getWriter().print(
                        "{\"erro\": \"Corpo da requisição em formato JSON inválido.\"}");
                return;
            }

            if (dadosRecebidos == null) {
                res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                res.getWriter().print("{\"erro\": \"Dados do anúncio não informados.\"}");
                return;
            }

            int idAnuncio = dadosRecebidos.getId();

            if (idAnuncio < 1) {
                res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                res.getWriter().print("{\"erro\": \"ID do anúncio inválido.\"}");
                return;
            }

            String novoStatus = normalizarStatusEditavel(dadosRecebidos.getStatus());
            boolean statusInformado = dadosRecebidos.getStatus() != null
                    && !dadosRecebidos.getStatus().trim().isEmpty();

            if (statusInformado && novoStatus == null) {
                res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                res.getWriter().print(
                        "{\"erro\": \"Status inválido. O usuário pode usar apenas ATIVO ou OCULTO.\"}");
                return;
            }

            boolean temDadosDeConteudo =
                    dadosRecebidos.getNome() != null
                    || dadosRecebidos.getTelefone() != null
                    || dadosRecebidos.getDescricao() != null
                    || dadosRecebidos.getDescricaoDetalhada() != null
                    || dadosRecebidos.getTipo() != null
                    || dadosRecebidos.getFotos() != null;

            // Permite alterar somente o status, por exemplo para pausar/reativar anúncio.
            if (statusInformado && !temDadosDeConteudo) {
                boolean statusAtualizado =
                        AnuncioDAO.alterarStatus(idAnuncio, idUsuarioToken, novoStatus);

                if (!statusAtualizado) {
                    res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    res.getWriter().print(
                            "{\"erro\": \"Não foi possível alterar o status. Verifique se o anúncio existe, se você é o dono e se ele não está banido.\"}");
                    return;
                }

                res.setStatus(HttpServletResponse.SC_OK);
                res.getWriter().print("{\"mensagem\": \"Status do anúncio atualizado com sucesso!\"}");
                return;
            }

            if (dadosRecebidos.getNome() == null
                    || dadosRecebidos.getNome().trim().isEmpty()
                    || dadosRecebidos.getTelefone() == null
                    || dadosRecebidos.getTelefone().trim().isEmpty()) {
                res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                res.getWriter().print("{\"erro\": \"Nome e telefone são obrigatórios.\"}");
                return;
            }

            String tipo = null;
            boolean tipoInformado = dadosRecebidos.getTipo() != null
                    && !dadosRecebidos.getTipo().trim().isEmpty();

            if (tipoInformado) {
                tipo = normalizarTipo(dadosRecebidos.getTipo());
            }

            if (tipoInformado && tipo == null) {
                res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                res.getWriter().print(
                        "{\"erro\": \"Tipo inválido. Use SERVICO ou COMERCIO.\"}");
                return;
            }

            Anuncio anuncio = new Anuncio();
            anuncio.setId(idAnuncio);
            anuncio.setNome(dadosRecebidos.getNome().trim());
            anuncio.setDescricao(
                    dadosRecebidos.getDescricao() != null
                            ? dadosRecebidos.getDescricao().trim()
                            : "");
            anuncio.setTelefone(dadosRecebidos.getTelefone().trim());
            anuncio.setDescricaoDetalhada(
                    dadosRecebidos.getDescricaoDetalhada() != null
                            ? dadosRecebidos.getDescricaoDetalhada().trim()
                            : "");
            anuncio.setTipo(tipo);

            boolean atualizado =
                    AnuncioDAO.atualizar(
                            anuncio,
                            idUsuarioToken,
                            dadosRecebidos.getFotos(),
                            statusInformado ? novoStatus : null);

            if (!atualizado) {
                res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                res.getWriter().print(
                        "{\"erro\": \"Não foi possível atualizar o anúncio. Verifique se ele existe e se você é o dono.\"}");
                return;
            }

            res.setStatus(HttpServletResponse.SC_OK);
            res.getWriter().print("{\"mensagem\": \"Anúncio atualizado com sucesso!\"}");

        } catch (Exception e) {
            e.printStackTrace();
            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            res.getWriter().print("{\"erro\": \"Erro interno ao atualizar anúncio.\"}");
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        prepararResposta(res);

        try {
            Integer idUsuarioToken = obterIdUsuarioToken(req);
            String emailToken = (String) req.getAttribute("emailUsuarioToken");

            if (idUsuarioToken == null || emailToken == null) {
                res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                res.getWriter().print("{\"erro\": \"Token inválido ou ausente.\"}");
                return;
            }

            String idParam = req.getParameter("id");
            String emailParam = req.getParameter("email");

            if (idParam == null || idParam.trim().isEmpty()
                    || emailParam == null || emailParam.trim().isEmpty()) {
                res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                res.getWriter().print(
                        "{\"erro\": \"Os parâmetros id e email são obrigatórios.\"}");
                return;
            }

            int idAnuncio;
            try {
                idAnuncio = Integer.parseInt(idParam.trim());
            } catch (NumberFormatException e) {
                res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                res.getWriter().print("{\"erro\": \"ID inválido.\"}");
                return;
            }

            if (!emailParam.trim().equalsIgnoreCase(emailToken)) {
                res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                res.getWriter().print(
                        "{\"erro\": \"O e-mail digitado não coincide com o e-mail da sua conta.\"}");
                return;
            }

            boolean excluido = AnuncioDAO.deletar(idAnuncio, idUsuarioToken);

            if (excluido) {
                res.setStatus(HttpServletResponse.SC_OK);
                res.getWriter().print("{\"mensagem\": \"Anúncio excluído com sucesso!\"}");
            } else {
                res.setStatus(HttpServletResponse.SC_NOT_FOUND);
                res.getWriter().print(
                        "{\"erro\": \"Anúncio não encontrado ou você não tem permissão para excluí-lo.\"}");
            }

        } catch (Exception e) {
            e.printStackTrace();
            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            res.getWriter().print("{\"erro\": \"Erro interno ao excluir anúncio.\"}");
        }
    }

    private void prepararResposta(HttpServletResponse res) {
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");
    }

    private String lerCorpo(HttpServletRequest req) throws IOException {
        StringBuilder sb = new StringBuilder();

        try (BufferedReader reader = req.getReader()) {
            String linha;
            while ((linha = reader.readLine()) != null) {
                sb.append(linha);
            }
        }

        return sb.toString();
    }

    private JsonObject tentarLerJsonObject(String corpo) {
        if (corpo == null || corpo.trim().isEmpty()) {
            return null;
        }

        try {
            return gson.fromJson(corpo, JsonObject.class);
        } catch (Exception e) {
            return null;
        }
    }

    private Integer extrairIdAnuncioDenuncia(HttpServletRequest req, JsonObject jsonBody) {
        String idParam = req.getParameter("idAnuncio");

        if (idParam == null || idParam.trim().isEmpty()) {
            idParam = req.getParameter("id");
        }

        if (idParam != null && !idParam.trim().isEmpty()) {
            try {
                return Integer.parseInt(idParam.trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }

        if (jsonBody != null
                && jsonBody.has("idAnuncio")
                && !jsonBody.get("idAnuncio").isJsonNull()) {
            try {
                return jsonBody.get("idAnuncio").getAsInt();
            } catch (Exception e) {
                return null;
            }
        }

        return null;
    }

    private Integer obterIdUsuarioToken(HttpServletRequest req) {
        Object valor = req.getAttribute("idUsuarioToken");

        if (valor instanceof Integer) {
            return (Integer) valor;
        }

        return null;
    }

    private int obterIdUsuarioTokenOuPadrao(HttpServletRequest req, int padrao) {
        Integer idUsuarioToken = obterIdUsuarioToken(req);
        return idUsuarioToken != null ? idUsuarioToken : padrao;
    }

    private boolean usuarioEstaBanido(Usuario usuario) {
        return usuario.getStatus() != null
                && "BANIDO".equalsIgnoreCase(usuario.getStatus().trim());
    }

    private void responderContaBanida(HttpServletResponse res) throws IOException {
        res.setStatus(HttpServletResponse.SC_FORBIDDEN);
        res.getWriter().print(
                "{\"erro\": \"Sua conta foi banida por violação das regras de conduta.\"}");
    }

    private String normalizarTipo(String tipo) {
        if (tipo == null || tipo.trim().isEmpty()) {
            return null;
        }

        String tipoNormalizado = tipo.trim().toUpperCase();

        if (!"SERVICO".equals(tipoNormalizado) && !"COMERCIO".equals(tipoNormalizado)) {
            return null;
        }

        return tipoNormalizado;
    }

    private String normalizarStatusEditavel(String status) {
        if (status == null || status.trim().isEmpty()) {
            return null;
        }

        String statusNormalizado = status.trim().toUpperCase();

        // BANIDO é reservado à moderação automática.
        if (!"ATIVO".equals(statusNormalizado) && !"OCULTO".equals(statusNormalizado)) {
            return null;
        }

        return statusNormalizado;
    }
}
