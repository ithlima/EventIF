# EventIF — Contexto Oficial do Projeto

## 1. Visão Geral e Informações Acadêmicas
- **Sistema:** EventIF — Sistema de Gestão de Eventos Acadêmicos.
- **Disciplina:** Tópicos em Java para a Web — IFCE Campus Fortaleza (Prof. Corneli Gomes Furtado Júnior).
- **Fonte de verdade:** `docs/especificacao_oficial.pdf` (24/11/2026).
- **Modalidade da equipe:** Trabalho **individual** (commits no repositório Git com tag `final` na entrega).
- **Prazo de entrega:** **03/12/2026**.
- **Objetivo funcional:** Divulgar eventos acadêmicos (semanas de curso, congressos, jornadas), receber inscrições em atividades com controle de vagas, registrar presença de participantes e emitir certificados imprimíveis.

### Critérios de Avaliação e Rubrica Oficial
- **Nota base (0 a 10):**
  - Funcionalidades (`RF01`–`RF14` funcionando): **3,0 pts**
  - Regras de negócio (`RN01`–`RN07` no `service`): **2,0 pts**
  - Segurança (login, perfis, URLs e proteção por dono do registro / 403): **2,0 pts**
  - JPA e Thymeleaf (mapeamentos, consultas, fragments, validação): **1,0 pt**
  - `README.md` e execução (sobe seguindo apenas o README; nota máxima exige Docker): **1,0 pt**
  - Vídeo explicativo (máx. 10 min, 3 perfis, teste 403 e explicação do código): **1,0 pt**
- **Extras opcionais (`EX01`–`EX04`):** até **0,5 pt** cada, com **teto de 1,0 pt**; só pontuam se todo o conteúdo obrigatório estiver funcionando. O projeto implementará todos os 4 extras na **Fase 15**, somente após a auditoria aprovada dos obrigatórios.
- **Regra eliminatória parcial:** projeto que não sobe seguindo o `README.md` recebe nota máxima de **5,0** (avaliado apenas por código e vídeo).
- **Fator de arguição individual ($F \in \{1{,}0;\ 0{,}8;\ 0{,}5;\ 0{,}0\}$):** $\text{Nota final} = \min(10,\ (\text{Nota da rubrica} + \text{Extras}) \times F)$.

---

## 2. Stack Tecnológica Obrigatória e Ambiente
- **Linguagem:** Java 21 (`C:\Users\Almeida\jdk21\jdk-21.0.2` no ambiente local).
- **Framework Backend:** Spring Boot 3.x (`3.4.x`), Spring MVC, Spring Data JPA / Hibernate, Spring Security 6.
- **Interface:** Thymeleaf renderizado no servidor, layout único com fragments, Bootstrap 5 e CSS próprio (sem SPA/React/Angular/Vue).
- **Validação:** Jakarta Bean Validation (`spring-boot-starter-validation`).
- **Build:** Maven com Maven Wrapper (`mvnw` / `mvnw.cmd`).
- **Banco de Dados:** MySQL 8.4 em execução via Docker Compose; H2 em memória exclusivamente no escopo `test`.
- **Criptografia de senhas:** `BCryptPasswordEncoder`.
- **Controle de versão:** Git (branch `main`, tag `final` na entrega).

---

## 3. Perfis de Usuário e Matriz de Acesso

| Perfil | Role Spring Security | Capacidades |
|---|---|---|
| Visitante | *(anônimo)* | Visualiza eventos publicados, consulta detalhes/atividades e cria conta pública (`RF01`). |
| Participante | `ROLE_PARTICIPANTE` | Inscreve-se em atividades, cancela a própria inscrição, consulta "Minhas inscrições" e acessa certificados. |
| Organizador | `ROLE_ORGANIZADOR` | Cria, edita, publica e encerra **apenas os próprios eventos** e atividades; lista inscritos e marca presença. |
| Administrador | `ROLE_ADMIN` | Gerencia categorias (CRUD) e lista usuários para conceder/retirar `ROLE_ORGANIZADOR`. |

### Matriz de Acesso Oficial (PDF pág. 2)
- **Ver eventos publicados:** Visitante (Sim), Participante (Sim), Organizador (Sim), Admin (Sim).
- **Inscrever-se e ver "Minhas inscrições":** Visitante (Não), **Participante (Sim)**, Organizador (Não), Admin (Não).
- **Criar e editar eventos e atividades:** Visitante (Não), Participante (Não), **Organizador (Apenas os seus)**, Admin (Não).
- **Listar inscritos e registrar presença:** Visitante (Não), Participante (Não), **Organizador (Apenas os seus)**, Admin (Não).
- **Gerenciar categorias e usuários:** Visitante (Não), Participante (Não), Organizador (Não), **Admin (Sim)**.

