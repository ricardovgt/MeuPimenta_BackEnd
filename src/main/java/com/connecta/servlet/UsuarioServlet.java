package com.connecta.servlet;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.validator.routines.EmailValidator;
import org.mindrot.jbcrypt.BCrypt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
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

    // doPost: RESPONSÁVEL PELO CADASTRO DO USUÁRIO
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
	    res.setContentType("application/json"); 
	    res.setCharacterEncoding("UTF-8");
	    
	    try {
	        String nome = req.getParameter("nome");
	        String email = req.getParameter("email") != null ? req.getParameter("email").trim().toLowerCase() : null;
	        String senha = req.getParameter("senha");
	        String tipoConta = req.getParameter("tipo_conta");
	        
	        if (nome == null || email == null || senha == null || nome.trim().isEmpty() || email.isEmpty()) {
	            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
	            res.getWriter().print("{\"erro\": \"Campos obrigatórios ausentes.\"}");
	            return;
	        }
	        
	        // Validação do tipo de conta (nunca confie só no que vem do front)
	        if (tipoConta == null || (!tipoConta.equalsIgnoreCase("COMUM") && !tipoConta.equalsIgnoreCase("COMERCIAL"))) {
	            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
	            res.getWriter().print("{\"erro\": \"Tipo de conta inválido. Use COMUM ou COMERCIAL.\"}");
	            return;
	        }
	        
	        // Validação de e-mail simplificada e robusta usando Apache Commons Validator
	        if (!EmailValidator.getInstance().isValid(email)) {
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
	        usuario.setTipoConta(tipoConta.toUpperCase());
	        
	        UsuarioDAO.cadastrar(usuario);
	        
	        Usuario usuarioCadastrado = UsuarioDAO.buscarPorEmail(email);
	        
	        Algorithm algoritmo = Algorithm.HMAC256(com.connecta.conexao.Conexao.JWT_SECRET);
	        String token = JWT.create()
	            .withIssuer("connecta-api")
	            .withClaim("id", usuarioCadastrado.getId())
	            .withClaim("email", usuarioCadastrado.getEmail())
	            .withExpiresAt(new Date(System.currentTimeMillis() + 2629800000L))
	            .sign(algoritmo);
	        
	        res.setStatus(HttpServletResponse.SC_CREATED);
	        res.getWriter().print("{\"mensagem\": \"Sucesso\", \"token\": \"" + token + "\"}");
	        
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

        try {
            int idDoToken = (int) req.getAttribute("idUsuarioToken");

            boolean contaExcluida = UsuarioDAO.descadastrar(idDoToken);
            
            if (contaExcluida) {
                res.setStatus(HttpServletResponse.SC_OK);
                res.getWriter().print("{\"mensagem\": \"Sucesso, conta excluída!\"}");
            } else {
                res.setStatus(HttpServletResponse.SC_NOT_FOUND);
                res.getWriter().print("{\"erro\": \"Conta não encontrada.\"}");
            }

        } catch (Exception e) {
            e.printStackTrace();
            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            res.getWriter().print("{\"erro\": \"Erro ao tentar excluir a conta.\"}");
        }
    }
    
    // doGet: RESPONSÁVEL POR LISTAR OS DADOS DO USUÁRIO
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");

        try {
            String emailDoToken = (String) req.getAttribute("emailUsuarioToken");
            
            Usuario usuarioBanco = UsuarioDAO.buscarPorEmail(emailDoToken);

            if (usuarioBanco == null) {
                res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                res.getWriter().print("{\"erro\": \"Sua conta não existe mais.\"}");
                return;
            }

            Map<String, Object> dados = new HashMap<>();
            dados.put("nome", usuarioBanco.getNome());            
            dados.put("email", usuarioBanco.getEmail());
            dados.put("tipoConta", usuarioBanco.getTipoConta());
            
            String json = new Gson().toJson(dados);
            res.setStatus(HttpServletResponse.SC_OK);
            res.getWriter().print(json);

        } catch (Exception e) {
            e.printStackTrace();
            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            res.getWriter().print("{\"erro\": \"Erro ao buscar perfil do usuário.\"}");
        }
    }
    
    // doPut: RESPONSÁVEL POR ATUALIZAR DADOS DO USUÁRIO (EX: TIPO DE CONTA)
    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");

        try {
            int idDoToken = (int) req.getAttribute("idUsuarioToken");
            
            String novoTipo = req.getParameter("tipoConta");
            
            if (novoTipo == null || (!novoTipo.equalsIgnoreCase("COMERCIAL") && !novoTipo.equalsIgnoreCase("COMUM"))) {
                res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                res.getWriter().print("{\"erro\": \"Tipo de conta inválido. Use COMERCIAL ou COMUM.\"}");
                return;
            }

            boolean atualizado = UsuarioDAO.atualizarTipoConta(idDoToken, novoTipo.toUpperCase());

            if (atualizado) {
                res.setStatus(HttpServletResponse.SC_OK);
                res.getWriter().print("{\"mensagem\": \"Tipo de conta atualizado com sucesso!\"}");
            } else {
                res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                res.getWriter().print("{\"erro\": \"Não foi possível atualizar a conta no banco de dados.\"}");
            }

        } catch (Exception e) {
            e.printStackTrace();
            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            res.getWriter().print("{\"erro\": \"Erro interno ao tentar atualizar o usuário.\"}");
        }
    }
}