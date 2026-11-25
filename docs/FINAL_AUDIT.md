# Auditoria Final contra a Especificação Oficial — EventIF

**Data da auditoria:** 25/11/2026  
**Documento de referência:** `docs/especificacao_oficial.pdf` (`24/11/2026`) e `AGENTS.md`  
**Ambiente verificado:** JDK `21.0.2`, Spring Boot `3.4.5`, Maven Wrapper `3.9.16`, perfil `test` (H2 em memória com `MODE=MySQL`) para suíte automatizada, `application.properties` configurado para MySQL 8.4 em execução e Docker Compose v5.6.0 (validação estática de configuração).

---

## 1. Resumo Executivo e Veredito Geral

- **Requisitos de software obrigatórios (`RF01`–`RF14`, `RN01`–`RN07`, `RS01`–`RS07`, `RI01`–`RI03`, `T01`–`T10`, `RT01`–`RT05`):** **100% implementados e aprovados (`PASSOU`)** na auditoria estática de código/templates e validados pela suíte automatizada (`48/48` testes passando em `14` classes com `0` falhas, `0` erros e `0` ignorados, incluindo os extras `EX01`–`EX04` adicionados na Fase 15).
- **Build e empacotamento:** `.\mvnw.cmd clean test` e `.\mvnw.cmd package` executaram com `BUILD SUCCESS`, gerando `target/eventif-0.0.1-SNAPSHOT.jar`.
- **Ressalvas honestas (critérios humanos/operacionais de entrega):**
  1. **Repositório Git / GitHub:** commits organizados na branch `main` com remote `origin` configurado; resta criar e enviar a tag `final` somente após o preenchimento autoral do `README.md` e gravação do vídeo.
  2. **Placeholders humanos e vídeo explicativo:** os campos de identificação do integrante único e o link do vídeo no `README.md` permanecem como placeholders aguardando preenchimento e gravação pelo autor.

---

## 2. Verificação de Stack, Modelo de Domínio e Arquitetura

| Item | Exigência da Especificação | Evidência Verificada | Status |
|---|---|---|---|
| Stack Java / Spring | Java 21, Spring Boot 3.x, Spring MVC + Thymeleaf, JPA, Spring Security 6, Bean Validation | `pom.xml` (`java.version=21`, `spring-boot-starter-parent:3.4.5`), zero uso de SPA | PASSOU |
| Banco de dados | MySQL em execução; H2 exclusivamente em `test` | `application.properties` (`jdbc:mysql://...`), `pom.xml` (`h2` em `<scope>test</scope>`) | PASSOU |
| 6 Entidades + 2 Enums + Extra `EX01` | `Usuario`, `Perfil`, `Categoria`, `Evento`, `Atividade`, `Inscricao`, `StatusEvento`, `TipoAtividade` + `ListaEspera` (adicionada na Fase 15 para `EX01`) | `DominioJpaMappingTest`, `ConcorrenciaEArquiteturaTest` (`7` entidades verificadas, incluindo as `6` obrigatórias) | PASSOU |
| Sem entidade Certificado | Certificado gerado dinamicamente sem entidade JPA | `CertificadoService`, `CertificadoView`, `ConcorrenciaEArquiteturaTest` | PASSOU |
| Compatibilidade DDL MySQL | Nenhuma tabela/coluna usando palavra reservada do MySQL 8.x | `Evento.local` mapeado para `local_evento` (`ADR-14`), validado em `CompatibilidadeMysqlSchemaTest` | PASSOU |
| Separação de camadas (`RT01`) | Estritamente `controller → service → repository` | `ConcorrenciaEArquiteturaTest#rt01ControllersNaoDevemAcessarRepositoriesNemReceberEntidadesJpaEmEndpointsESemEntidadeCertificado` | PASSOU |

---

## 3. Auditoria Requisito por Requisito

### 3.1 Requisitos Funcionais (`RF01`–`RF14`)

