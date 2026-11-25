# EventIF — Sistema de Gestão de Eventos Acadêmicos

Aplicação web server-side construída para a disciplina **Tópicos em Java para a Web** (IFCE – Campus Fortaleza, Prof. Corneli Gomes Furtado Júnior) com foco na divulgação de eventos acadêmicos (semanas de curso, congressos e jornadas), gestão de atividades e vagas, registro de presença e emissão de certificados imprimíveis.

---

## Integrante(s)

Trabalho desenvolvido na modalidade individual:

- **Nome:** `João Íthalo de Almeida Lima`
- **Matrícula:** `20241045050596`
- **GitHub / E-mail:** `[PREENCHER GITHUB / E-MAIL]`

## Vídeo Explicativo

- **Link do vídeo (YouTube não listado ou Google Drive):** `[INSERIR LINK DO VÍDEO NO YOUTUBE (NÃO LISTADO) OU GOOGLE DRIVE]`

---

## Tecnologias e Pré-requisitos

- **Linguagem:** Java 21 (JDK 21+)
- **Backend:** Spring Boot 3.4.5, Spring MVC, Spring Data JPA (Hibernate), Spring Security 6, Jakarta Bean Validation
- **Interface Web:** Thymeleaf (renderização no servidor com layout compartilhado em fragmentos), Bootstrap 5 e CSS próprio (`@media print` para certificados)
- **Banco de Dados:** MySQL 8.4 em execução (Docker Compose ou instância local); H2 em memória exclusivamente no perfil/escopo `test`
- **Build:** Maven Wrapper (`./mvnw` e `mvnw.cmd`) incluso no repositório

### Pré-requisitos para Execução Recomendada (Docker)
- **Docker Engine / Docker Desktop:** `24+`
- **Docker Compose plugin (`docker compose`):** `v2+`

### Pré-requisitos para Execução Manual (sem Docker)
- **Java JDK:** `21` (ou superior compatível com release 21)
- **MySQL Server:** `8.4` (ou `8.0+`) ativo localmente
- **Maven:** não precisa instalar globalmente — use `./mvnw` (Linux/macOS) ou `.\mvnw.cmd` (Windows)

---

## Execução com Docker Compose (Recomendada)

### 1. Configuração opcional de variáveis de ambiente
O projeto já possui defaults fictícios configurados diretamente no `docker-compose.yml` e em `application.properties`, então é possível subir imediatamente. Caso deseje personalizar portas, banco ou credenciais de demonstração:

```bash
cp .env.example .env
```

> **Atenção:** nunca versione arquivos `.env` com segredos reais no Git (`.gitignore` já os ignora automaticamente).

### 2. Subir aplicação e banco MySQL 8.4

Na raiz do repositório, execute:

```bash
docker compose up --build
```

O Compose iniciará o serviço `db` (`mysql:8.4`) com volume persistente (`eventif_mysql_data`) e aguardará o `healthcheck` (`mysqladmin ping`) ficar saudável antes de iniciar o serviço `app`.

### 3. Acessar o sistema

Quando os logs exibirem `Started EventifApplication`, acesse no navegador:

- **URL da aplicação:** `http://localhost:8080`
- **Porta externa padrão do MySQL no host:** `3307` (mapeada para `3306` no contêiner `db`, evitando conflito caso já exista um MySQL local na porta `3306`).

### 4. Parar os contêineres

- Para parar mantendo os dados salvos no volume do MySQL:
  ```bash
  docker compose down
  ```
- Para parar e apagar completamente o volume do banco (zerar os dados):
  ```bash
  docker compose down -v
  ```

---

## Execução Manual (Alternativa sem Docker)

### 1. Preparar o banco MySQL
Inicie seu MySQL local (`localhost:3306`) e crie o banco e o usuário correspondentes (ou utilize suas próprias credenciais via variáveis de ambiente):

```sql
CREATE DATABASE IF NOT EXISTS eventif
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'eventif_user'@'localhost' IDENTIFIED BY 'eventif_dev_pass';
GRANT ALL PRIVILEGES ON eventif.* TO 'eventif_user'@'localhost';
FLUSH PRIVILEGES;
```

### 2. Configurar variáveis e iniciar a aplicação

**Linux / macOS:**
```bash
export SPRING_DATASOURCE_URL="jdbc:mysql://localhost:3306/eventif?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=America/Fortaleza&characterEncoding=UTF-8"
export SPRING_DATASOURCE_USERNAME="eventif_user"
export SPRING_DATASOURCE_PASSWORD="eventif_dev_pass"
./mvnw spring-boot:run
```

**Windows PowerShell:**
```powershell
$env:SPRING_DATASOURCE_URL = "jdbc:mysql://localhost:3306/eventif?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=America/Fortaleza&characterEncoding=UTF-8"
$env:SPRING_DATASOURCE_USERNAME = "eventif_user"
$env:SPRING_DATASOURCE_PASSWORD = "eventif_dev_pass"
.\mvnw.cmd spring-boot:run
```

