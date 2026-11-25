# Plano de Implementação por Etapas — EventIF

Este documento define as fases sequenciais de construção do EventIF, alinhadas aos Prompts 0–15 de `EventIF_Antigravity_Prompts.md` e à `docs/especificacao_oficial.pdf`.

---

### Fase 0 — Leitura da Especificação e Planejamento Arquitetural (`CONCLUÍDA`)
- **Escopo:** Extração integral dos requisitos (`RF01`–`RF14`, `RN01`–`RN07`, `RS01`–`RS07`, `RI01`–`RI03`, `RT01`–`RT05`, `EX01`–`EX04`), comparação entre PDF e roteiro, análise de riscos e definição das decisões de projeto.
- **Critério de conclusão:** Planejamento validado e aprovado pelo usuário.

### Fase 1 — Memória Persistente e Repositório Git (`CONCLUÍDA`)
- **Depende de:** Fase 0.
- **Escopo:** Inicializar Git na branch `main` e criar `AGENTS.md`, `docs/PROJECT_CONTEXT.md`, `docs/REQUIREMENTS_TRACEABILITY.md`, `docs/ARCHITECTURE.md`, `docs/DECISIONS.md` e `docs/IMPLEMENTATION_PLAN.md`.
- **Critério de conclusão:** Os 6 documentos coerentes entre si, sem código prematuro e com rastreador inicializado em `PENDENTE`.

### Fase 2 — Scaffold Maven e Configuração Spring Boot 3.x (`CONCLUÍDA`)
- **Depende de:** Fase 1.
- **Escopo:** `pom.xml` (Java 21.0.2, Spring Boot 3.4.5, Web, Thymeleaf, Security, JPA, Validation, MySQL Driver, H2 em `test`), Maven Wrapper (`mvnw`/`mvnw.cmd` com Maven 3.9.16), `application.properties` (MySQL via env vars), `application-test.properties` (H2), `.gitignore`, estrutura de pacotes, `TimeConfig` (`Clock`) e página inicial provisória.
- **Critério de conclusão:** `.\mvnw.cmd test` e `.\mvnw.cmd package` executaram com sucesso usando JDK 21.0.2.

### Fase 3 — Entidades, Enums e Mapeamentos JPA (`CONCLUÍDA`)
- **Depende de:** Fase 2.
- **Escopo:** `Usuario`, `Perfil`, `Categoria`, `Evento`, `Atividade`, `Inscricao`, `StatusEvento`, `TipoAtividade`, constraints `UNIQUE` (`email`, `nome` e `(usuario_id, atividade_id)`), sem entidade para Certificado e com documentação da decisão de tipos de data (`LocalDate` em `Evento` / `LocalDateTime` em `Atividade`).
- **Critério de conclusão:** `DominioJpaMappingTest` (`@DataJpaTest`) e `EventifApplicationTests` passando no H2 (`6/6` testes verdes) e `.\mvnw.cmd package` verde.

### Fase 4 — Repositories e Consultas Spring Data JPA (`CONCLUÍDA`)
- **Depende de:** Fase 3.
- **Escopo:** 6 interfaces `Repository` (`PerfilRepository`, `UsuarioRepository`, `CategoriaRepository`, `EventoRepository`, `AtividadeRepository`, `InscricaoRepository`) com consultas derivadas (`RT02`, ex.: `findByStatus`), consulta paginada com filtro por categoria (`RF09`), contagem de vagas ocupadas e query com `@Lock(LockModeType.PESSIMISTIC_WRITE)` na `Atividade`.
- **Critério de conclusão:** `RepositoryConsultasTest` (`@DataJpaTest`) e suíte completa (`10/10` testes) e `.\mvnw.cmd package` verdes.

### Fase 5 — Autenticação e Autorização Spring Security 6 (`CONCLUÍDA`)
- **Depende de:** Fases 3 e 4.
- **Escopo:** `SecurityConfig` (`SecurityFilterChain` + `BCryptPasswordEncoder`), página própria `/login`, logout, `CustomUserDetailsService` por e-mail (bloqueando inativos), `SecurityService`, fragments `sec:authorize`, matriz de URLs (`RS04` + Matriz de Acesso), bootstrap idempotente do `ADMIN` (`DataInitializer`) e `.env.example`.
- **Critério de conclusão:** `SecurityAccessTest` (`6/6`) e suíte completa (`16/16` testes) e `.\mvnw.cmd package` verdes.

### Fase 6 — Camada Service e Regras de Negócio `RN01`–`RN07` (`CONCLUÍDA`)
- **Depende de:** Fases 4 e 5.
- **Escopo:** Exceções e `GlobalExceptionHandler` (`403`/`404`/`400`), Form DTOs (`RT03`) e View DTOs, e os 6 services (`UsuarioService`, `CategoriaService`, `EventoService`, `AtividadeService`, `InscricaoService`, `CertificadoService`) com `RN01`–`RN07`, `RS05`/`RS06` e lock pessimista.
- **Critério de conclusão:** `RegrasNegocioServiceTest` (`6/6`) e suíte completa (`22/22` testes) e `.\mvnw.cmd package` verdes.

### Fase 7 — Fluxos Públicos e Módulo do Participante (`CONCLUÍDA`)
- **Depende de:** Fases 5 e 6.
- **Escopo:** `HomeController`, `AuthController`, `ParticipanteController`, DTOs e templates `auth/cadastro.html`, `publico/index.html`, `publico/evento-detalhe.html`, `participante/minhas-inscricoes.html` e `participante/certificado.html` (`RF01`, `RF02`, `RF09`–`RF14`, `RS06`).
- **Critério de conclusão:** `FluxoPublicoEParticipanteControllerTest` (`4/4`) e suíte completa (`26/26` testes) e `.\mvnw.cmd package` verdes.

