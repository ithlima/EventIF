# AGENTS.md — Regras Permanentes do Projeto EventIF

Este arquivo define regras obrigatórias para qualquer agente ou desenvolvedor que atuar neste repositório.

## 1. Leitura obrigatória antes de alterar código
- Sempre leia `docs/PROJECT_CONTEXT.md` e `docs/REQUIREMENTS_TRACEABILITY.md` antes de iniciar qualquer alteração relevante.
- Consulte `docs/ARCHITECTURE.md`, `docs/DECISIONS.md` e `docs/IMPLEMENTATION_PLAN.md` para respeitar a ordem das etapas e as decisões registradas.
- A especificação oficial em `docs/especificacao_oficial.pdf` é a **fonte primária de verdade** dos requisitos. Em caso de conflito com qualquer outro documento, o PDF prevalece.

## 2. Stack e decisões técnicas inegociáveis
- **Java 21** (`JAVA_HOME=C:\Users\Almeida\jdk21\jdk-21.0.2` neste ambiente local), **Spring Boot 3.x**, **Maven** (usar `./mvnw` / `mvnw.cmd`).
- **Interface server-side:** Spring MVC + **Thymeleaf** (Bootstrap 5 + CSS próprio). **Proibido usar SPA** (React, Angular, Vue ou frontend separado) ou migrar para Spring Boot 4.
- **Persistência:** Spring Data JPA / Hibernate com **MySQL 8.4** (via Docker Compose) para execução e **H2 exclusivamente no escopo `test`**.
- **Segurança:** Spring Security 6 com formulário próprio (`/login`), `UserDetailsService` por e-mail e `BCryptPasswordEncoder`.
- **Validação:** Jakarta Bean Validation (`@NotBlank`, `@Email`, `@Positive`, `@NotNull`, etc.).
- **Certificado:** **não criar entidade JPA para Certificado**. Gerar a visualização/impressão (e futuramente o PDF do `EX03`) dinamicamente a partir das inscrições com `presente = true` em eventos `ENCERRADO`.

## 3. Arquitetura e separação de responsabilidades
- Seguir estritamente `controller → service → repository` (`RT01`).
- **Proibido colocar regra de negócio em controllers ou templates Thymeleaf.** Todas as regras (`RN01`–`RN07`) e verificações de propriedade (`RS05`–`RS06`) ficam na camada `service`.
- Usar sempre **DTOs/Form objects** dedicados nos formulários e listagens sensíveis; nunca vincular formulários diretamente às entidades JPA (`Usuario`, `Evento`, etc.) nem expor hashes de senha.

## 4. Segurança no servidor (obrigatória)
- Nunca confiar apenas em botões/links ocultos com `sec:authorize`: toda proteção deve existir no servidor (`SecurityFilterChain` + validações no `service`).
- **Identidade pela sessão (`RS06`):** nunca obter `usuarioId` ou `organizadorId` a partir de parâmetro de URL, formulário ou campo oculto. Sempre extrair do `Principal` / `SecurityContext`.
- **Proteção contra IDOR (`RS05`):** ao consultar, editar, excluir, publicar, encerrar ou gerenciar atividades/presenças, validar no `service` que o registro pertence ao organizador autenticado. Tentar acessar ou alterar registro de outro organizador trocando o ID na URL deve retornar **HTTP 403**.
- **Matriz de Acesso:** rotas de inscrição, cancelamento, "Minhas inscrições" e certificado exigem `ROLE_PARTICIPANTE` (não ficando sob `/eventos/**`).
- Nunca versionar senhas reais, tokens ou arquivos `.env` com segredos. Usar variáveis de ambiente e manter valores fictícios apenas em `.env.example` / `README.md`.

## 5. Qualidade, verificação e disciplina de fluxo
- A partir do Prompt 2, executar `./mvnw test` (ou `./mvnw clean test` / `./mvnw package`) com JDK 21 após qualquer alteração de código e **nunca avançar de etapa com testes ou build falhando**.
- Manter `docs/REQUIREMENTS_TRACEABILITY.md` atualizado **apenas com evidências reais e verificadas** (classe, método, rota, template, teste executado).
- Registrar novas decisões arquiteturais em `docs/DECISIONS.md` e seguir a ordem do `docs/IMPLEMENTATION_PLAN.md`.
- Implementar e auditar primeiro **100% dos requisitos obrigatórios** (`RF01`–`RF14`, `RN01`–`RN07`, `RS01`–`RS07`, `RI01`–`RI03`, `RT01`–`RT05`) até a Fase 14 antes de implementar os extras aprovados (`EX01`–`EX04`) na Fase 15.
- **Não apagar trabalho existente nem arquivos oficiais** (`docs/especificacao_oficial.pdf`, `EventIF_Antigravity_Prompts.md`) sem explicar o motivo e obter autorização explícita do usuário.
