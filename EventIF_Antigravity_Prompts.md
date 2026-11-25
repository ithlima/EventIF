# EventIF — roteiro de prompts para Google Antigravity

Este roteiro foi montado a partir da especificação do trabalho de Tópicos em Java para a Web. Use os prompts na ordem, um por vez, no mesmo workspace do repositório. Não peça para o agente construir o sistema inteiro em uma única execução.

## Como usar

1. Crie/abra uma pasta vazia para o repositório do EventIF no Antigravity.
2. Anexe ou copie para dentro do repositório o PDF da especificação oficial, se possível, por exemplo em `docs/especificacao-oficial.pdf`.
3. Selecione o modelo indicado abaixo.
4. Envie o Prompt 0. Só avance depois de revisar o plano e os documentos de contexto gerados.
5. Em cada etapa, confira os arquivos alterados, o relatório e o resultado do build/testes.
6. Quando abrir uma conversa nova, comece pelo Prompt de retomada no fim deste arquivo.

**Regra de ouro:** cada etapa deve terminar com compilação/testes aplicáveis, atualização do checklist e resumo do que foi feito. Não deixe o agente avançar silenciosamente com falhas.

---

# Modelos Gemini recomendados

## Configuração principal

**Use `Orchestrator` com `Gemini 4 Neon` como planejador e `Gemini 4 Hydrogen` como executor**, ambos em `High` quando a opção estiver disponível.

- **Neon (planejador):** arquitetura, decomposição das tarefas, análise de requisitos, revisão e decisões difíceis.
- **Hydrogen (executor):** edição dos arquivos, implementação, execução de Maven, testes e correções rotineiras.
- **Neon em `Extra`:** reserve para auditoria final, falhas complexas de segurança ou decisões arquiteturais realmente difíceis, se sua conta apresentar esse nível.

O Orchestrator é adequado para este projeto porque combina planejamento/revisão com execução de código na mesma tarefa. Não é necessário alternar manualmente entre modelos a cada prompt.

## Se o Orchestrator não aparecer

Use `Gemini 4 Neon` em `High` para todas as etapas importantes. Para tarefas pequenas e repetitivas, como ajustes de HTML/CSS ou mensagens de validação, pode usar `Gemini 4 Hydrogen` em `High` ou `Gemini 3.8 Flash` em `Medium`, caso ainda esteja disponível no seletor.

A disponibilidade exata depende do tipo de assinatura e do que aparece no seletor da sua conta. A documentação oficial consultada lista Neon, Carbon, Hydrogen e Orchestrator para planos Google AI Pro elegíveis. Confirme os modelos realmente liberados na sua interface.

---

# Decisões-base do projeto

Estas decisões deixam o desenvolvimento consistente. Se quiser trocar banco ou alguma escolha, faça isso antes do Prompt 1 e atualize todos os documentos de contexto.

- **Nome:** EventIF — Sistema de Gestão de Eventos Acadêmicos.
- **Java:** 21.
- **Backend:** Spring Boot 3.x, Spring MVC, Spring Data JPA/Hibernate e Spring Security 6.
- **Interface:** Thymeleaf renderizado no servidor. Não usar React, Angular, Vue, SPA ou arquitetura frontend separada.
- **Build:** Maven.
- **Banco de desenvolvimento/execução:** MySQL, executado pelo Docker Compose. H2 somente para testes automatizados.
- **Arquitetura obrigatória:** `controller → service → repository`; controllers não contêm regras de negócio.
- **Validação:** Jakarta Bean Validation.
- **Senhas:** BCrypt.
- **UI:** layout único com fragments Thymeleaf; Bootstrap pode ser usado, com CSS próprio para identidade visual.
- **Certificado:** não criar entidade JPA para certificado. Gerar a página a partir de inscrições com presença.
- **Entrega:** repositório Git, Dockerfile, docker-compose.yml e README completo.

## Modelo de domínio obrigatório