| Código | Requisito | Evidência Principal (Classe / Método / Rota / Template) | Teste Verificado | Status |
|---|---|---|---|---|
| **RF01** | Cadastro público atribuindo `ROLE_PARTICIPANTE` | `AuthController#cadastrar`, `CadastroUsuarioForm`, `UsuarioService#cadastrarParticipante` · `GET/POST /cadastro` · `auth/cadastro.html` | `FluxoPublicoEParticipanteControllerTest#rf01...`, `RegrasNegocioServiceTest#rn01ERf01...` | PASSOU |
| **RF02** | Login e logout por formulário próprio | `SecurityConfig#securityFilterChain`, `AuthController#login`, `CustomUserDetailsService` · `GET/POST /login`, `POST /logout` · `auth/login.html` | `SecurityAccessTest#deveAutenticarUsuarioAtivoPorEmail...` | PASSOU |
| **RF03** | (Admin) CRUD completo de `Categoria` | `AdminCategoriaController`, `CategoriaForm`, `CategoriaService`, `CategoriaRepository` · `/admin/categorias/**` · `admin/categorias.html` | `AdminModuloControllerTest#rf03...` | PASSOU |
| **RF04** | (Admin) Listar usuários e conceder/retirar `ROLE_ORGANIZADOR` | `AdminUsuarioController`, `UsuarioResumoView`, `UsuarioService#listarTodosParaAdmin`, `concederPerfilOrganizador`, `removerPerfilOrganizador` · `/admin/usuarios/**` · `admin/usuarios.html` | `AdminModuloControllerTest#rf04...` | PASSOU |
| **RF05** | (Organizador) CRUD de eventos próprios nascendo em `RASCUNHO` | `OrganizadorEventoController`, `EventoForm`, `EventoService#criar`, `atualizar`, `excluir` · `/organizador/eventos/**` · `organizador/eventos.html`, `evento-form.html` | `OrganizadorModuloEIdorControllerTest#rf05ERf06...`, `RegrasNegocioServiceTest#rn02...` | PASSOU |
| **RF06** | (Organizador) Publicar e encerrar evento próprio | `OrganizadorEventoController#publicar`, `encerrar`, `EventoService#publicar`, `encerrar`, `StatusEvento` · `POST /organizador/eventos/{id}/publicar`, `/encerrar` | `OrganizadorModuloEIdorControllerTest#rf05ERf06...`, `RegressaoCompletaControllerClockTest#cenario7...` | PASSOU |
| **RF07** | (Organizador) CRUD de atividades dentro de evento próprio | `OrganizadorAtividadeController`, `AtividadeForm`, `AtividadeService` · `/organizador/eventos/{eId}/atividades/**` · `organizador/atividades.html` | `OrganizadorModuloEIdorControllerTest#rf07ERf08...`, `RegrasNegocioServiceTest#rn02...` | PASSOU |
| **RF08** | (Organizador) Listar inscritos e marcar presença via checkboxes | `OrganizadorAtividadeController#listarInscritos`, `registrarPresencas`, `InscricaoService#registrarPresencas` (`required=false`) · `organizador/inscritos.html` | `OrganizadorModuloEIdorControllerTest#rf07ERf08...` | PASSOU |
| **RF09** | Página inicial pública com `PUBLICADO`, paginação e filtro | `HomeController#index`, `EventoService#listarPublicados`, `EventoRepository` · `GET /`, `GET /eventos` · `publico/index.html` | `FluxoPublicoEParticipanteControllerTest#rf09ERf10...`, `RepositoryConsultasTest` | PASSOU |
| **RF10** | Detalhes do evento com atividades e vagas restantes | `HomeController#detalhes`, `EventoService#buscarDetalhePublico`, `EventoDetalheView`, `AtividadeComVagasView` · `GET /eventos/{id}` · `publico/evento-detalhe.html` | `FluxoPublicoEParticipanteControllerTest#rf09ERf10...` | PASSOU |
| **RF11** | (Participante) Inscrever-se em atividade | `ParticipanteController#inscrever`, `InscricaoService#inscrever` · `POST /inscricoes/atividades/{atividadeId}` | `FluxoPublicoEParticipanteControllerTest#rf11Rf12Rf13ERs06...`, `RegressaoCompletaControllerClockTest#cenario3...`, `ConcorrenciaEArquiteturaTest` | PASSOU |
| **RF12** | (Participante) Cancelar própria inscrição | `ParticipanteController#cancelar`, `InscricaoService#cancelar` · `POST /inscricoes/{id}/cancelar` | `FluxoPublicoEParticipanteControllerTest#rf11Rf12Rf13ERs06...`, `RegressaoCompletaControllerClockTest#cenario4...` | PASSOU |
| **RF13** | (Participante) Página "Minhas inscrições" com presença | `ParticipanteController#minhasInscricoes`, `MinhaInscricaoView`, `InscricaoService#listarMinhasInscricoes` · `GET /inscricoes` · `participante/minhas-inscricoes.html` | `FluxoPublicoEParticipanteControllerTest#rf11Rf12Rf13ERs06...`, `RegressaoCompletaControllerClockTest#cenarios5E6...` | PASSOU |
| **RF14** | (Participante) Certificado imprimível de evento `ENCERRADO` | `ParticipanteController#certificado`, `CertificadoService#emitirCertificado`, `CertificadoView` · `GET /certificados/eventos/{eventoId}` · `participante/certificado.html` (`@media print`) | `FluxoPublicoEParticipanteControllerTest#rf14ERn07...`, `InterfaceThymeleafTest` | PASSOU |

