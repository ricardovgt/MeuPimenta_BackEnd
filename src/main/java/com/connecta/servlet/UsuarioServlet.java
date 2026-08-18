package com.connecta.servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Date;

import org.apache.commons.validator.routines.EmailValidator;
import org.mindrot.jbcrypt.BCrypt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.connecta.dao.AnuncioDAO;
import com.connecta.dao.UsuarioDAO;
import com.connecta.dto.UsuarioRequestDTO;
import com.connecta.dto.UsuarioResponseDTO;
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

    // doGet: RESPONSÁVEL POR LISTAR OS DADOS DO USUÁRIO
    @Override
	protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
	    res.setContentType("application/json");
	    res.setCharacterEncoding("UTF-8");
	
	    try {
	        int idDoToken = (int) req.getAttribute("idUsuarioToken");
	
	        // Busca a entidade Usuario
	        Usuario usuario = UsuarioDAO.buscarPorId(idDoToken);
	
	        if (usuario != null) {
	            // Converte a entidade para o DTO seguro
	            UsuarioResponseDTO dto = new UsuarioResponseDTO(
	                usuario.getId(),
	                usuario.getNome(),
	                usuario.getEmail(),
	                usuario.getTipoConta(),
	                usuario.getFotoPerfil()
	            );
	
	            Gson gson = new Gson();
	            res.setStatus(HttpServletResponse.SC_OK);
	            res.getWriter().print(gson.toJson(dto));
	        } else {
	            res.setStatus(HttpServletResponse.SC_NOT_FOUND);
	            res.getWriter().print("{\"erro\": \"Usuário não encontrado.\"}");
	        }
	    } catch (Exception e) {
	        e.printStackTrace();
	        res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
	        res.getWriter().print("{\"erro\": \"Erro interno ao buscar perfil.\"}");
	}
    }
    
    // doPut: RESPONSÁVEL POR ATUALIZAR DADOS DO USUÁRIO (FOTO, NOME, EMAIL, SENHA, TIPO DE CONTA)
    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");

        try {
            int idDoToken = (int) req.getAttribute("idUsuarioToken");

            Usuario usuarioAtual = UsuarioDAO.buscarPorId(idDoToken);

            if (usuarioAtual == null) {
                res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                res.getWriter().print("{\"erro\": \"Usuário inválido.\"}");
                return;
            }

            if (usuarioAtual.getStatus() != null
                    && "BANIDO".equalsIgnoreCase(usuarioAtual.getStatus().trim())) {
                res.setStatus(HttpServletResponse.SC_FORBIDDEN);
                res.getWriter().print(
                        "{\"erro\": \"Sua conta foi banida por violação das regras de conduta.\"}");
                return;
            }

            StringBuilder sb = new StringBuilder();
            BufferedReader reader = req.getReader();
            String linha;
            while ((linha = reader.readLine()) != null) {
                sb.append(linha);
            }

            UsuarioRequestDTO dadosRecebidos;
            try {
                dadosRecebidos = new Gson().fromJson(sb.toString(), UsuarioRequestDTO.class);
            } catch (Exception e) {
                res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                res.getWriter().print("{\"erro\": \"Corpo da requisição em formato JSON inválido.\"}");
                return;
            }

            if (dadosRecebidos == null) {
                res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                res.getWriter().print("{\"erro\": \"Dados do usuário não informados.\"}");
                return;
            }

            // 3. EXTRAIR OS DADOS DO DTO
            String fotoPerfil = dadosRecebidos.getFotoPerfil();
            String novoTipo = dadosRecebidos.getTipoConta();
            String novoNome = dadosRecebidos.getNome();
            String novoEmail = dadosRecebidos.getEmail();
            String senhaAtual = dadosRecebidos.getSenhaAtual();
            String novaSenha = dadosRecebidos.getNovaSenha();
            String confirmarNovaSenha = dadosRecebidos.getConfirmarNovaSenha();

            // Se enviou Foto de Perfil:
            if (fotoPerfil != null && !fotoPerfil.trim().isEmpty()) {
                boolean atualizado = UsuarioDAO.atualizarFotoPerfil(idDoToken, fotoPerfil);
                if (atualizado) {
                    res.setStatus(HttpServletResponse.SC_OK);
                    res.getWriter().print("{\"mensagem\": \"Foto de perfil atualizada com sucesso!\"}");
                    return;
                }
            }

            // Se enviou Tipo de Conta:
            if (novoTipo != null && !novoTipo.trim().isEmpty()) {
                String tipoNormalizado = novoTipo.trim().toUpperCase();

                if (!tipoNormalizado.equals("COMERCIAL") && !tipoNormalizado.equals("COMUM")) {
                    res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    res.getWriter().print("{\"erro\": \"Tipo de conta inválido. Use COMUM ou COMERCIAL.\"}");
                    return;
                }

                if (senhaAtual == null || senhaAtual.trim().isEmpty()) {
                    res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    res.getWriter().print("{\"erro\": \"Senha atual é obrigatória para trocar o tipo de conta.\"}");
                    return;
                }

                String senhaHash = UsuarioDAO.buscarSenhaHashPorId(idDoToken);
                if (senhaHash == null || !BCrypt.checkpw(senhaAtual, senhaHash)) {
                    res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    res.getWriter().print("{\"erro\": \"Senha atual incorreta.\"}");
                    return;
                }

                // Ao trocar o tipo de conta, remove todos os anúncios já publicados
                AnuncioDAO.excluirTodosPorUsuario(idDoToken);

                boolean atualizado = UsuarioDAO.atualizarTipoConta(idDoToken, tipoNormalizado);
                if (atualizado) {
                    res.setStatus(HttpServletResponse.SC_OK);
                    res.getWriter().print("{\"mensagem\": \"Tipo de conta atualizado com sucesso!\"}");
                    return;
                }
            }

            // Se enviou Nome:
            if (novoNome != null && !novoNome.trim().isEmpty()) {
                String nomeLimpo = novoNome.trim();

                if (nomeLimpo.length() < 3) {
                    res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    res.getWriter().print("{\"erro\": \"O nome deve ter pelo menos 3 caracteres.\"}");
                    return;
                }
                
                if (nomeLimpo.length() > 30) {
                    res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    res.getWriter().print("{\"erro\": \"O nome tem um limite máximo de 50 caracteres.\"}");
                    return;
                }

                boolean atualizado = UsuarioDAO.atualizarNome(idDoToken, nomeLimpo);
                if (atualizado) {
                    res.setStatus(HttpServletResponse.SC_OK);
                    res.getWriter().print("{\"mensagem\": \"Nome atualizado com sucesso!\"}");
                    return;
                }
            }

            // Se enviou Email:
            if (novoEmail != null && !novoEmail.trim().isEmpty()) {
                String emailLimpo = novoEmail.trim().toLowerCase();

                if (!EmailValidator.getInstance().isValid(emailLimpo)) {
                    res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    res.getWriter().print("{\"erro\": \"O formato do e-mail inserido é inválido.\"}");
                    return;
                }

                if (senhaAtual == null || senhaAtual.trim().isEmpty()) {
                    res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    res.getWriter().print("{\"erro\": \"Senha atual é obrigatória para trocar o e-mail.\"}");
                    return;
                }

                String senhaHash = UsuarioDAO.buscarSenhaHashPorId(idDoToken);
                if (senhaHash == null || !BCrypt.checkpw(senhaAtual, senhaHash)) {
                    res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    res.getWriter().print("{\"erro\": \"Senha atual incorreta.\"}");
                    return;
                }

                Usuario usuarioComEsseEmail = UsuarioDAO.buscarPorEmail(emailLimpo);
                if (usuarioComEsseEmail != null && usuarioComEsseEmail.getId() != idDoToken) {
                    res.setStatus(HttpServletResponse.SC_CONFLICT);
                    res.getWriter().print("{\"erro\": \"Este e-mail já está cadastrado por outro usuário.\"}");
                    return;
                }

                boolean atualizado = UsuarioDAO.atualizarEmail(idDoToken, emailLimpo);
                if (atualizado) {
                    res.setStatus(HttpServletResponse.SC_OK);
                    res.getWriter().print("{\"mensagem\": \"E-mail atualizado com sucesso!\"}");
                    return;
                }
            }

            // Se enviou troca de Senha:
            if (novaSenha != null && !novaSenha.trim().isEmpty()) {
                if (senhaAtual == null || senhaAtual.trim().isEmpty()) {
                    res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    res.getWriter().print("{\"erro\": \"Senha atual é obrigatória para trocar a senha.\"}");
                    return;
                }

                if (confirmarNovaSenha == null || !novaSenha.equals(confirmarNovaSenha)) {
                    res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    res.getWriter().print("{\"erro\": \"A confirmação da nova senha não confere.\"}");
                    return;
                }

                String senhaHash = UsuarioDAO.buscarSenhaHashPorId(idDoToken);
                if (senhaHash == null || !BCrypt.checkpw(senhaAtual, senhaHash)) {
                    res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    res.getWriter().print("{\"erro\": \"Senha atual incorreta.\"}");
                    return;
                }

                String novaSenhaHash = BCrypt.hashpw(novaSenha, BCrypt.gensalt(12));
                boolean atualizado = UsuarioDAO.atualizarSenha(idDoToken, novaSenhaHash);
                if (atualizado) {
                    res.setStatus(HttpServletResponse.SC_OK);
                    res.getWriter().print("{\"mensagem\": \"Senha atualizada com sucesso!\"}");
                    return;
                }
            }

            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            res.getWriter().print("{\"erro\": \"Parâmetros inválidos para atualização.\"}");

        } catch (Exception e) {
            e.printStackTrace();
            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            res.getWriter().print("{\"erro\": \"Erro interno ao atualizar usuário.\"}");
        }
    }
    
    // doDelete: RESPONSÁVEL POR EXCLUIR A CONTA DO USUÁRIO (EXIGE EMAIL + SENHA)
    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");

        try {
            int idDoToken = (int) req.getAttribute("idUsuarioToken");

            StringBuilder sb = new StringBuilder();
            BufferedReader reader = req.getReader();
            String linha;
            while ((linha = reader.readLine()) != null) {
                sb.append(linha);
            }

            UsuarioRequestDTO dadosRecebidos;
            try {
                dadosRecebidos = new Gson().fromJson(sb.toString(), UsuarioRequestDTO.class);
            } catch (Exception e) {
                res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                res.getWriter().print("{\"erro\": \"Corpo da requisição em formato JSON inválido.\"}");
                return;
            }

            if (dadosRecebidos == null || dadosRecebidos.getEmail() == null || dadosRecebidos.getSenhaAtual() == null
                    || dadosRecebidos.getEmail().trim().isEmpty() || dadosRecebidos.getSenhaAtual().trim().isEmpty()) {
                res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                res.getWriter().print("{\"erro\": \"E-mail e senha são obrigatórios para excluir a conta.\"}");
                return;
            }

            Usuario usuario = UsuarioDAO.buscarPorId(idDoToken);
            if (usuario == null) {
                res.setStatus(HttpServletResponse.SC_NOT_FOUND);
                res.getWriter().print("{\"erro\": \"Conta não encontrada.\"}");
                return;
            }

            String emailDigitado = dadosRecebidos.getEmail().trim().toLowerCase();
            if (!emailDigitado.equals(usuario.getEmail())) {
                res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                res.getWriter().print("{\"erro\": \"E-mail não corresponde à conta autenticada.\"}");
                return;
            }

            String senhaHash = UsuarioDAO.buscarSenhaHashPorId(idDoToken);
            if (senhaHash == null || !BCrypt.checkpw(dadosRecebidos.getSenhaAtual(), senhaHash)) {
                res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                res.getWriter().print("{\"erro\": \"Senha incorreta.\"}");
                return;
            }

            AnuncioDAO.excluirTodosPorUsuario(idDoToken);
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
}