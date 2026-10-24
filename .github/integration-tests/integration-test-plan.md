# Plano de testes de integração

## Estratégia

Executar os endpoints de autenticação através do `MockMvc`, com o contexto Spring completo, serviços reais, repositórios JPA e banco H2 em memória. Nenhum serviço ou repositório da aplicação é mockado. As requisições HTTP percorrem validação, segurança, lógica de negócio e persistência.

## Cobertura existente e lacunas

Os testes existentes cobrem entidades, JWT e serviços isoladamente com mocks. Não verificavam a interação entre controller, configuração de segurança, serviços e banco de dados.

## Componentes e cenários

- Cadastro: persiste usuário, senha codificada e refresh token associado.
- Login: valida credenciais persistidas e rotaciona refresh token.
- Renovação: valida e rotaciona token persistido.
- Validação de entrada: rejeita dados inválidos sem persistir usuário.
- Erros: rejeita e-mail duplicado, senha incorreta e refresh token inexistente.

## Configuração e dependências

- Dependência de teste: H2.
- Configuração: `src/test/resources/application.yml`.
- Hibernate cria e remove as tabelas em cada contexto de teste.
- Flyway fica desativado nos testes porque a migração atual usa `gen_random_uuid()`, específico do PostgreSQL.
- Os testes são identificados com a tag JUnit `Layer1` e a classe termina em `L1Test`.

## Critérios de validação

Executar `mvnw verify -Dgroups=Layer1` (ou `.\mvnw.cmd verify -Dgroups=Layer1` no Windows), confirmar que os sete cenários são descobertos e passam, e verificar dados e estados de token diretamente nos repositórios da aplicação.
