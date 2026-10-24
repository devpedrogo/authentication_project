# 🔐 Authentication Module

API de autenticação com Spring Boot, PostgreSQL e tokens JWT. Permite cadastrar usuários, iniciar sessão e renovar tokens. 🚀

## 📋 Requisitos

- ☕ Java 21
- 🐘 PostgreSQL

## ⚙️ Configuração

Defina as variáveis de ambiente antes de iniciar a aplicação:

| Variável | Descrição |
| --- | --- |
| `DB_URL` | URL JDBC do PostgreSQL, por exemplo `jdbc:postgresql://localhost:5432/authentication_db` |
| `DB_USER` | Usuário do banco |
| `DB_PASSWORD` | Senha do banco |
| `JWT_SECRET` | Chave usada para assinar os tokens |

O banco informado em `DB_URL` deve existir. As tabelas são criadas pelo Flyway ao iniciar a aplicação. Configure uma chave `JWT_SECRET` própria e segura, especialmente fora do ambiente local. 🔒

## ▶️ Executar

No Windows, usando PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

Em sistemas Unix:

```bash
./mvnw spring-boot:run
```

A API fica disponível em `http://localhost:8080`. A documentação interativa está em `http://localhost:8080/swagger-ui/index.html`. 📚

## 🌐 Endpoints

Todos os endpoints usam o prefixo `/api/v1/auth`.

| Método | Rota | Descrição |
| --- | --- | --- |
| `POST` | `/register` | Cadastra um usuário e retorna tokens |
| `POST` | `/login` | Autentica um usuário e retorna tokens |
| `POST` | `/refresh-token` | Renova os tokens usando um refresh token |

Exemplo de cadastro: ✍️

```json
{
  "name": "Ana",
  "email": "ana@example.com",
  "password": "senha123"
}
```

O login recebe `email` e `password`. A renovação recebe `refreshToken`. As respostas de autenticação incluem `access_token` e `refresh_token`. 🎟️

O access token expira em 15 minutos e o refresh token em 7 dias. ⏱️

## 🧪 Testes

```powershell
.\mvnw.cmd test
```

Em sistemas Unix, use `./mvnw test`.