> **Nota de coerência:** Para cumprir literalmente a Matriz de Acesso oficial, os perfis funcionais são mantidos de forma exclusiva na aplicação: ao conceder `ROLE_ORGANIZADOR` pelo Admin (`RF04`), a conta deixa de possuir `ROLE_PARTICIPANTE` (ficando restrita às ações de organizador); ao remover `ROLE_ORGANIZADOR`, volta a receber `ROLE_PARTICIPANTE`. Contas `ROLE_ADMIN` permanecem protegidas e não participam dessa alternância.

---

## 4. Modelo de Domínio Obrigatório (6 Entidades + 2 Enums)

Não existe entidade JPA para Certificado (`RF14` é gerado dinamicamente a partir das inscrições com `presente = true`). Todas as datas usam `java.time`.

### Entidades e Relacionamentos
1. **`Usuario` (tabela `usuarios`)**
   - Atributos: `id` (`Long`), `nome` (`String`), `email` (`String`, **UNIQUE**, `nullable = false`), `senha` (`String`, hash BCrypt, `nullable = false`), `ativo` (`boolean`).
   - Relacionamentos: `@ManyToMany` com `Perfil` (tabela associativa `usuarios_perfis`); `@OneToMany` com `Evento` (como organizador); `@OneToMany` com `Inscricao`.
2. **`Perfil` (tabela `perfis`)**
   - Atributos: `id` (`Long`), `nome` (`String`, **UNIQUE**, valores: `ROLE_PARTICIPANTE`, `ROLE_ORGANIZADOR`, `ROLE_ADMIN`).
   - Relacionamentos: `@ManyToMany(mappedBy = "perfis")` com `Usuario`.
3. **`Categoria` (tabela `categorias`)**
   - Atributos: `id` (`Long`), `nome` (`String`, **UNIQUE**, `nullable = false`).
   - Relacionamentos: `@OneToMany(mappedBy = "categoria")` com `Evento`.
4. **`Evento` (tabela `eventos`)**
   - Atributos: `id` (`Long`), `titulo` (`String`), `descricao` (`String` / `TEXT`), `local` (`String`), `dataInicio` (`LocalDate`), `dataFim` (`LocalDate`), `status` (`StatusEvento`, `@Enumerated(EnumType.STRING)`).
   - Relacionamentos: `@ManyToOne(optional = false)` `Categoria` (`categoria_id`); `@ManyToOne(optional = false)` `Usuario` organizador (`organizador_id`); `@OneToMany(mappedBy = "evento")` `Atividade`.
5. **`Atividade` (tabela `atividades`)**
   - Atributos: `id` (`Long`), `titulo` (`String`), `tipo` (`TipoAtividade`, `@Enumerated(EnumType.STRING)`), `ministrante` (`String`), `inicio` (`LocalDateTime`), `fim` (`LocalDateTime`), `cargaHoraria` (`BigDecimal` `DECIMAL(6,2)`, em horas — suporta horas fracionadas como `1,5h`), `vagas` (`Integer`).
   - Relacionamentos: `@ManyToOne(optional = false)` `Evento` (`evento_id`); `@OneToMany(mappedBy = "atividade")` `Inscricao`.
6. **`Inscricao` (tabela `inscricoes`)**
   - Atributos: `id` (`Long`), `dataHora` (`LocalDateTime`), `presente` (`boolean`).
   - Relacionamentos: `@ManyToOne(optional = false)` `Usuario` (`usuario_id`); `@ManyToOne(optional = false)` `Atividade` (`atividade_id`).
   - Restrição de banco obrigatória: **`@UniqueConstraint(columnNames = {"usuario_id", "atividade_id"})`**.

### Enums (`@Enumerated(EnumType.STRING)`)
- **`StatusEvento`:** `RASCUNHO`, `PUBLICADO`, `ENCERRADO`
- **`TipoAtividade`:** `PALESTRA`, `MINICURSO`, `OFICINA`

---

## 5. Requisitos Completos

### Requisitos Funcionais (`RF01`–`RF14`)
- **RF01:** Cadastro público de conta (`nome`, `email`, `senha`); toda conta nova recebe `ROLE_PARTICIPANTE`.
- **RF02:** Login e logout por formulário próprio (`/login`).
- **RF03:** (Admin) CRUD completo de `Categoria`.
- **RF04:** (Admin) Listar usuários e conceder ou retirar o perfil `ROLE_ORGANIZADOR`.
- **RF05:** (Organizador) CRUD de eventos próprios; todo evento nasce em `RASCUNHO`.
- **RF06:** (Organizador) Publicar (`RASCUNHO → PUBLICADO`) e encerrar (`PUBLICADO → ENCERRADO`) evento próprio.
- **RF07:** (Organizador) CRUD de atividades dentro de um evento próprio.
- **RF08:** (Organizador) Listar inscritos de uma atividade própria e marcar presença via checkboxes.
- **RF09:** Página inicial pública listando eventos `PUBLICADO`, paginada e com filtro por categoria.
- **RF10:** Página de detalhes do evento com suas atividades e vagas restantes.
- **RF11:** (Participante) Inscrever-se em uma atividade.
- **RF12:** (Participante) Cancelar a própria inscrição.
- **RF13:** (Participante) Página "Minhas inscrições", mostrando a presença em cada atividade.
- **RF14:** (Participante) Certificado para evento `ENCERRADO`: página imprimível com nome do participante, nome do evento e carga horária somada das atividades com presença.

