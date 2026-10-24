# Resumo dos testes de integração

## Testes adicionados

- `src/test/java/com/devpedrogo/authentication_module/integration/AuthFlowL1Test.java`
  - Cadastro com verificação do usuário persistido, senha codificada e refresh token.
  - Login com rotação e revogação do refresh token anterior.
  - Renovação com rotação e revogação do token consumido.
  - Validação de campos obrigatórios e formato da requisição.
  - Rejeição de e-mail duplicado, senha incorreta e refresh token inexistente.

## Configuração

- `src/test/resources/application.yml` configura H2 em memória, esquema criado pelo Hibernate e Flyway desativado para os testes.
- `pom.xml` adiciona H2 somente no escopo de teste.
- O mapeamento de `RefreshToken.user` passou a `@ManyToOne`, permitindo persistir o token novo sem remover o token antigo revogado, conforme exige a rotação e o esquema da migration.

## Resultado final

- `.\mvnw.cmd verify -Dgroups=Layer1`: 7 testes de integração, 0 falhas, 0 erros.
- `.\mvnw.cmd verify`: 26 testes no total, 0 falhas, 0 erros.
- Runner PowerShell da camada 1: executado com sucesso.

## Como executar

- Windows: `.\.github\integration-tests\run-layer1-tests.ps1`
- Unix: `bash .github/integration-tests/run-layer1-tests.sh`
- Com Maven: `.\mvnw.cmd verify -Dgroups=Layer1` (Windows) ou `./mvnw verify -Dgroups=Layer1` (Unix).
