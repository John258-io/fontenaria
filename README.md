# Fontanária Manhiça

Sistema de gestão da Fontanária Manhiça (clientes, leituras, facturação e pagamentos).

O projecto está separado em duas partes independentes:

```
fontenaria/
├── backend/fontenaria/     ← API REST (Spring Boot) — porto 8080
└── frontend/               ← interface estática (HTML + CSS + JS) — porto 5501
```

O frontend consome a API em `http://localhost:8080/api`; a autenticação é feita
com **tokens JWT** (o backend é stateless, não há sessões HTTP).

## Backend — `backend/fontenaria`

Spring Boot 3.5 (Java 17) + JPA + Spring Security (filtro JWT) + H2/PostgreSQL.

```bash
cd backend/fontenaria
./mvnw spring-boot:run
```

Estrutura:

```
src/main/java/mz/edu/fontanaria/
├── FontenariaApplication.java   ← ponto de entrada
├── config/
│   ├── AdminInicial.java              ← cria o admin na 1.ª execução (dev)
│   ├── UtilizadorPadraoInicial.java    ← cria o utilizador padrão na 1.ª execução (dev)
│   └── SecurityConfig.java            ← CORS, rotas públicas, 401 em JSON
├── security/
│   ├── JwtService.java          ← emite/valida tokens JWT
│   └── JwtAuthFilter.java       ← lê o header "Authorization: Bearer …"
├── models/                      ← Usuario (entidade) e UsuarioSessao (DTO)
├── repositories/                ← UsuarioRepository (Spring Data)
├── services/                    ← AuthService (login BCrypt)
├── dto/                         ← pedidos/respostas da API (records)
└── controllers/
    ├── AuthController.java      ← POST /api/auth/login · GET /me · POST /logout
    └── DashboardController.java ← GET /api/dashboard
```

### Endpoints

| Método | Rota                  | Acesso  | Descrição                                        |
|--------|-----------------------|---------|--------------------------------------------------|
| POST   | `/api/auth/login`     | público | `{telefone, senha}` → `{token, usuario}`         |
| GET    | `/api/auth/me`        | token   | dados do utilizador autenticado                  |
| POST   | `/api/auth/logout`    | token   | confirmação (o cliente descarta o token)         |
| GET    | `/api/dashboard`      | token   | saudação, indicadores, receita, actividade       |

### Base de dados

Por omissão usa **H2 em memória** em modo PostgreSQL (nada a instalar; os dados
perdem-se ao reiniciar e o admin é recriado). Para usar **PostgreSQL** de
verdade, crie a base de dados `fontenaria-db` e comente/destaque as linhas em
`src/main/resources/application.properties` conforme as instruções no próprio
ficheiro.

## Frontend — `frontend/`

Páginas estáticas (sem framework) que falam com a API via `fetch`:

```
frontend/
├── index.html          ← redirecciona para o login
├── login.html          ← login (js/login.js)
├── dashboard.html      ← painel preenchido a partir de /api/dashboard (js/dashboard.js)
├── clientes.html       ← placeholder "em construção"
├── leituras.html       ← placeholder "em construção"
├── configuracoes.html  ← placeholder "em construção"
├── css/                ← global.css (tema/variables), login.css, dashboard.css
└── js/
    ├── api.js          ← wrapper fetch + token no localStorage
    ├── app.js          ← guardas de sessão, dados do utilizador, logout
    ├── layout.js       ← sidebar/topo partilhados das páginas simples
    ├── login.js
    ├── dashboard.js
    └── pagina.js
```

Para o correr (o backend tem CORS aberto em desenvolvimento), use qualquer
servidor estático, por exemplo o **Live Server** do VS Code na pasta `frontend`
(porto 5501, já configurado em `.vscode/settings.json`), ou:

```bash
cd frontend
npx serve -l 5501 .
```

Depois abra `http://127.0.0.1:5501/`.

## Credenciais de demonstração

| Telefone      | Palavra-passe | Perfil         |
|---------------|---------------|----------------|
| `84 123 4567` | `admin123`    | `Administrador` |
| `84 123 4568` | `user123`     | `Operador`      |

(Criadas por `AdminInicial` e `UtilizadorPadraoInicial` na primeira execução —
mude as palavras-passe e remova essas classes antes de produção.)

## Aplicação do tema

As variáveis de cor/tipo estão todas em `frontend/css/global.css` (bloco
`:root`). Mudando esse bloco, muda o tema inteiro do sistema.