1. `Usuario`: `id`, `nome`, `email` único, `senha` com BCrypt e `ativo`; relacionamento `ManyToMany` com `Perfil`.
2. `Perfil`: `id`, `nome`; valores de nome: `ROLE_PARTICIPANTE`, `ROLE_ORGANIZADOR`, `ROLE_ADMIN`; `ManyToMany` com `Usuario`.
3. `Categoria`: `id`, `nome`; `OneToMany` com `Evento`.
4. `Evento`: `id`, `titulo`, `descricao`, `local`, `dataInicio`, `dataFim`, `status`; `ManyToOne` com `Categoria` e com `Usuario` organizador; `OneToMany` com `Atividade`.
5. `Atividade`: `id`, `titulo`, `tipo`, `ministrante`, `inicio`, `fim` (`LocalDateTime`), `cargaHoraria` em horas e `vagas`; `ManyToOne` com `Evento`; `OneToMany` com `Inscricao`.
6. `Inscricao`: `id`, `dataHora`, `presente` (`boolean`); `ManyToOne` com `Usuario` e `Atividade`; restrição única no par `(usuario_id, atividade_id)`.

Enums com `@Enumerated(EnumType.STRING)`:
- `StatusEvento`: `RASCUNHO`, `PUBLICADO`, `ENCERRADO`.
- `TipoAtividade`: `PALESTRA`, `MINICURSO`, `OFICINA`.

Use tipos `java.time` para datas. Para os campos de data do evento, escolha um tipo coerente com o horário das atividades e documente a decisão.

## Requisitos que não podem ser esquecidos

### Perfis
- Visitante: visualiza eventos publicados e cria conta.
- Participante (`ROLE_PARTICIPANTE`): inscreve-se, cancela a própria inscrição, vê inscrições e certificados.
- Organizador (`ROLE_ORGANIZADOR`): gerencia somente os próprios eventos/atividades e registra presença.
- Administrador (`ROLE_ADMIN`): gerencia categorias e concede/remove o perfil de organizador.

### Requisitos funcionais RF01–RF14
- RF01 cadastro público; toda conta nova recebe `ROLE_PARTICIPANTE`.
- RF02 login e logout por formulário próprio.
- RF03 CRUD de categoria pelo admin.
- RF04 admin lista usuários e concede/remove perfil de organizador.
- RF05 organizador faz CRUD dos próprios eventos; evento nasce em `RASCUNHO`.
- RF06 organizador publica e encerra evento.
- RF07 CRUD de atividades dentro de evento próprio.
- RF08 organizador lista inscritos e marca presença com checkboxes.
- RF09 página inicial pública com eventos `PUBLICADO`, paginação e filtro por categoria.
- RF10 detalhes do evento, atividades e vagas restantes.
- RF11 participante inscreve-se em atividade.
- RF12 participante cancela a própria inscrição.
- RF13 página “Minhas inscrições”, mostrando presença.
- RF14 certificado imprimível para evento `ENCERRADO`, com nome do participante, evento e soma da carga horária das atividades em que teve presença.

### Regras de negócio RN01–RN07
- RN01 email único.
- RN02 `dataFim >= dataInicio` no evento; `fim > inicio` na atividade e atividade dentro do período do evento.
- RN03 inscrição somente em atividade de evento `PUBLICADO`.
- RN04 não permitir duas inscrições do mesmo usuário na mesma atividade.
- RN05 não permitir inscrição sem vagas.
- RN06 cancelamento somente antes do início da atividade.
- RN07 certificado somente para evento `ENCERRADO` e participante com presença em pelo menos uma atividade.

Todas as regras devem ficar na camada `service`, apresentar mensagem clara ao usuário e ter proteção também no banco quando aplicável (por exemplo, email e inscrição duplicada).

### Segurança RS01–RS07
- `SecurityFilterChain` e página de login própria.
- `UserDetailsService` busca usuário por email.
- Senhas armazenadas com `BCryptPasswordEncoder`.
- Rotas públicas: `/`, `/eventos/**`, `/cadastro`, `/login`.
- `/organizador/**`: somente `ROLE_ORGANIZADOR`.
- `/admin/**`: somente `ROLE_ADMIN`.
- Demais rotas exigem autenticação.
- Organizador só altera registros que pertencem a ele. Trocar ID na URL para evento de outro organizador deve resultar em HTTP 403.
- Participante só consulta/cancela suas próprias inscrições. O usuário deve vir do principal/sessão autenticada, nunca de um ID enviado pelo formulário.
- Menus condicionados com `sec:authorize`, mas esconder links não substitui autorização no servidor.
- Criar um usuário ADMIN inicial na subida da aplicação.

