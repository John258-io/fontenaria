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
│   ├── ClientesIniciais.java           ← cria clientes de demonstração (dev)
│   ├── ParametrosIniciais.java         ← cria tarifas e vencimento (dev)
│   ├── DadosDemoIniciais.java          ← cria leituras/facturas/pagamentos (dev)
│   └── SecurityConfig.java            ← CORS, rotas públicas, 401 em JSON
├── security/
│   ├── JwtService.java          ← emite/valida tokens JWT
│   └── JwtAuthFilter.java       ← lê o header "Authorization: Bearer …"
├── models/                      ← Usuario, Cliente, Leitura, Factura,
│                                   Pagamento e Parametro (entidades) + UsuarioSessao (DTO)
├── repositories/                ← repositories Spring Data de cada entidade
├── services/                    ← AuthService (login BCrypt), ClienteService (CRUD),
│                                   LeituraService, FacturaService, PagamentoService,
│                                   UsuarioService, ParametroService e RelatorioService
├── errors/                      ← excepções de negócio → 400/403/404 em JSON
├── dto/                         ← pedidos/respostas da API (records)
└── controllers/
    ├── AuthController.java      ← POST /api/auth/login · GET /me · POST /logout
    ├── DashboardController.java ← GET /api/dashboard
    ├── ClienteController.java   ← CRUD de /api/clientes
    ├── LeituraController.java   ← CRUD de /api/leituras
    ├── FacturaController.java   ← /api/facturas (emissão + consulta)
    ├── PagamentoController.java ← CRUD de /api/pagamentos
    ├── UsuarioController.java   ← /api/usuarios (Definições, admin)
    ├── ParametroController.java ← /api/parametros (Definições, admin)
    └── RelatorioController.java ← GET /api/relatorios
```

### Endpoints

| Método | Rota                        | Acesso  | Descrição                                        |
|--------|-----------------------------|---------|--------------------------------------------------|
| POST   | `/api/auth/login`           | público | `{telefone, senha}` → `{token, usuario}`         |
| GET    | `/api/auth/me`              | token   | dados do utilizador autenticado                  |
| POST   | `/api/auth/logout`          | token   | confirmação (o cliente descarta o token)         |
| GET    | `/api/dashboard`            | token   | saudação, indicadores, receita e actividade      |
| GET    | `/api/clientes`             | token   | lista + contagens (`?busca=` nome/telefone/bairro/contador) |
| GET    | `/api/clientes/{id}`        | token   | um cliente                                       |
| POST   | `/api/clientes`             | token   | cria um cliente (201)                            |
| PUT    | `/api/clientes/{id}`        | token   | actualiza um cliente                             |
| DELETE | `/api/clientes/{id}`        | token   | elimina um cliente                               |
| GET    | `/api/leituras`             | token   | lista + contagens (`?busca=`…&`periodo=AAAA-MM`) |
| GET    | `/api/leituras/{id}`        | token   | uma leitura                                      |
| POST   | `/api/leituras`             | token   | regista uma leitura (201); calcula período, leitura anterior e consumo |
| PUT    | `/api/leituras/{id}`        | token   | edita a leitura (recalcula o consumo)            |
| DELETE | `/api/leituras/{id}`        | token   | elimina (só se ainda não foi facturada)          |
| GET    | `/api/facturas`             | token   | lista + contagens (`?busca=`…&`estado=`…&`periodo=`…) |
| GET    | `/api/facturas/{id}`        | token   | uma factura                                      |
| GET    | `/api/facturas/{id}/pagamentos` | token | pagamentos de uma factura                        |
| POST   | `/api/facturas`             | token   | emite a factura de uma leitura (`{leituraId}`, 201) |
| POST   | `/api/facturas/gerar-mes`   | token   | emite todas as facturas em falta de um mês (`{periodo}`) |
| DELETE | `/api/facturas/{id}`        | token   | apaga (só facturas sem pagamentos)               |
| GET    | `/api/pagamentos`           | token   | lista + contagens + facturas pendentes (`?busca=`…&`metodo=`…) |
| POST   | `/api/pagamentos`           | token   | regista um recebimento (201)                     |
| DELETE | `/api/pagamentos/{id}`      | token   | anula um recibo (estorno)                        |
| GET    | `/api/relatorios`           | token   | consumo/facturação/recebimentos (`?inicio=`…&`fim=`, por omissão 6 meses) |
| GET    | `/api/usuarios`             | token+admin | lista os utilizadores                        |
| POST   | `/api/usuarios`             | token+admin | cria um utilizador (201)                     |
| PUT    | `/api/usuarios/{id}`        | token+admin | edita nome, telefone, perfil, estado e senha |
| GET    | `/api/parametros`           | token   | tarifas por m³ e vencimento                      |
| PUT    | `/api/parametros`           | token+admin | actualiza os parâmetros                      |

### Regras de negócio principais

- **Leituras**: uma por cliente e por mês (`periodo` = "AAAA-MM" da data);
  o consumo é `leituraActual − leituraAnterior` e a leitura anterior sai da
  última leitura do cliente.
- **Facturas**: nascem de uma leitura (uma factura por leitura), com
  `valor = consumo × tarifa da categoria` (tarifa definida em Definições e
  guardada na factura) e `vencimento = emissão + vencimento.dias`.
- **Pagamentos**: totais ou parciais, sempre contra uma factura pendente e sem
  exceder o saldo; quando `valorPago ≥ valor`, a factura passa a "Paga".
- **Definições**: alterações de parâmetros e utilizadores exigem o perfil
  `Administrador` (o servidor responde 403 caso contrário).

### Base de dados

Por omissão usa **H2 em memória** em modo PostgreSQL (nada a instalar; os dados
perdem-se ao reiniciar e o admin é recriado). Para usar **PostgreSQL** de
verdade, crie a base de dados `fontenaria-db` e comente/destaque as linhas em
`src/main/resources/application.properties` conforme as instruções no próprio
ficheiro.

Na primeira execução, além do admin e dos clientes, são criados também os
parâmetros (tarifas por m³ e vencimento) e 5 meses de leituras, facturas e
pagamentos de demonstração, para o sistema não arrancar vazio.

## Frontend — `frontend/`

Páginas estáticas (sem framework) que falam com a API via `fetch`:

```
frontend/
├── index.html          ← redirecciona para o login
├── login.html          ← login (js/login.js)
├── dashboard.html      ← painel preenchido a partir de /api/dashboard (js/dashboard.js)
├── clientes.html       ← lista/criação/edição de clientes (js/clientes.js)
├── leituras.html       ← leituras de contadores e consumo mensal (js/leituras.js)
├── facturas.html       ← emissão/consulta de facturas (js/facturas.js)
├── pagamentos.html     ← recebimentos contra facturas pendentes (js/pagamentos.js)
├── relatorios.html     ← consumo, facturação e receita por período (js/relatorios.js)
├── configuracoes.html  ← tarifas do sistema e utilizadores (js/configuracoes.js)
├── css/                ← global.css (tema/variables), login.css, dashboard.css,
│                         componentes.css (toolbar/tabela/modal/tabs) e clientes.css
└── js/
    ├── api.js          ← wrapper fetch + token no localStorage + endpoints
    ├── app.js          ← guardas de sessão, dados do utilizador, logout
    ├── layout.js       ← barra lateral/topo e ícones das páginas internas
    ├── pagina.js       ← sessão + utilizador das páginas internas
    ├── login.js
    ├── dashboard.js
    ├── clientes.js
    ├── leituras.js
    ├── facturas.js
    ├── pagamentos.js
    ├── relatorios.js
    └── configuracoes.js
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
