# Registro de Decisões Arquiteturais — EventIF

Este documento registra as decisões de projeto tomadas em conformidade com `docs/especificacao_oficial.pdf` e aprovadas pelo autor antes da codificação.

---

### ADR-01 — Renderização server-side com Thymeleaf, Bootstrap 5 e CSS próprio
- **Contexto:** A especificação oficial proíbe SPA/React e exige Thymeleaf renderizado no servidor com layout compartilhado (`RI01`–`RI03`).
- **Decisão:** Utilizar Thymeleaf com fragmentos (`header`, `navbar`, `alerts`, `footer`), Bootstrap 5 para responsividade e folha de estilos CSS própria para identidade acadêmica do EventIF (incluindo `@media print` para o certificado).
- **Justificativa:** Atende integralmente a especificação, mantém o projeto simples de empacotar em um único JAR e facilita manutenção e arguição.

### ADR-02 — MySQL 8.4 na execução (Docker) e H2 apenas em testes
- **Contexto:** O PDF permite PostgreSQL ou MySQL e autoriza H2 apenas nos testes; a nota máxima no critério de execução exige Docker.
- **Decisão:** Utilizar **MySQL 8.4** via `docker-compose.yml` para desenvolvimento/entrega e **H2** (`MODE=MySQL`) exclusivamente em `src/test/resources/application-test.properties` no escopo `test`.
- **Justificativa:** Garante paridade entre desenvolvimento e entrega com `docker compose up --build`, sem exigir instalação manual de SGBD na máquina do avaliador.

### ADR-03 — Maven Wrapper (`mvnw`) e JDK 21 local
- **Contexto:** O ambiente local possui JDK 21 instalado em `C:\Users\Almeida\jdk21\jdk-21.0.2`, enquanto o `java` padrão do sistema está na versão 1.8 e não há `mvn` global no `PATH`.
- **Decisão:** Incluir Maven Wrapper (`mvnw` e `mvnw.cmd`) no repositório a partir do Prompt 2 e executar comandos Maven com `JAVA_HOME=C:\Users\Almeida\jdk21\jdk-21.0.2`.
- **Justificativa:** Torna o build reproduzível para qualquer avaliador e no `Dockerfile` sem alterar variáveis globais do sistema operacional.

### ADR-04 — `LocalDate` em `Evento`, `LocalDateTime` em `Atividade` e `Clock` injetável
- **Contexto:** O roteiro solicita escolher um tipo `java.time` coerente para as datas do `Evento` e documentar a decisão; `RN02` exige `dataFim >= dataInicio` no evento e `fim > inicio` na atividade dentro do período do evento.
- **Decisão:** Adotar `LocalDate` para `Evento.dataInicio` e `Evento.dataFim`, `LocalDateTime` para `Atividade.inicio`, `Atividade.fim` e `Inscricao.dataHora`, além de um bean `java.time.Clock` injetável nos services.
- **Justificativa:** `LocalDate` permite eventos de 1 dia (`dataFim == dataInicio`) com `<input type="date">` limpo; uma atividade pertence ao período do evento quando `!inicio.toLocalDate().isBefore(evento.dataInicio) && !fim.toLocalDate().isAfter(evento.dataFim)`. O `Clock` injetável torna os testes de `RN06` determinísticos.

### ADR-05 — Matriz de Acesso do PDF para rotas de Participante e exclusividade de perfis (`RF04`)
- **Contexto:** A Matriz de Acesso da pág. 2 do PDF determina explicitamente que apenas `ROLE_PARTICIPANTE` pode se inscrever, ver "Minhas inscrições" e emitir certificado (Organizador = Não; Admin = Não), enquanto `RS04` define `/eventos/**` como rota pública.
- **Decisão:** Posicionar as rotas de inscrição e certificado em `/inscricoes/**` e `/certificados/**` (fora de `/eventos/**`), exigindo `hasRole("PARTICIPANTE")` no `SecurityFilterChain`. Além disso, ao promover um usuário a `ROLE_ORGANIZADOR` via `RF04`, `UsuarioService` remove `ROLE_PARTICIPANTE` (mantendo o relacionamento `@ManyToMany` no schema, porém aplicando exclusividade operacional entre `PARTICIPANTE`, `ORGANIZADOR` e `ADMIN`); ao revogar `ROLE_ORGANIZADOR`, a conta retorna para `ROLE_PARTICIPANTE`.
- **Justificativa:** Evita que endpoints de inscrição sejam expostos publicamente sob `/eventos/**` e impede que um organizador promovido mantenha simultaneamente acesso de participante, cumprindo literalmente a Matriz de Acesso da especificação.

