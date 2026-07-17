package com.connecta.servlet;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.mindrot.jbcrypt.BCrypt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.connecta.dao.UsuarioDAO;
import com.connecta.entity.Usuario;
import com.google.gson.Gson;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/usuario")
public class UsuarioServlet extends HttpServlet {
    
    private static final long serialVersionUID = 1L;
    
    private static final Set<String> TLDS_VALIDOS = Set.of(
    	    // Genéricos comuns
    	    "com", "net", "org", "edu", "gov", "mil", "int", "info", "biz", "name",
    	    // Brasil (segundo nível — "br" puro foi removido de propósito, ver explicação)
    	    "com.br", "net.br", "org.br", "edu.br", "gov.br", "mil.br",
    	    "adv.br", "med.br", "eng.br", "arq.br", "mus.br", "art.br",
    	    // Outros países comuns (segundo nível)
    	    "co.uk", "org.uk", "me.uk", "co.jp", "co.au", "com.au",
    	    "com.ar", "com.mx", "com.pt", "co.in",
    	    // TLDs modernos comuns
    	    "io", "app", "dev", "ai", "tech", "online", "site", "store",
    	    "cloud", "studio", "agency", "email",
    	    // Países (ISO 3166) — usados apenas para TLDs de nível único (ex: usuario@empresa.de)
    	    "us", "ca", "de", "fr", "es", "it",
    	    "ru", "cn"
    );

    private boolean emailValido(String email) {
   	    if (email == null || email.isBlank()) return false;

   	    String emailLower = email.toLowerCase().trim();

   	    String regex = "^[a-z0-9_+&*-]+(?:\\.[a-z0-9_+&*-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z]{2,10}$";
   	    if (!emailLower.matches(regex)) {
   	    	return false;
    	}

        String dominioParte = emailLower.substring(emailLower.indexOf('@') + 1);
   	    String[] partes = dominioParte.split("\\.");

   	    String tldSimples = partes[partes.length - 1];

    	if (partes.length >= 3) {
    		String tldComposto = partes[partes.length - 2] + "." + partes[partes.length - 1];
    	    if (TLDS_VALIDOS.contains(tldComposto)) {
               return true;
   	        }
   	    }
    	
    	return TLDS_VALIDOS.contains(tldSimples);
    }
    
    // doPost: RESPONSÁVEL PELO CADASTRO DO USUÁRIO
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        res.setContentType("application/json"); 
        res.setCharacterEncoding("UTF-8");
        
