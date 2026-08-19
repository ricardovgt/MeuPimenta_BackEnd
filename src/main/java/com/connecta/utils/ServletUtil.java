package com.connecta.utils;

import java.io.BufferedReader;
import java.io.IOException;

import com.connecta.entity.Usuario;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public final class ServletUtil {
    private static final Gson GSON = new Gson();

    private ServletUtil() {
    }

    public static void prepararResposta(HttpServletResponse res) {
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");
    }

    public static String lerCorpo(HttpServletRequest req) throws IOException {
        StringBuilder corpo = new StringBuilder();

        try (BufferedReader reader = req.getReader()) {
            String linha;
            while ((linha = reader.readLine()) != null) {
                corpo.append(linha);
            }
        }

        return corpo.toString();
    }

    public static JsonObject tentarLerJsonObject(String corpo) {
        if (corpo == null || corpo.trim().isEmpty()) {
            return null;
        }

        try {
            return GSON.fromJson(corpo, JsonObject.class);
        } catch (Exception e) {
            return null;
        }
    }

    public static Integer extrairIdAnuncioDenuncia(
            HttpServletRequest req, JsonObject jsonBody) {
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

    public static Integer obterIdUsuarioToken(HttpServletRequest req) {
        Object valor = req.getAttribute("idUsuarioToken");

        if (valor instanceof Integer) {
            return (Integer) valor;
        }

        return null;
    }

    public static int obterIdUsuarioTokenOuPadrao(HttpServletRequest req, int padrao) {
        Integer idUsuarioToken = obterIdUsuarioToken(req);
        return idUsuarioToken != null ? idUsuarioToken : padrao;
    }

    public static boolean usuarioEstaBanido(Usuario usuario) {
        return usuario.getStatus() != null
                && "BANIDO".equalsIgnoreCase(usuario.getStatus().trim());
    }

    public static void responderContaBanida(HttpServletResponse res) throws IOException {
        res.setStatus(HttpServletResponse.SC_FORBIDDEN);
        res.getWriter().print(
                "{\"erro\": \"Sua conta foi banida por violação das regras de conduta.\"}");
    }

    public static String normalizarTipo(String tipo) {
        if (tipo == null || tipo.trim().isEmpty()) {
            return null;
        }

        String tipoNormalizado = tipo.trim().toUpperCase();

        if (!"SERVICO".equals(tipoNormalizado) && !"COMERCIO".equals(tipoNormalizado)) {
            return null;
        }

        return tipoNormalizado;
    }

    public static String normalizarStatusEditavel(String status) {
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

    public static int lerInteiroOuPadrao(String valor, int padrao) {
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