### ADR-06 — Certificado calculado dinamicamente (sem entidade JPA)
- **Contexto:** A Seção 3 do PDF declara explicitamente que o certificado não é uma entidade JPA.
- **Decisão:** Implementar `CertificadoService` e `CertificadoView` calculando em tempo real a elegibilidade (`RN07`) e a soma da `cargaHoraria` das atividades com `presente = true` no evento `ENCERRADO`.
- **Justificativa:** Respeita o modelo de domínio obrigatório e evita inconsistência entre presença alterada e registro duplicado.

### ADR-07 — Controle de concorrência com lock pessimista + constraint `UNIQUE`
- **Contexto:** Verificações apenas em memória (`count < vagas` e `!exists`) podem falhar sob requisições simultâneas na última vaga ou duplo clique.
- **Decisão:** No `InscricaoService` (`@Transactional`), adquirir lock `PESSIMISTIC_WRITE` sobre a `Atividade` antes de contar inscrições e salvar, mantendo `UNIQUE(usuario_id, atividade_id)` e `UNIQUE(email)` no banco e tratando `DataIntegrityViolationException`.
- **Justificativa:** Elimina race conditions de superlotação (`RN05`) e duplicidade (`RN01`, `RN04`).

### ADR-08 — Proteção contra IDOR no `service`, usuário da sessão e uso estrito de DTOs
- **Contexto:** `RS05`, `RS06` e os critérios de segurança proíbem confiar apenas em links escondidos ou em IDs enviados pelo navegador.
- **Decisão:** Todos os formulários usam Form DTOs dedicados; nunca expor `Usuario` nem `senha` nas views; todo serviço de organizador valida a cadeia `evento.organizador.id == usuarioAutenticadoId` (lançando exceção HTTP 403 em caso de divergência); todo serviço de participante extrai o usuário exclusivamente da sessão autenticada.
- **Justificativa:** Garante bloqueio real no servidor contra mass assignment e troca manual de ID na URL.

### ADR-09 — Ciclo de vida do Evento (`RF06`) e Presença (`RF08`)
- **Contexto:** A especificação define `RASCUNHO`, `PUBLICADO`, `ENCERRADO`, sem detalhar reabertura ou em quais estados a chamada pode ser salva.
- **Decisão:** Transições unidirecionais `RASCUNHO → PUBLICADO → ENCERRADO` (eventos encerrados bloqueiam edição estrutural/exclusão e não voltam atrás). Eventos em `RASCUNHO` não são acessíveis publicamente por URL (retornam 404). Registro de presença por checkboxes é permitido em eventos `PUBLICADO` e `ENCERRADO` (bloqueado em `RASCUNHO`), aceitando lista de IDs opcional (`required = false`) para permitir desmarcar todos os presentes.
- **Justificativa:** Preserva a confiabilidade dos certificados emitidos sem engessar o fechamento da chamada pelo organizador.

### ADR-10 — Integridade referencial amigável em exclusões e edições
- **Contexto:** Excluir uma categoria usada por eventos ou um evento/atividade com inscritos causaria falha de FK (`DataIntegrityViolationException`) ou perda silenciosa de histórico.
- **Decisão:** Bloquear no `service` com `RegraNegocioException` (mensagem clara ao usuário): exclusão de `Categoria` com eventos vinculados; exclusão de `Evento` ou `Atividade` que já possua inscrições; redução de `vagas` abaixo do número atual de inscritos; e alteração do período do `Evento` que deixe alguma atividade existente fora do novo intervalo (`RN02`).
- **Justificativa:** Mantém consistência dos dados e mensagens amigáveis em lugar de erros 500.

### ADR-11 — Carga inicial idempotente (`RT04`, `RS07`)
- **Contexto:** `RS07` exige usuário `ADMIN` na subida; `RT04` exige 2 categorias, 2 eventos com atividades e 1 usuário de cada perfil; o roteiro do vídeo exige demonstrar troca de ID na URL retornando HTTP 403 e emissão de certificado.
- **Decisão:** Implementar `DataInitializer` em Java (idempotente por consulta de existência, usando `BCryptPasswordEncoder` e senhas configuráveis por variáveis de ambiente com defaults fictícios em `.env.example`), criando:
  - 3 perfis (`ROLE_PARTICIPANTE`, `ROLE_ORGANIZADOR`, `ROLE_ADMIN`);
  - 4 usuários (`admin@eventif.edu.br`, `organizador@eventif.edu.br`, `organizador2@eventif.edu.br`, `participante@eventif.edu.br`);
  - 2 categorias;
  - 3 eventos com atividades: 2 `PUBLICADO` (um em cada categoria, facilitando filtro/paginação) e 1 `ENCERRADO` com inscrição e presença marcada para o `participante`, permitindo testar o certificado imediatamente.