Ou empacotando e executando o JAR:
```bash
./mvnw clean package -DskipTests
java -jar target/eventif-0.0.1-SNAPSHOT.jar
```

Após a subida, acesse **`http://localhost:8080`**.

### 3. Executar a suíte de testes automatizados (H2 em memória)

```bash
./mvnw clean test
```

*(No Windows PowerShell, utilize `.\mvnw.cmd clean test` com `JAVA_HOME` apontando para o JDK 21).*

---

## Usuários de Teste e Dados de Demonstração (`RS07` / `RT04`)

Na subida inicial da aplicação, o inicializador Java (`DataInitializer`) popula de forma idempotente (sem duplicar em reinicializações) os perfis base, 2 categorias (`Computação e Tecnologia` e `Pesquisa e Extensão`), 3 eventos com atividades e as contas fictícias abaixo com senhas criptografadas via `BCryptPasswordEncoder`:

| Perfil | E-mail | Senha fictícia padrão | Dados pré-carregados para demonstração |
|---|---|---|---|
| **Administrador** (`ROLE_ADMIN`) | `admin@eventif.edu.br` | `AdminDemo@123` | Acesso à gestão de categorias (`/admin/categorias`) e perfis de usuários (`/admin/usuarios`). |
| **Organizador 1** (`ROLE_ORGANIZADOR`) | `organizador@eventif.edu.br` | `OrgDemo@123` | Dono de 1 evento `PUBLICADO` (*Semana Acadêmica de Computação e Inovação*) e 1 evento `ENCERRADO` (*Jornada de Engenharia de Software do IFCE*). |
| **Organizador 2** (`ROLE_ORGANIZADOR`) | `organizador2@eventif.edu.br` | `Org2Demo@123` | Dono de 1 evento `PUBLICADO` (*Simpósio de Pesquisa Aplicada e Extensão Tecnológica*), permitindo testar bloqueio **HTTP 403** ao tentar editar eventos do Organizador 1 pela URL. |
| **Participante** (`ROLE_PARTICIPANTE`) | `participante@eventif.edu.br` | `PartDemo@123` | Inscrito em 1 atividade publicada e em 2 atividades do evento encerrado (1 com presença confirmada de `4h` para emissão imediata de certificado e 1 sem presença). |

> Novas contas criadas pela tela pública `/cadastro` recebem automaticamente `ROLE_PARTICIPANTE`.

---

## Variáveis de Ambiente

| Variável | Finalidade | Valor padrão (fictício) |
|---|---|---|
| `SERVER_PORT` | Porta HTTP exposta pela aplicação | `8080` |
| `MYSQL_HOST_PORT` | Porta externa do MySQL no Docker Compose | `3307` |
| `MYSQL_DATABASE` | Nome do schema no MySQL | `eventif` |
| `MYSQL_USER` | Usuário da aplicação no MySQL | `eventif_user` |
| `MYSQL_PASSWORD` | Senha do usuário da aplicação no MySQL | `eventif_dev_pass` |
| `MYSQL_ROOT_PASSWORD` | Senha root do contêiner MySQL (`db`) | `eventif_root_dev_pass` |
| `SPRING_DATASOURCE_URL` | URL JDBC do MySQL (no Compose aponta para `db:3306`) | `jdbc:mysql://localhost:3306/eventif?...` |
| `SPRING_DATASOURCE_USERNAME` | Usuário JDBC usado pelo Spring | `eventif_user` |
| `SPRING_DATASOURCE_PASSWORD` | Senha JDBC usada pelo Spring | `eventif_dev_pass` |
| `SPRING_JPA_DDL_AUTO` | Estratégia de schema Hibernate (`update`, `validate`, etc.) | `update` |
| `EVENTIF_ADMIN_NOME` | Nome da conta inicial `ROLE_ADMIN` | `Administrador EventIF` |
| `EVENTIF_ADMIN_EMAIL` | E-mail da conta inicial `ROLE_ADMIN` | `admin@eventif.edu.br` |
| `EVENTIF_ADMIN_SENHA` | Senha da conta inicial `ROLE_ADMIN` | `AdminDemo@123` |
| `EVENTIF_SEED_DEMO_ENABLED` | Ativa (`true`) ou desativa (`false`) os dados de demonstração | `true` |
| `EVENTIF_DEMO_ORG_NOME` / `_EMAIL` / `_SENHA` | Credenciais do Organizador 1 de demonstração | `organizador@eventif.edu.br` / `OrgDemo@123` |
| `EVENTIF_DEMO_ORG2_NOME` / `_EMAIL` / `_SENHA` | Credenciais do Organizador 2 de demonstração | `organizador2@eventif.edu.br` / `Org2Demo@123` |
| `EVENTIF_DEMO_PART_NOME` / `_EMAIL` / `_SENHA` | Credenciais do Participante de demonstração | `participante@eventif.edu.br` / `PartDemo@123` |