### 3.2 Regras de Negócio (`RN01`–`RN07`)

| Código | Regra | Implementação no `service` / BD | Teste Verificado | Status |
|---|---|---|---|---|
| **RN01** | E-mail único | `UsuarioService#cadastrarParticipante` + constraint `uk_usuarios_email` | `RegrasNegocioServiceTest#rn01ERf01...`, `DominioJpaMappingTest#deveRejeitarEmailDuplicadoNoBanco` | PASSOU |
| **RN02** | `dataFim >= dataInicio`; `fim > inicio` dentro do período do evento | `EventoService#validarPeriodoEvento`, `validarAtividadesExistentesDentroDoNovoPeriodo`, `AtividadeService#validarDadosAtividade` | `RegrasNegocioServiceTest#rn02...`, `OrganizadorModuloEIdorControllerTest`, `RegressaoCompletaControllerClockTest#cenario7...` | PASSOU |
| **RN03** | Inscrição apenas em evento `PUBLICADO` | `InscricaoService#inscrever` (`StatusEvento.PUBLICADO`) | `RegrasNegocioServiceTest#rn03Rn04ERn05...` | PASSOU |
| **RN04** | Bloqueio de inscrição duplicada | `InscricaoService#inscrever` + constraint `uk_inscricoes_usuario_atividade` | `RegrasNegocioServiceTest#rn03Rn04ERn05...`, `DominioJpaMappingTest`, `ConcorrenciaEArquiteturaTest#rn04...` | PASSOU |
| **RN05** | Bloqueio sem vagas com lock transacional | `AtividadeRepository#findByIdComLock` (`PESSIMISTIC_WRITE`) + `InscricaoService#inscrever` | `RegrasNegocioServiceTest#rn03Rn04ERn05...`, `RegressaoCompletaControllerClockTest#cenario3...`, `ConcorrenciaEArquiteturaTest#rn05...` | PASSOU |
| **RN06** | Cancelamento apenas antes do início | `InscricaoService#cancelar` (`LocalDateTime.now(clock).isBefore(atividade.getInicio())`) | `RegrasNegocioServiceTest#rn06...`, `RegressaoCompletaControllerClockTest#cenario4...` | PASSOU |
| **RN07** | Certificado somente para `ENCERRADO` e presença >= 1 | `CertificadoService#emitirCertificado` + `InscricaoRepository#findPresentesPorUsuarioIdEEventoId` | `RegrasNegocioServiceTest#rn07ERf14...`, `FluxoPublicoEParticipanteControllerTest#rf14ERn07...`, `RegressaoCompletaControllerClockTest#cenarios5E6...` | PASSOU |

### 3.3 Requisitos de Segurança (`RS01`–`RS07`)