### Fase 8 — Módulo do Organizador (`CONCLUÍDA`)
- **Depende de:** Fase 6.
- **Escopo:** `OrganizadorEventoController`, `OrganizadorAtividadeController`, projeções seguras e templates `organizador/eventos.html`, `evento-form.html`, `atividades.html` e `inscritos.html` (`RF05`–`RF08`, `RS05`).
- **Critério de conclusão:** `OrganizadorModuloEIdorControllerTest` (`3/3`), suíte completa (`29/29` testes) e `.\mvnw.cmd package` verdes.

### Fase 9 — Módulo do Administrador (`CONCLUÍDA`)
- **Depende de:** Fase 6.
- **Escopo:** `AdminCategoriaController`, `AdminUsuarioController`, proteção de contas `ROLE_ADMIN` e `ROLE_PARTICIPANTE` em `UsuarioService`, templates `admin/categorias.html` e `admin/usuarios.html`, e remoção de `provisorio/placeholder.html`.
- **Critério de conclusão:** `AdminModuloControllerTest` (`3/3`), suíte completa (`32/32` testes) e `.\mvnw.cmd package` verdes.

### Fase 10 — Interface Thymeleaf e Acabamento Visual (`CONCLUÍDA`)
- **Depende de:** Fases 7, 8 e 9.
- **Escopo:** Revisão das 10 telas mínimas (`T01`–`T10`) e telas de erro (`403`, `404`, `500`, `regra-negocio`), layout único `fragments/layout.html` (`page`, `head`, `header`, `menu`, `alerts`, `footer`), `RI01`–`RI03`, `sec:authorize`, identidade visual acadêmica Bootstrap 5 + `eventif.css`, responsividade, `@media print` no certificado e inspeção visual no navegador.
- **Critério de conclusão:** `InterfaceThymeleafTest` (`1/1`), suíte completa (`33/33` testes), inspeção visual e `.\mvnw.cmd package` verdes.

### Fase 11 — Carga de Dados de Exemplo `RT04` (`CONCLUÍDA` · Prompt 11)
- **Depende de:** Fases 7–10.
- **Escopo:** `DataInitializer` idempotente em Java criando perfis, 2 categorias, 4 usuários (`admin`, `organizador`, `organizador2`, `participante`) e 3 eventos com atividades (2 `PUBLICADO` e 1 `ENCERRADO` com presença), usando BCrypt e variáveis de ambiente.
- **Critério de conclusão:** Teste garante idempotência após duas execuções consecutivas sem duplicação.

### Fase 12 — Suíte de Testes Automatizados e Regressão (`CONCLUÍDA` · Prompt 12)
- **Depende de:** Fases 6–11.
- **Escopo:** Cobertura completa de regressão dos 8 cenários mínimos do Prompt 12 (incluindo `RN01`–`RN07`, `RS05`, `RS06` e rotas administrativas).
- **Critério de conclusão:** `./mvnw clean test` e `./mvnw package` 100% verdes com relatório requisito → teste → resultado.

### Fase 13 — Docker e `README.md` de Entrega (`CONCLUÍDA` · Prompt 13)
- **Depende de:** Fase 12.
- **Escopo:** `Dockerfile` multi-stage, `docker-compose.yml` (App + MySQL 8.4 com volume persistente e healthcheck), `.dockerignore`, `.env.example` e `README.md` completo (`RT05`).
- **Critério de conclusão:** `docker compose up --build` validado (caso Docker Desktop esteja ativo) e checklist de `README.md` completo.

### Fase 14 — Auditoria Final dos Requisitos Obrigatórios (`CONCLUÍDA` · Prompt 14)
- **Depende de:** Fase 13.
- **Escopo:** Revisão rigorosa requisito por requisito contra `docs/especificacao_oficial.pdf`, geração de `docs/FINAL_AUDIT.md` e atualização de `docs/REQUIREMENTS_TRACEABILITY.md` com evidências verificadas.
- **Critério de conclusão:** Todos os itens `RF01`–`RF14`, `RN01`–`RN07`, `RS01`–`RS07`, `RI01`–`RI03`, `RT01`–`RT05` e `T01`–`T10` classificados como **`PASSOU`**.

### Fase 15 — Funcionalidades Extras Opcionais `EX01`–`EX04` (`CONCLUÍDA` · Prompt 15)
- **Depende de:** **100% da Fase 14 com status `PASSOU`**.
- **Escopo:**
  1. **`EX01`:** Lista de espera quando vagas se esgotam e promoção automática FIFO no cancelamento (preservando `RN05` e respeitando `EX02`).
  2. **`EX02`:** Bloqueio de inscrição quando houver sobreposição de horário com outra atividade em que o participante esteja inscrito.
  3. **`EX03`:** Geração de certificado em PDF (`/certificados/eventos/{eventoId}/pdf`) usando os dados validados por `RN07`.
  4. **`EX04`:** Consolidação e ampliação dos testes JUnit cobrindo regras obrigatórias e extras.
- **Critério de conclusão:** Suíte completa de regressão (`./mvnw clean test` + `./mvnw package`), `README.md`, `docs/REQUIREMENTS_TRACEABILITY.md` e `docs/FINAL_AUDIT.md` atualizados e verdes.