### Regras de Negócio (`RN01`–`RN07`)
- **RN01:** O e-mail do usuário é único.
- **RN02:** Evento: `dataFim >= dataInicio`. Atividade: `fim > inicio` e inteiramente dentro do período do evento (`inicio.toLocalDate() >= evento.dataInicio` e `fim.toLocalDate() <= evento.dataFim`).
- **RN03:** Só se aceita inscrição em atividade de evento `PUBLICADO`.
- **RN04:** Um participante não pode se inscrever duas vezes na mesma atividade.
- **RN05:** Não há inscrição em atividade com vagas esgotadas (com proteção transacional e lock pessimista).
- **RN06:** O cancelamento da inscrição só é permitido antes do início da atividade (`agora < atividade.inicio`).
- **RN07:** O certificado só existe para evento `ENCERRADO` e se o participante tiver presença (`presente = true`) em pelo menos uma atividade daquele evento.

### Requisitos de Segurança (`RS01`–`RS07`)
- **RS01:** Configuração com `SecurityFilterChain` e página de login própria (`/login`).
- **RS02:** `UserDetailsService` que carrega o usuário do banco pelo `email` (bloqueando usuários com `ativo = false`).
- **RS03:** Senhas armazenadas com `BCryptPasswordEncoder`.
- **RS04:** Regras por URL: rotas públicas (`/`, `/eventos/**`, `/cadastro`, `/login`, estáticos); `/organizador/**` só `ROLE_ORGANIZADOR`; `/admin/**` só `ROLE_ADMIN`; rotas de inscrição/certificado restritas a `ROLE_PARTICIPANTE` conforme Matriz de Acesso; demais rotas exigem login.
- **RS05:** Organizador só altera/consulta os próprios eventos, atividades e presenças; trocar o ID na URL para registro de outro organizador deve resultar em **HTTP 403**.
- **RS06:** Participante só vê e cancela as próprias inscrições; o usuário vem sempre da sessão autenticada, nunca de parâmetro do formulário.
- **RS07:** Menus exibidos conforme perfil com `sec:authorize`, e criação automática de usuário `ADMIN` na subida da aplicação (sem duplicação).

### Requisitos de Interface (`RI01`–`RI03`) e 10 Telas Mínimas (`T01`–`T10`)
- **RI01:** Layout único com fragments Thymeleaf (cabeçalho, menu e rodapé).
- **RI02:** Formulários com `th:object` / `th:field` e mensagens de validação por campo.
- **RI03:** Mensagens de sucesso ou erro após cada ação via flash attributes (`RedirectAttributes`).
- **Telas mínimas:**
  - **T01:** Página inicial com eventos publicados (Público)
  - **T02:** Detalhes do evento e atividades (Público)
  - **T03:** Login e cadastro de conta (Público — `/login` e `/cadastro`)
  - **T04:** Minhas inscrições (Participante)
  - **T05:** Certificado para impressão com `@media print` (Participante)
  - **T06:** Meus eventos (Organizador)
  - **T07:** Formulário de evento (Organizador)
  - **T08:** Atividades do evento e formulário de atividade (Organizador)
  - **T09:** Inscritos e presença (Organizador)
  - **T10:** Categorias e usuários (Admin)

### Requisitos Técnicos (`RT01`–`RT05`)
- **RT01:** Camadas separadas `controller → service → repository`, sem regra de negócio no controller.
- **RT02:** Spring Data JPA com pelo menos uma consulta derivada (ex.: `findByStatus`) e uma consulta paginada (`Page`/`Pageable`).
- **RT03:** Validação com Bean Validation (`@NotBlank`, `@Email`, `@Positive`, `@NotNull`, etc.).
- **RT04:** Dados de exemplo idempotentes na subida: mínimo 2 categorias, 2 eventos com atividades e 1 usuário de cada perfil. *(Decisão aprovada: criar 2 categorias, 4 usuários [`admin`, `organizador`, `organizador2`, `participante`] e 3 eventos com atividades [2 `PUBLICADO` em categorias distintas + 1 `ENCERRADO` com presença para o participante], facilitando testes de filtro, paginação, certificado e HTTP 403).*
- **RT05:** Arquivo `README.md` na raiz do repositório com instruções completas (preferencialmente Docker Compose + alternativa manual).

### Funcionalidades Extras Opcionais (`EX01`–`EX04` — Planejadas para Fase 15)
- **EX01:** Lista de espera quando as vagas acabam, com promoção automática FIFO no cancelamento.
- **EX02:** Impedir inscrição em atividades com horários sobrepostos.
- **EX03:** Certificado em PDF.
- **EX04:** Testes automatizados (JUnit) de pelo menos 3 regras de negócio.