| Código | Requisito | Implementação Verificada | Teste Verificado | Status |
|---|---|---|---|---|
| **RS01** | `SecurityFilterChain` e login próprio | `SecurityConfig` (`.loginPage("/login")`, logout POST com CSRF) | `SecurityAccessTest` | PASSOU |
| **RS02** | `UserDetailsService` por e-mail bloqueando inativos | `CustomUserDetailsService`, `CustomUserDetails#isEnabled` | `SecurityAccessTest#deveAutenticarUsuarioAtivoPorEmail...` | PASSOU |
| **RS03** | Senhas com `BCryptPasswordEncoder` | `SecurityConfig#passwordEncoder`, `UsuarioService`, `DataInitializer` | `SecurityAccessTest#deveCriarAdminInicialComBCryptDeFormaIdempotente`, `DataInitializerTest` | PASSOU |
| **RS04** | Matriz URL × Perfil no servidor | `SecurityConfig` (`/inscricoes/**` e `/certificados/**` -> `PARTICIPANTE`, `/organizador/**` -> `ORGANIZADOR`, `/admin/**` -> `ADMIN`) | `SecurityAccessTest`, `AdminModuloControllerTest`, `RegressaoCompletaControllerClockTest#cenario8...` | PASSOU |
| **RS05** | Dono do evento/atividade/presença; troca de ID -> HTTP 403 | `EventoService#buscarEventoDoOrganizadorOu403`, `AtividadeService#buscarAtividadeDoOrganizadorOu403`, `AcessoNegadoPropriedadeException` (`403`) | `OrganizadorModuloEIdorControllerTest#rs05TrocarIdDeEventoOuAtividade...`, `RegrasNegocioServiceTest#rs05ERs06...` | PASSOU |
| **RS06** | Participante só vê/cancela próprias inscrições; ID da sessão | `SecurityService#obterUsuarioAutenticadoId`, `InscricaoService#cancelar` (`AcessoNegadoPropriedadeException`) | `FluxoPublicoEParticipanteControllerTest#rf11Rf12Rf13ERs06...`, `RegressaoCompletaControllerClockTest#cenarios5E6...` | PASSOU |
| **RS07** | Menus condicionais `sec:authorize` + `ADMIN` inicial | `fragments/layout.html` (`sec:authorize`), `DataInitializer` | `SecurityAccessTest`, `DataInitializerTest` | PASSOU |

### 3.4 Interface (`RI01`–`RI03`), 10 Telas (`T01`–`T10`) e Requisitos Técnicos (`RT01`–`RT05`)

| Grupo | Itens Cobertos | Evidências Verificadas | Status |
|---|---|---|---|
| **RI01–RI03** | Layout único com fragments, `th:object`/`th:field` + erros por campo, flash attributes | `fragments/layout.html`, `GlobalExceptionHandler` (`detalheErro`), todos os templates e controllers PRG | `InterfaceThymeleafTest`, `FluxoPublicoEParticipanteControllerTest`, `OrganizadorModuloEIdorControllerTest`, `AdminModuloControllerTest` -> **PASSOU** |
| **T01–T10** | Todas as 10 telas mínimas + páginas de erro `403`, `404`, `500`, `regra-negocio` | `publico/{index,evento-detalhe}`, `auth/{login,cadastro}`, `participante/{minhas-inscricoes,certificado}`, `organizador/{eventos,evento-form,atividades,inscritos}`, `admin/{categorias,usuarios}` | `InterfaceThymeleafTest` (todas as 10 telas + `@media print`) -> **PASSOU** |
| **RT01** | Camadas `controller → service → repository` | Inspeção arquitetural automatizada em todos os 7 controllers | `ConcorrenciaEArquiteturaTest#rt01...` -> **PASSOU** |
| **RT02** | Spring Data JPA derivada e paginada | `EventoRepository#findByStatus`, `findByStatusAndCategoriaId`, `buscarPorStatusECategoriaOpcional` | `RepositoryConsultasTest` -> **PASSOU** |
| **RT03** | Bean Validation nos formulários | `@NotBlank`, `@Email`, `@Size`, `@NotNull`, `@Positive` nos 4 Form DTOs | `FluxoPublicoEParticipanteControllerTest`, `OrganizadorModuloEIdorControllerTest`, `AdminModuloControllerTest` -> **PASSOU** |
| **RT04** | Carga idempotente de dados de exemplo | `DataInitializer` (3 perfis, 4 usuários, 2 categorias, 3 eventos, 6 atividades, 3 inscrições) | `DataInitializerTest` -> **PASSOU** |
| **RT05** | `README.md`, `Dockerfile`, `docker-compose.yml` | Artefatos na raiz + `docker compose up --build -d` validado em runtime (`mysql:8.4.13` `healthy` + `app` HTTP 200 em `/` e `/login`) + `.\mvnw.cmd package` (`BUILD SUCCESS`) | **PASSOU** |

---

## 4. Checklist dos 11 Itens Críticos do Prompt 14