---

## Visão Geral dos Pacotes e Arquitetura

A aplicação segue estritamente a separação em camadas **`controller → service → repository`**, com regras de negócio (`RN01`–`RN07`) e validação de propriedade/IDOR (`RS05`–`RS06`) concentradas na camada `service`:

```text
src/main/java/br/edu/ifce/eventif/
├── EventifApplication.java   # Ponto de entrada Spring Boot
├── config/                   # SecurityConfig, TimeConfig (Clock injetável), DataInitializer
├── controller/               # HomeController, AuthController, ParticipanteController,
│                             # OrganizadorEventoController, OrganizadorAtividadeController,
│                             # AdminCategoriaController, AdminUsuarioController
├── dto/                      # Form DTOs (Bean Validation) e View DTOs (inclui MinhaListaEsperaView)
├── entity/                   # Entidades JPA: Usuario, Perfil, Categoria, Evento, Atividade,
│                             # Inscricao e ListaEspera (EX01; Certificado sem entidade JPA)
├── enums/                    # StatusEvento (RASCUNHO, PUBLICADO, ENCERRADO) e TipoAtividade
├── exception/                # RegraNegocioException, AcessoNegadoPropriedadeException,
│                             # RecursoNaoEncontradoException e GlobalExceptionHandler (400/403/404/409)
├── repository/               # Repositórios Spring Data JPA (inclui paginação, PESSIMISTIC_WRITE e ListaEsperaRepository)
├── security/                 # CustomUserDetails, CustomUserDetailsService, SecurityService, PerfilConstants
└── service/                  # UsuarioService, CategoriaService, EventoService, AtividadeService,
                              # InscricaoService (EX01/EX02), CertificadoService, CertificadoPdfService (EX03)
```

- **Templates Thymeleaf (`src/main/resources/templates/`):** `fragments/layout.html`, `publico/`, `auth/`, `participante/`, `organizador/`, `admin/` e `error/` (`403`, `404`, `500`, `regra-negocio`).
- **Estilos (`src/main/resources/static/css/eventif.css`):** identidade visual com Bootstrap 5 e regras `@media print` A4 para impressão do certificado.
- **Testes automatizados (`src/test/java/br/edu/ifce/eventif/`):** `48` testes cobrindo mapeamento JPA, compatibilidade de schema MySQL, consultas Spring Data, segurança e matriz de acesso, regras de negócio `RN01`–`RN07`, proteção contra IDOR (`403`), regressão HTTP com `Clock` controlável, concorrência com lock pessimista, inspeção arquitetural e todas as funcionalidades extras (`EX01`–`EX04`).

---

## Funcionalidades Extras e Limitações Conhecidas

### Funcionalidades extras opcionais implementadas (`EX01`–`EX04`)
- **`EX01` — Lista de espera com promoção automática FIFO:** quando uma atividade atinge `0` vagas restantes em evento `PUBLICADO`, participantes podem ingressar na fila (`POST /inscricoes/atividades/{id}/espera`) e acompanhar sua posição em *Minhas Inscrições*. Ao cancelar uma inscrição antes do início da atividade, o próximo candidato elegível (por ordem de chegada e sem choque de horário) é promovido automaticamente sob lock pessimista.
- **`EX02` — Bloqueio de horários sobrepostos:** impede inscrição (ou entrada/promoção na lista de espera) em atividades cujo intervalo `[inicio, fim]` se sobreponha ao de outra atividade em que o participante já esteja inscrito (permitindo horários adjacentes, como `08:00–10:00` e `10:00–12:00`).
- **`EX03` — Certificado em PDF:** disponibiliza download em PDF A4 paisagem gerado dinamicamente com OpenPDF (`GET /certificados/eventos/{eventoId}/pdf`) reutilizando as mesmas validações de evento `ENCERRADO` e presença confirmada (`RN07`).
- **`EX04` — Testes automatizados JUnit:** suíte com `48` testes em `14` classes JUnit cobrindo regras obrigatórias, segurança, concorrência e todos os extras (`FuncionalidadesExtrasTest`).

### Limitações conhecidas
- Não realiza envio de e-mails reais (ex.: confirmação de cadastro ou notificação de inscrição).
- Certificados são gerados dinamicamente sob demanda a partir das inscrições com presença confirmada em eventos `ENCERRADO`, sem persistência de código verificador próprio em tabela separada.
- Eventos marcados como `ENCERRADO` tornam-se imutáveis para edição/exclusão estrutural, preservando a consistência dos certificados emitidos.