### Interface RI01–RI03 e telas mínimas
- Layout compartilhado com fragments de cabeçalho, menu e rodapé.
- Formulários com `th:object`, `th:field` e mensagens de validação por campo.
- Mensagens de sucesso/erro após ações com flash attributes.
- Pelo menos 10 telas: (1) início público, (2) detalhes do evento, (3) login/cadastro, (4) minhas inscrições, (5) certificado imprimível, (6) meus eventos, (7) formulário de evento, (8) atividades/formulário, (9) inscritos/presença, (10) categorias/usuários.

### Requisitos técnicos RT01–RT05
- Separação controller/service/repository.
- Pelo menos uma consulta derivada (ex.: `findByStatus`) e uma consulta paginada.
- Bean Validation: usar, conforme aplicável, `@NotBlank`, `@Email`, `@Positive` etc.
- Dados de exemplo na inicialização: 2 categorias, 2 eventos com atividades e um usuário de cada perfil.
- README com instruções completas para executar.

### Extras opcionais
Só iniciar depois dos requisitos obrigatórios estarem funcionando. O teto de extras é 1,0 ponto. Opções: lista de espera com promoção automática após cancelamento; impedir inscrição em atividades com horários sobrepostos; certificado em PDF; testes JUnit de pelo menos três regras. Não permitir que extras atrasem ou quebrem RF01–RF14.

---

# Sequência de prompts

## Prompt 0 — leitura da especificação e planejamento, sem programar

```text
Você será o engenheiro de software responsável por me orientar e implementar o EventIF, um trabalho acadêmico de Java Web.

Antes de qualquer código:
1. Leia integralmente a especificação oficial em PDF disponível no workspace/anexo. Ela é a fonte de verdade dos requisitos.
2. Leia também este roteiro de projeto, se ele estiver no workspace.
3. Extraia todos os requisitos, entidades, relacionamentos, regras de negócio, regras de segurança, telas, requisitos técnicos e critérios de avaliação.
4. Compare o conteúdo do PDF com as decisões-base deste roteiro. Se encontrar conflito, o PDF prevalece e você deve apontar o conflito antes de alterar a arquitetura.
5. Não implemente funcionalidades ainda. Primeiro analise e apresente um plano por etapas.

Decisões-base: Java 21, Spring Boot 3.x, Spring MVC + Thymeleaf (server-side, sem SPA), Spring Data JPA/Hibernate, Spring Security 6, Maven e MySQL. H2 apenas em testes. Arquitetura controller → service → repository. O sistema chama-se EventIF.

Quero que você apresente:
- resumo do escopo;
- lista RF01–RF14, RN01–RN07, RS01–RS07, RI01–RI03 e RT01–RT05;
- modelo das seis entidades e seus relacionamentos;
- proposta de pacotes e fluxo de requisição;
- ordem de implementação com dependências entre etapas;
- riscos técnicos, principalmente autorização por propriedade, inscrições duplicadas e limite de vagas;
- estratégia de testes e de execução com Docker.

Não invente requisitos e não elimine itens do PDF. Se algum detalhe não estiver especificado, marque como decisão de projeto e proponha uma opção simples, justificando-a. Não comece a codificar nesta etapa.
```

## Prompt 1 — criar a memória persistente do projeto

```text
Com base na especificação oficial e no plano aprovado, crie a documentação de contexto que será a memória persistente deste repositório.

Crie:
1. `AGENTS.md` na raiz, com regras curtas e permanentes para qualquer agente que trabalhar neste projeto.
2. `docs/PROJECT_CONTEXT.md`, com visão geral, stack, decisões, entidades, relacionamentos, regras de negócio, segurança, telas e requisitos completos.
3. `docs/REQUIREMENTS_TRACEABILITY.md`, com uma tabela que relacione cada requisito RF/RN/RS/RI/RT ao pacote, classe, tela e teste responsável. Inicialmente marque tudo como PENDENTE.
4. `docs/ARCHITECTURE.md`, com camadas, pacotes, responsabilidades, fluxos principais e decisões de arquitetura.
5. `docs/DECISIONS.md`, registrando escolhas e justificativas, inclusive banco MySQL e uso de Thymeleaf server-side.
6. `docs/IMPLEMENTATION_PLAN.md`, com fases, dependências e critérios de conclusão.

No `AGENTS.md`, estabeleça estas regras: sempre ler `docs/PROJECT_CONTEXT.md` e `docs/REQUIREMENTS_TRACEABILITY.md` antes de alterações relevantes; respeitar a especificação; não implementar regra de negócio no controller; não trocar a stack; não usar SPA; não confiar apenas em botões escondidos para segurança; não obter usuário/participante por ID enviado pelo formulário; manter testes e documentação atualizados; executar Maven após mudanças; não apagar trabalho existente sem explicar e pedir autorização.

Não crie ainda entidades ou funcionalidades. Revise se os documentos são coerentes entre si e me mostre a árvore de arquivos criada.
```