- **Justificativa:** Atende integralmente `RT04`/`RS07`, evita dados duplicados em reinicializações e viabiliza demonstração imediata de todos os fluxos obrigatórios.

### ADR-12 — Modalidade individual e fluxo Git
- **Contexto:** O trabalho será desenvolvido individualmente pelo autor com prazo final em 03/12/2026.
- **Decisão:** Repositório Git iniciado na branch `main`, com commits progressivos ao final das etapas e geração da tag `final` na conclusão. O `README.md` conterá campo preenchível/identificado do integrante único e placeholder para o link do vídeo.
- **Justificativa:** Atende às regras de entrega do GitHub na Seção 10 do PDF.

### ADR-13 — Implementação dos 4 extras (`EX01`–`EX04`) na Fase 15 pós-auditoria
- **Contexto:** O autor aprovou implementar todos os extras da especificação (`EX01` lista de espera com promoção automática, `EX02` bloqueio de horários sobrepostos, `EX03` certificado em PDF e `EX04` testes JUnit de regras de negócio), respeitando a regra do PDF de que extras só pontuam se todo o obrigatório estiver funcionando.
- **Decisão:** Após a conclusão e aprovação dos requisitos obrigatórios na Fase 14, foram implementados na Fase 15:
  - `EX01`: entidade `ListaEspera` (`uk_lista_espera_usuario_atividade`) e `ListaEsperaRepository`, rota dedicada `POST /inscricoes/atividades/{id}/espera` quando `vagasRestantes == 0`, saída protegida por sessão (`RS06`) em `POST /inscricoes/espera/{id}/cancelar` e promoção automática FIFO no cancelamento sob lock pessimista;
  - `EX02`: validação de interseção de intervalos (`a.inicio < :fim AND :inicio < a.fim`) em `InscricaoRepository#findConflitosDeHorario`, aplicada em inscrição, entrada na lista de espera e promoção FIFO;
  - `EX03`: geração de PDF A4 paisagem com `com.github.librepdf:openpdf:1.3.43` em `CertificadoPdfService` reutilizando `CertificadoService` (`RN07`) em `GET /certificados/eventos/{eventoId}/pdf`;
  - `EX04`: suíte expandida para `48` testes JUnit incluindo `FuncionalidadesExtrasTest`.
- **Justificativa:** Preserva 100% da compatibilidade com os requisitos obrigatórios auditados e adiciona todas as funcionalidades extras de forma testada.

### ADR-14 — Coluna física `local_evento` para o atributo `Evento.local` (compatibilidade MySQL)
- **Contexto:** Na auditoria final (Prompt 14) identificou-se que a coluna `eventos.local` usa `LOCAL`, palavra reservada do MySQL 8.x. O H2 (perfil `test`) aceita o identificador, então os testes passavam, mas o DDL gerado pelo Hibernate e as consultas falhariam no MySQL em execução (Docker), impedindo a subida da aplicação.
- **Decisão:** Mapear o atributo Java `local` para a coluna física `local_evento` (`@Column(name = "local_evento")`), mantendo intactos JPQL, DTOs, templates e formulários. Adicionado `CompatibilidadeMysqlSchemaTest`, que falha se qualquer tabela/coluna gerada usar uma palavra reservada do MySQL 8.x (lista curada).
- **Justificativa:** Correção mínima e independente de SGBD, sem `auto_quote_keyword` global; como nenhum banco MySQL de produção havia sido criado, não há migração de dados.

### ADR-15 — Carga horária decimal (`BigDecimal`) e mensagens de conversão em português
- **Contexto:** Atividades com duração fracionada (ex.: `10:30` às `12:00` = `1,5h`) eram rejeitadas por `Atividade.cargaHoraria` ser `Integer`, gerando mensagem técnica em inglês (`Failed to convert property value...`) no `BindingResult`.
- **Decisão:** Alterar `Atividade.cargaHoraria` para `BigDecimal` (`DECIMAL(6,2)`), configurar `Converter<String, BigDecimal>` em `WebConfig` aceitando ponto (`.`) e vírgula (`,`), formatar inteiros sem zeros à direita (`4h`) e frações com vírgula (`1,5h`) via `CargaHorariaFormatter`, ajustar `<input type="number" step="0.25" min="0.25">` e adicionar `src/main/resources/messages.properties` com mensagens de `typeMismatch` em português.
- **Justificativa:** Alinha o formulário de atividades à realidade acadêmica e garante validação clara em português (`RI02`).