        try {
            String nome = req.getParameter("nome");
            String email = req.getParameter("email").trim().toLowerCase();
            String senha = req.getParameter("senha");
            
            if (nome == null || email == null || senha == null || nome.trim().isEmpty() || email.trim().isEmpty()) {
                res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                res.getWriter().print("{\"erro\": \"Campos obrigatórios ausentes.\"}");
                return;
            }
            
            if (!emailValido(email)) {
                res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                res.getWriter().print("{\"erro\": \"O formato do e-mail inserido é inválido.\"}");
                return;
            }
            
            Usuario usuarioExistente = UsuarioDAO.buscarPorEmail(email);
            if (usuarioExistente != null) {
                res.setStatus(HttpServletResponse.SC_CONFLICT);
                res.getWriter().print("{\"erro\": \"Este e-mail já está cadastrado por outro usuário.\"}");
                return;
            }
            
            Usuario usuario = new Usuario();
            usuario.setNome(nome);
            usuario.setEmail(email);
            String senhaHash = BCrypt.hashpw(senha, BCrypt.gensalt(12));
            usuario.setSenha(senhaHash);
            
            UsuarioDAO.cadastrar(usuario);
            
            Usuario usuarioCadastrado = UsuarioDAO.buscarPorEmail(email);
            
            Algorithm algoritmo = Algorithm.HMAC256(com.connecta.conexao.Conexao.JWT_SECRET);
            String token = JWT.create()
                .withIssuer("connecta-api")
                .withClaim("id", usuarioCadastrado.getId())
                .withClaim("email", usuarioCadastrado.getEmail())
                .withExpiresAt(new Date(System.currentTimeMillis() + 2629800000L)) // Expira em 1 mês
                .sign(algoritmo);
            
            res.setStatus(HttpServletResponse.SC_CREATED);
            res.getWriter().print("{\"mensagem\": \"Sucesso\", \"token\": \"" + token + "\"}");
            // --------------------------------------------------------
            
        } catch (Exception e) {
            e.printStackTrace();
            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            res.getWriter().print("{\"erro\": \"Erro ao processar o cadastro no servidor.\"}");
        }
    }

    // doDelete: RESPONSÁVEL POR EXCLUIR A CONTA DO USUÁRIO
    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
    	res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");

        String authHeader = req.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                String token = authHeader.substring(7);
                Algorithm algoritmo = Algorithm.HMAC256(com.connecta.conexao.Conexao.JWT_SECRET);
                DecodedJWT jwt = JWT.require(algoritmo).withIssuer("connecta-api").build().verify(token);

                int idDoToken = jwt.getClaim("id").asInt();

                boolean contaExcluida = UsuarioDAO.descadastrar(idDoToken);
                
                if (contaExcluida) {
                    res.setStatus(HttpServletResponse.SC_OK);
                    res.getWriter().print("{\"mensagem\": \"Sucesso, conta excluída!\"}");
                } else {
                    res.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    res.getWriter().print("{\"erro\": \"Conta não encontrada.\"}");
                }

            } catch (Exception e) {
                res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                res.getWriter().print("{\"erro\": \"Token inválido ou expirado.\"}");
            }
        } else {
            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            res.getWriter().print("{\"erro\": \"Você precisa estar logado para excluir sua conta.\"}");
        }
    }
    
    // doGet: RESPONSÁVEL POR LISTAR OS DADOS DO USUÁRIO
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
    	res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");
        String authHeader = req.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                String token = authHeader.substring(7); 
                
                Algorithm algoritmo = Algorithm.HMAC256(com.connecta.conexao.Conexao.JWT_SECRET);
                DecodedJWT jwt = JWT.require(algoritmo).withIssuer("connecta-api").build().verify(token);

                String emailDoToken = jwt.getClaim("email").asString(); 
                
                Usuario usuarioBanco = UsuarioDAO.buscarPorEmail(emailDoToken);

                if (usuarioBanco == null) {
                    res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    res.getWriter().print("{\"erro\": \"Sua conta não existe mais.\"}");
                    return;
                }

                Map<String,Object> dados = new HashMap<>();
                dados.put("nome", usuarioBanco.getNome());            
                dados.put("email", usuarioBanco.getEmail());
                dados.put("tipoConta", usuarioBanco.getTipoConta());
                
                String json = new Gson().toJson(dados);
                res.getWriter().print(json);

            } catch (Exception e) {
                // Se cair aqui, é porque o Token foi adulterado ou o prazo de 1 mês expirou
                res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                res.getWriter().print("{\"erro\": \"Token inválido ou expirado.\"}");
            }
        } else {
            // Se o aplicativo esqueceu de enviar o cabeçalho Authorization
            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            res.getWriter().print("{\"erro\": \"Você precisa estar logado para ver o perfil.\"}");
        }
    }
    
 // doPut: RESPONSÁVEL POR ATUALIZAR DADOS DO USUÁRIO (EX: TIPO DE CONTA)
    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");

        String authHeader = req.getHeader("Authorization");

        // Verifica se o usuário está logado
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            res.getWriter().print("{\"erro\": \"Autenticação necessária para alterar a conta.\"}");
            return;
        }

        try {
            // Descriptografa e valida o Token JWT
            String token = authHeader.substring(7);
            Algorithm algoritmo = Algorithm.HMAC256(com.connecta.conexao.Conexao.JWT_SECRET);
            DecodedJWT jwt = JWT.require(algoritmo).withIssuer("connecta-api").build().verify(token);

            // Pega o ID do usuário direto do Token (segurança: impede que o usuário mude a conta de outro)
            int idDoToken = jwt.getClaim("id").asInt();
            
            // Pega o parâmetro da requisição (ex: /usuario?tipoConta=COMERCIAL)
            String novoTipo = req.getParameter("tipoConta");
            
            // Validação de segurança para não aceitarem qualquer texto no banco
            if (novoTipo == null || (!novoTipo.equalsIgnoreCase("COMERCIAL") && !novoTipo.equalsIgnoreCase("COMUM"))) {
                res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                res.getWriter().print("{\"erro\": \"Tipo de conta inválido. Use COMERCIAL ou COMUM.\"}");
                return;
            }

            // Chama o DAO que você já deixou preparado
            boolean atualizado = UsuarioDAO.atualizarTipoConta(idDoToken, novoTipo.toUpperCase());

            if (atualizado) {
                res.setStatus(HttpServletResponse.SC_OK);
                res.getWriter().print("{\"mensagem\": \"Tipo de conta atualizado com sucesso!\"}");
            } else {
                res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                res.getWriter().print("{\"erro\": \"Não foi possível atualizar a conta no banco de dados.\"}");
            }

        } catch (com.auth0.jwt.exceptions.JWTVerificationException e) {
            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            res.getWriter().print("{\"erro\": \"Token inválido ou expirado. Faça login novamente.\"}");
        } catch (Exception e) {
            e.printStackTrace();
            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            res.getWriter().print("{\"erro\": \"Erro interno ao tentar atualizar o usuário.\"}");
        }
    }
}