# Connecta API

API Java para cadastro de usuários, autenticação, publicação de anúncios,
avaliações e denúncias. O projeto usa Jakarta Servlets, JDBC e MySQL e é
empacotado como um arquivo WAR.

## Requisitos

- Java 25
- Maven
- MySQL 8.0
- Servidor compatível com Jakarta Servlet 6.1

## Configuração

Configure estas variáveis de ambiente antes de iniciar a aplicação:

- `DB_URL`: URL JDBC do banco MySQL.
- `DB_USER`: usuário do banco.
- `DB_PASSWORD`: senha do banco.
- `JWT_SECRET`: chave usada para assinar e validar os tokens JWT.

Para gerar o WAR:

```shell
mvn clean package
```

O arquivo gerado fica em `target/connecta-api.war`.

## Endpoints

### Autenticação e usuário

- `POST /login`: autentica pelos parâmetros `email` e `senha`.
- `POST /usuario`: cadastra uma conta usando `nome`, `email`, `senha` e
  `tipo_conta` (`COMUM` ou `COMERCIAL`).
- `GET /usuario`: retorna o perfil autenticado.
- `PUT /usuario`: altera foto, tipo de conta, nome, e-mail ou senha. A troca
  de e-mail devolve um novo token, que deve substituir o token anterior.
- `DELETE /usuario`: exclui a conta após confirmar e-mail e senha.

### Anúncios

- `GET /anuncios`: lista anúncios ativos com paginação.
- `GET /anuncios?id={id}`: retorna os detalhes de um anúncio.
- `GET /anuncios?meus=true`: lista os anúncios da conta autenticada.
- `GET /anuncios?destaques=true`: retorna três anúncios em destaque.
- `POST /anuncios`: cadastra um anúncio para uma conta comercial ou registra
  uma denúncia quando a ação `DENUNCIAR` é informada.
- `PUT /anuncios`: edita o conteúdo ou o status de um anúncio do usuário.
- `DELETE /anuncios`: exclui um anúncio do usuário.

A listagem aceita `busca`, `tipo`, `top`, `pagina` e `limite`. A descrição
curta aceita até 255 caracteres e a descrição detalhada até 2.000.

### Avaliações

- `GET /avaliacoes?idAnuncio={id}`: lista avaliações com paginação.
- `POST /avaliacoes`: cria ou atualiza uma avaliação.
- `DELETE /avaliacoes`: remove uma avaliação do usuário autenticado.

As notas devem ser números inteiros de 1 a 5. O comentário é opcional e
aceita até 1.000 caracteres.

## Autenticação

As rotas protegidas esperam o token no cabeçalho:

```http
Authorization: Bearer <token>
```