## Prompt 2 — scaffold e configuração do Spring Boot

```text
Leia `AGENTS.md`, `docs/PROJECT_CONTEXT.md`, `docs/ARCHITECTURE.md` e o plano. Implemente somente a estrutura inicial do projeto.

Crie um projeto Maven com Java 21 e Spring Boot 3.x, compatível com a especificação. Inclua dependências necessárias para Spring MVC, Thymeleaf, Spring Data JPA, Spring Security 6, Bean Validation, driver MySQL e testes. H2 deve existir somente no escopo de testes. Não use Spring Boot 4, React, Angular, Vue ou SPA.

Configure:
- classe principal;
- estrutura de pacotes conforme a arquitetura aprovada;
- `application.properties` com configuração via variáveis de ambiente para banco, usuário e senha;
- perfil de teste separado usando H2;
- `.gitignore` para `target/`, arquivos de IDE, logs e arquivos locais sensíveis;
- página inicial provisória simples para validar Thymeleaf.

Sugestão de pacotes: `config`, `controller`, `dto`, `entity`, `enums`, `exception`, `repository`, `security`, `service`, `validation`. Ajuste apenas se houver justificativa documentada.

Não implemente ainda entidades, regras de negócio ou telas completas. Execute `mvn test` e `mvn package`, corrija erros e informe versões efetivamente usadas e arquivos criados. Atualize o checklist somente para itens realmente concluídos.
```

## Prompt 3 — entidades, enums e mapeamentos JPA

```text
Leia os documentos de contexto e implemente somente o modelo de domínio JPA do EventIF, respeitando integralmente a especificação.

Crie as seis entidades: Usuario, Perfil, Categoria, Evento, Atividade e Inscricao. Preserve os atributos mínimos e relacionamentos obrigatórios descritos em `docs/PROJECT_CONTEXT.md`.

Regras de mapeamento:
- Usuario ↔ Perfil: ManyToMany;
- Categoria → Evento: OneToMany / ManyToOne;
- Usuario organizador → Evento: OneToMany / ManyToOne;
- Evento → Atividade: OneToMany / ManyToOne;
- Atividade → Inscricao: OneToMany / ManyToOne;
- Usuario → Inscricao: OneToMany / ManyToOne.
- email de Usuario deve possuir restrição UNIQUE;
- Inscricao deve possuir UNIQUE composto por usuario_id + atividade_id;
- usar `@Enumerated(EnumType.STRING)` para StatusEvento e TipoAtividade.
- StatusEvento: RASCUNHO, PUBLICADO, ENCERRADO.
- TipoAtividade: PALESTRA, MINICURSO, OFICINA.
- usar tipos `java.time`.

Defina cuidadosamente os lados proprietários dos relacionamentos, `mappedBy`, nullable, nomes de tabelas/colunas e estratégia de carregamento. Evite `CascadeType.ALL` indiscriminado e evite serialização JSON de entidades, pois a aplicação usa Thymeleaf.

Crie construtores, getters/setters ou recursos equivalentes necessários, sem lógica de negócio complexa dentro das entidades. Documente a decisão sobre os tipos de data de Evento. Inclua testes de contexto ou mapeamento quando fizer sentido.

Não crie controllers nem telas nesta etapa. Execute os testes e o build. Ao final, mostre os relacionamentos e confirme que a restrição única de inscrição está no mapeamento do banco.
```

## Prompt 4 — repositories e consultas JPA