| # | Verificação Crítica | Resultado |
|---|---|---|
| 1 | Cadastro público sempre cria `ROLE_PARTICIPANTE` (ignora parâmetros extras como `perfis=ROLE_ADMIN`) | **PASSOU** (`FluxoPublicoEParticipanteControllerTest#rf01...`) |
| 2 | Participante não acessa rotas de organizador/admin (`HTTP 403`) | **PASSOU** (`SecurityAccessTest`, `AdminModuloControllerTest`, `RegressaoCompletaControllerClockTest`) |
| 3 | Organizador não altera evento/atividade/chamada alheia, inclusive trocando ID na URL (`HTTP 403`) | **PASSOU** (`OrganizadorModuloEIdorControllerTest#rs05...` — 13 vetores 403) |
| 4 | Participante não vê nem cancela inscrições de terceiros (`HTTP 403` e filtro por sessão) | **PASSOU** (`FluxoPublicoEParticipanteControllerTest`, `RegressaoCompletaControllerClockTest`) |
| 5 | Inscrição bloqueia evento não publicado, duplicidade e lotação (com concorrência real) | **PASSOU** (`RegrasNegocioServiceTest`, `ConcorrenciaEArquiteturaTest`) |
| 6 | Cancelamento após o início da atividade é negado (`Clock` controlável) | **PASSOU** (`RegrasNegocioServiceTest#rn06...`, `RegressaoCompletaControllerClockTest#cenario4...`) |
| 7 | Certificado só aparece para evento `ENCERRADO` com presença | **PASSOU** (`RegrasNegocioServiceTest#rn07...`, `RegressaoCompletaControllerClockTest#cenarios5E6...`) |
| 8 | Carga horária soma apenas atividades com `presente = true` | **PASSOU** (`RegrasNegocioServiceTest`, `FluxoPublicoEParticipanteControllerTest`, `DataInitializerTest`) |
| 9 | Página inicial tem paginação e filtro por categoria | **PASSOU** (`FluxoPublicoEParticipanteControllerTest#rf09ERf10...`, `RepositoryConsultasTest`) |
| 10 | Docker sobe aplicação e MySQL | **PASSOU** — `docker compose up --build -d` executado em runtime: `eventif-db-1` (`mysql:8.4`, MySQL `8.4.13`) `healthy` e `eventif-app-1` `Started EventifApplication in 7.147 seconds`, respondendo `HTTP 200` em `http://localhost:8080/` (com dados de exemplo do MySQL) e `/login`. |
| 11 | `README.md` permite executar o projeto seguindo apenas as instruções | **PASSOU** para instruções Docker e manuais + credenciais/variáveis; **PARCIAL** apenas nos placeholders autorais (nome do integrante e link do vídeo). |

---

## 5. Correções Aplicadas Durante Esta Auditoria

1. **Compatibilidade real com MySQL 8.x (`ADR-14`):** identificado que o atributo `Evento.local` gerava coluna física `local` no DDL (`LOCAL` é palavra reservada do MySQL 8.x, que passava no H2 mas falharia ao subir no MySQL em Docker). Mapeado para `@Column(name = "local_evento")` em `Evento.java` e adicionado `CompatibilidadeMysqlSchemaTest`.
2. **Eliminação de alerta duplicado nas páginas de erro:** `GlobalExceptionHandler` e os templates `error/{403,404,regra-negocio}.html` passaram a usar o atributo `detalheErro` (em vez de `mensagemErro`), evitando renderização dupla pelo fragmento `alerts` de `fragments/layout.html`.

---

## 6. Pendências Honestas Antes da Entrega Definitiva

1. **Docker Compose em runtime (`CONCLUÍDO`):** validação real executada com `docker compose up --build -d` (`db` MySQL `8.4.13` `healthy` e `app` respondendo `HTTP 200` em `http://localhost:8080/` e `/login`).
2. **Preenchimento de autoria e vídeo (ação humana):**
   - Gravar vídeo explicativo (máx. 10 min) cumprindo o roteiro da Seção 10.2 do PDF.
   - Substituir os placeholders em `README.md` (nome completo, matrícula, GitHub/e-mail e link do vídeo).
3. **Tag `final` no Git para entrega:** os commits estruturados da branch `main` foram registrados e enviados ao remoto `origin`; falta apenas criar e enviar a tag `final` após preencher os placeholders do `README.md` e inserir o link do vídeo.
4. **Extras opcionais (`EX01`–`EX04`):** todos os 4 extras foram implementados e validados na **Fase 15** com status **`PASSOU`** (`ListaEspera` FIFO + promoção automática em `InscricaoService`, validação de horários sobrepostos `findConflitosDeHorario`, geração de PDF A4 paisagem via `CertificadoPdfService` / OpenPDF `1.3.43`, e `48/48` testes em `14` classes incluindo `FuncionalidadesExtrasTest`), mantendo 100% dos requisitos obrigatórios funcionando.