```text
Leia o contexto e implemente somente a camada repository do EventIF.

Crie repositories Spring Data JPA para as entidades necessárias. A camada deve oferecer consultas para:
- localizar Usuario por email;
- localizar Perfil pelo nome;
- verificar se email já existe;
- listar eventos de um organizador;
- buscar eventos publicados com paginação e filtro opcional por categoria;
- localizar atividades de um evento;
- contar inscrições/vagas ocupadas de uma atividade;
- listar inscrições de um participante;
- listar inscritos de uma atividade;
- verificar inscrição existente por usuário e atividade.

Atenda explicitamente ao RT02: implemente pelo menos uma consulta derivada legível, por exemplo `findByStatus`, e uma consulta paginada real para a página inicial. Use `Pageable`/`Page` quando apropriado.

Não coloque validações de negócio nos repositories. Se for necessário lock para evitar duas inscrições consumirem a última vaga simultaneamente, proponha e documente uma estratégia transacional adequada (por exemplo, lock pessimista na atividade) para ser usada pela camada service.

Não crie controllers ou templates. Execute testes e build; informe quais métodos atendem cada consulta exigida.
```

## Prompt 5 — autenticação e autorização Spring Security

```text
Implemente a segurança do EventIF. Leia os documentos de contexto antes de alterar arquivos.

Requisitos obrigatórios:
1. `SecurityFilterChain` com login por formulário próprio em `/login` e logout.
2. `UserDetailsService` carrega o usuário pelo email.
3. `BCryptPasswordEncoder` para codificar senhas.
4. Rotas públicas: `/`, `/eventos/**`, `/cadastro`, `/login` e recursos estáticos necessários.
5. `/organizador/**` somente para `ROLE_ORGANIZADOR`.
6. `/admin/**` somente para `ROLE_ADMIN`.
7. Todas as demais rotas exigem autenticação.
8. Menus condicionais com `sec:authorize`.
9. Criar usuário ADMIN inicial na inicialização, sem duplicá-lo em reinicializações.

Use de forma consistente `hasRole`/`hasAuthority` e as authorities `ROLE_*`. Garanta que usuário inativo não autentique. Não armazene senha em texto puro nem credenciais reais no repositório; use variáveis de ambiente para a senha inicial do admin e forneça apenas valores de demonstração em `.env.example`.

Importante: controle de acesso não pode existir apenas na interface. A autorização deve ser verificada no servidor e, para operações por propriedade, também na camada service. Ainda não implemente o CRUD funcional das telas.

Crie testes de segurança para acesso anônimo, perfil errado e perfil correto nas rotas principais. Execute Maven e apresente a matriz URL × perfil testada.
```

## Prompt 6 — serviços e regras de negócio RN01–RN07

```text
Implemente a camada service do EventIF e centralize nela todas as regras de negócio. Leia primeiro o contexto e o rastreador de requisitos.

Implemente e teste:
- RN01: email único;
- RN02: data final do evento não pode ser anterior à inicial; fim da atividade deve ser posterior ao início e a atividade deve ficar dentro do período do evento;
- RN03: só aceitar inscrição em atividade cujo evento esteja PUBLICADO;
- RN04: impedir inscrição duplicada do mesmo participante na mesma atividade;
- RN05: impedir inscrição quando as vagas estiverem esgotadas;
- RN06: permitir cancelamento somente antes do início da atividade;
- RN07: certificado somente quando o evento estiver ENCERRADO e o participante tiver presença em pelo menos uma atividade.

Implemente também a regra de propriedade:
- organizador só pode consultar/alterar eventos e atividades pertencentes a ele;
- a troca manual do ID de outro organizador deve gerar 403;
- participante só pode consultar/cancelar inscrições próprias;
- usuário participante deve ser obtido do principal autenticado, nunca de parâmetro de formulário.

Crie exceções específicas ou um tratamento centralizado que retorne mensagens amigáveis e status HTTP adequados. Não deixe regra de negócio em controller ou template.

Para inscrições concorrentes, não confie apenas em contar vagas sem proteção transacional. Use uma estratégia segura, combinando transação, lock quando necessário e restrição UNIQUE no banco. Preserve a restrição única mesmo com validação no service.

Não implemente telas completas ainda. Escreva testes unitários/integrados para pelo menos RN01, RN04, RN05, RN06 e RN07, ou explique tecnicamente qualquer limitação. Execute `mvn test` e atualize o rastreador.
```

## Prompt 7 — cadastro público e páginas do participante

```text
Implemente os fluxos públicos e do participante, utilizando os services já existentes. Leia o contexto e respeite a segurança.

Implementar:
- RF01 cadastro público com nome, email e senha; validar campos; codificar senha com BCrypt; atribuir automaticamente ROLE_PARTICIPANTE; nunca permitir que o formulário escolha ADMIN ou ORGANIZADOR.
- RF02 formulário de login próprio e logout integrado ao Spring Security.
- RF09 página inicial pública listando apenas eventos PUBLICADO, com paginação e filtro por categoria.
- RF10 página de detalhes do evento com atividades, vagas totais/ocupadas/restantes e indicação apropriada de atividade sem vagas.
- RF11 inscrição do participante autenticado.
- RF12 cancelamento da própria inscrição, respeitando RN06.
- RF13 página “Minhas inscrições”, exibindo evento, atividade, data e situação de presença.
- RF14 página de certificado para evento encerrado, validando RN07 e mostrando nome do participante, nome do evento e soma da carga horária das atividades em que teve presença.

O participante deve ser identificado pelo principal autenticado. Não aceite `usuarioId` do navegador para criar, consultar ou cancelar inscrição. Mensagens de sucesso/erro devem usar flash attributes. Faça redirecionamento após POST para evitar reenvio de formulário.

Crie os DTOs/form objects necessários para não vincular formulários diretamente a entidades sensíveis. Ainda não implemente os módulos de organizador e administrador. Inclua testes de controller/service relevantes, execute Maven e atualize o rastreador.
```

## Prompt 8 — módulo do organizador

```text
Implemente o módulo de organizador com rotas sob `/organizador/**`. Leia o contexto, a matriz de acesso e os services existentes.

Implementar:
- RF05: listar “Meus eventos” e CRUD dos próprios eventos; todo evento novo nasce com status RASCUNHO e organizador definido pelo usuário autenticado.
- RF06: ações para publicar e encerrar evento, respeitando o estado e as regras do sistema.
- RF07: CRUD de atividades dentro de um evento do próprio organizador.
- RF08: listar inscritos por atividade e marcar presença usando checkboxes.

Segurança é prioridade:
- a rota deve exigir ROLE_ORGANIZADOR;
- em toda consulta/alteração de evento ou atividade, confirmar no service que o evento pai pertence ao organizador autenticado;
- não confiar em ID de organizador enviado no formulário;
- trocar o ID na URL para acessar evento de outra pessoa deve resultar em HTTP 403, nunca em edição ou exposição dos dados;
- ao manipular atividade, verificar também a propriedade do evento pai.

Valide datas do evento e da atividade com RN02. Evite que um organizador marque presença em inscrições de atividade pertencente a outro organizador. Use mensagens flash e validação de formulário.

Crie testes, em especial um teste de IDOR/troca de ID que confirme 403. Execute Maven e atualize o rastreador.
```

## Prompt 9 — módulo do administrador

```text
Implemente o módulo administrativo sob `/admin/**`, disponível somente para ROLE_ADMIN.

Implementar:
- RF03 CRUD completo de Categoria;
- RF04 listagem de usuários e ações para conceder/remover apenas o perfil ROLE_ORGANIZADOR.

Cuidados:
- não permitir que usuário comum eleve o próprio perfil;
- ao remover ROLE_ORGANIZADOR, não remover indevidamente ROLE_PARTICIPANTE;
- não expor hashes de senha em listagens ou DTOs;
- preservar a conta ADMIN inicial e não criar duplicações;
- validar nome de categoria e evitar duplicidade, caso essa seja a regra adotada e documentada;
- proteger todas as operações no servidor, não apenas por ocultação de botões.

Crie templates administrativos, mensagens flash, validação de campos e testes de autorização. Execute Maven e atualize o rastreador.
```

## Prompt 10 — interface Thymeleaf e acabamento visual

```text
Faça uma revisão e complete a interface Thymeleaf do EventIF, sem mudar a arquitetura server-side.

Requisitos:
- RI01: fragments reutilizáveis para cabeçalho, menu, conteúdo e rodapé;
- RI02: formulários usando `th:object` e `th:field`, com erros de validação junto ao campo;
- RI03: mensagens de sucesso e erro via flash attributes;
- menus condicionais com `sec:authorize` para visitante, participante, organizador e admin;
- responsividade para desktop e celular, hierarquia visual clara, contraste adequado, estados vazios e mensagens de erro/sucesso compreensíveis.

Confirme as dez telas mínimas: início pública; detalhes do evento; login/cadastro; minhas inscrições; certificado imprimível; meus eventos; formulário de evento; atividades/formulário; inscritos/presença; categorias/usuários.

Crie uma identidade visual acadêmica própria para o EventIF, consistente e discreta. Pode usar Bootstrap, complementado por CSS próprio. Não invente funcionalidades e não transforme o projeto em SPA.

No certificado, inclua estilos `@media print` para ocultar navegação e imprimir somente o certificado. Garanta que os templates não exponham dados sensíveis e que links/ações correspondam às permissões reais.

Revise todos os links, formulários e mensagens; execute o build e os testes. Se houver navegador de preview, faça uma inspeção visual das telas principais e corrija problemas reais encontrados.
```

## Prompt 11 — carga de dados de exemplo e usuários de teste

```text
Implemente a carga de dados de exemplo exigida pelo RT04, de forma idempotente e segura.

Na inicialização devem existir:
- 2 categorias;
- 2 eventos com atividades;
- um usuário PARTICIPANTE;
- um usuário ORGANIZADOR;
- um usuário ADMIN.

O ADMIN inicial é obrigatório por RS07. Garanta que as senhas de demonstração sejam codificadas com BCrypt e que credenciais configuráveis venham de variáveis de ambiente. Inclua valores fictícios e claramente identificados em `.env.example`/README, nunca segredos reais.

A inicialização não pode duplicar categorias, perfis, usuários ou eventos a cada reinício. Crie dados coerentes para demonstrar eventos publicados, inscrições, presença e certificado, sem violar as regras de datas. Se for preciso ter um evento encerrado de demonstração, crie atividades e inscrições com presença coerentes para testar o certificado.

Não use `data.sql` com senhas em texto puro. Prefira um inicializador Java organizado e idempotente. Execute testes e valide que a aplicação sobe duas vezes sem duplicação.
```

## Prompt 12 — testes automatizados e regressão

```text
Faça uma etapa dedicada de testes e qualidade. Leia o rastreador RF/RN/RS/RI/RT e identifique lacunas.

Crie/complete testes JUnit para cobrir no mínimo:
1. cadastro rejeita email duplicado e atribui ROLE_PARTICIPANTE;
2. inscrição duplicada é bloqueada;
3. inscrição com vagas esgotadas é bloqueada;
4. cancelamento após o início é bloqueado;
5. certificado é negado para evento não encerrado ou sem presença;
6. participante não acessa/cancela inscrição de outro usuário;
7. organizador não altera evento de outro organizador e recebe 403;
8. rotas administrativas bloqueiam perfis não ADMIN.

O H2 pode ser usado nos testes, mas a aplicação em execução deve continuar configurada para MySQL. Não enfraqueça regras para fazer teste passar. Evite testes que dependam de horário real instável; use relógio injetável ou datas controladas quando necessário.

Execute `mvn clean test` e `mvn package`. Corrija falhas. Apresente relatório com requisito → teste → resultado e indique claramente qualquer requisito ainda não coberto. Atualize o rastreador.
```

## Prompt 13 — Docker e README de entrega

```text
Prepare o EventIF para execução e entrega conforme a especificação.

Crie:
- `Dockerfile` para a aplicação Java;
- `docker-compose.yml` com aplicação e MySQL;
- volume persistente do banco;
- healthcheck do MySQL e dependência de inicialização apropriada;
- configuração por variáveis de ambiente;
- `.env.example` com valores locais fictícios, sem segredos reais;
- `.dockerignore`.

O comando preferencial deve ser simples: `docker compose up --build`. Documente como configurar variáveis, iniciar, acessar e parar o sistema. A aplicação deve aguardar o banco estar pronto e usar a configuração MySQL no ambiente Docker.

Escreva um README.md completo contendo:
- nome e descrição do projeto;
- integrantes (deixe placeholders claros para preencher, sem inventar nomes);
- link do vídeo como placeholder;
- pré-requisitos e versões;
- instruções com Docker e, como alternativa, instruções manuais;
- URL de acesso;
- variáveis de ambiente;
- usuários de teste e senhas fictícias configuradas;
- visão geral dos pacotes;
- funcionalidades extras implementadas;
- limitações conhecidas.

Nunca inclua senhas reais ou tokens no Git. Teste o Docker Compose de verdade se o ambiente permitir. Se não puder executar Docker, valide sintaxe, build e explique exatamente o que ficou sem teste. Atualize o rastreador.
```

## Prompt 14 — auditoria final contra a especificação

```text
Atue agora como revisor técnico rigoroso e avaliador do trabalho. Não adicione funcionalidades novas antes de concluir esta auditoria.

Leia o PDF oficial, `AGENTS.md`, todos os documentos de contexto e o código. Faça uma auditoria requisito por requisito:
- RF01 a RF14;
- RN01 a RN07;
- RS01 a RS07;
- RI01 a RI03;
- RT01 a RT05;
- dez telas mínimas;
- critérios de entrega do GitHub, vídeo, README e Docker.

Para cada item, classifique como PASSOU, FALHOU ou PARCIAL e aponte evidência concreta (classe, método, rota, template ou teste). Não marque como concluído apenas porque existe um botão ou arquivo: valide o fluxo funcional e a proteção no servidor.

Teste especialmente:
1. cadastro público sempre cria PARTICIPANTE;
2. participante não acessa rotas de organizador/admin;
3. organizador não altera evento alheio, inclusive trocando ID na URL (deve retornar 403);
4. participante não vê nem cancela inscrições de terceiros;
5. inscrição bloqueia evento não publicado, duplicidade e lotação;
6. cancelamento após o início é negado;
7. certificado só aparece para evento encerrado com presença;
8. a carga horária soma somente atividades com presença;
9. página inicial tem paginação e filtro por categoria;
10. Docker sobe aplicação e MySQL;
11. README permite executar o projeto seguindo somente as instruções.

Execute `mvn clean test` e `mvn package`. Corrija falhas sem remover requisitos. Atualize `docs/REQUIREMENTS_TRACEABILITY.md` com evidências e gere `docs/FINAL_AUDIT.md` com pendências honestas. Não afirme que algo foi testado se não foi executado.
```

## Prompt 15 — extras opcionais (somente depois dos obrigatórios)

```text
Os requisitos obrigatórios do EventIF já foram auditados. Antes de implementar extras, confirme no `docs/FINAL_AUDIT.md` que RF01–RF14, RN01–RN07, RS01–RS07, RI01–RI03 e RT01–RT05 estão PASSOU. Se houver falha, não implemente extras: corrija primeiro.

Avalie custo/benefício dos extras da especificação e recomende a combinação mais segura para até 1,0 ponto adicional. Dê preferência a:
- EX02: impedir inscrição em atividades com horários sobrepostos;
- EX04: testes automatizados de pelo menos três regras de negócio.

Só implemente depois de explicar o impacto no modelo, services, mensagens, testes e README. Preserve compatibilidade com requisitos obrigatórios e não altere o comportamento já validado. Ao final, execute toda a suíte de regressão e atualize a auditoria.
```

---

# Prompt de retomada para uma conversa nova

Use este prompt sempre que o histórico ficar longo, você abrir uma nova conversa ou trocar de modelo:

```text
Vamos continuar o projeto EventIF neste mesmo repositório.

Antes de responder ou editar qualquer arquivo:
1. Leia `AGENTS.md`.
2. Leia `docs/PROJECT_CONTEXT.md`, `docs/ARCHITECTURE.md`, `docs/DECISIONS.md` e `docs/REQUIREMENTS_TRACEABILITY.md`.
3. Leia `docs/IMPLEMENTATION_PLAN.md` e, se existir, `docs/FINAL_AUDIT.md`.
4. Inspecione o estado atual do código e do Git. Não suponha que etapas anteriores foram concluídas apenas porque estão descritas no plano.
5. Identifique a última etapa realmente concluída, o que está pendente e se existem testes/build quebrados.

Resuma o estado atual em até 10 tópicos e proponha somente a próxima tarefa pequena, respeitando as dependências. Não reescreva código que já funciona e não altere decisões registradas sem justificar e atualizar a documentação. Aguarde minha próxima instrução antes de executar uma etapa diferente.
```

# Prompt curto de verificação após qualquer etapa

```text
Antes de encerrar esta etapa, revise o diff completo. Verifique se a implementação respeita `AGENTS.md`, `docs/PROJECT_CONTEXT.md` e os requisitos relacionados. Execute os testes e o build aplicáveis. Corrija regressões, atualize `docs/REQUIREMENTS_TRACEABILITY.md` apenas com evidências verificadas e registre decisões novas em `docs/DECISIONS.md`. Informe arquivos alterados, comandos executados, resultados e pendências. Não comece a próxima etapa.
```
