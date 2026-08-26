# FIPE Explorer

Ferramenta de exploração e análise da Tabela FIPE (preços de veículos no Brasil). Não é
uma réplica do consulta oficial da FIPE — o objetivo é permitir buscas combinadas, comparação
lado a lado entre veículos e visão agregada (rankings, distribuição por combustível) sobre o
mesmo dado histórico.

Os dados vêm de um CSV estático da Tabela FIPE (`data/tabela-fipe-336.csv`), importado uma
única vez para um banco Postgres na primeira subida do backend.

## Stack

**Backend** — `backend/`
- Java 21, Spring Boot 3.3 (Web, Data JPA, Validation)
- PostgreSQL 16 + Flyway (migração única de schema)
- Maven (wrapper `mvnw` incluído, não precisa ter Maven instalado)

**Frontend** — `frontend/`
- React 19 + TypeScript, Vite
- TanStack Query (data fetching/cache), React Router (rotas + estado compartilhável via URL)
- Tailwind CSS v4, Recharts (gráficos)

## Estrutura de pastas

```
backend/    API REST (Spring Boot). src/main/java/com/fipeexplorer/backend:
              domain/      entidades JPA (Brand, VehicleModel, FuelType, PriceEntry)
              repository/  Spring Data JPA + queries nativas (agregações de /stats)
              web/         controllers e DTOs
              importer/    importação do CSV da Tabela FIPE pro Postgres
            src/main/resources/db/migration/  schema (Flyway)
            src/test/...  testes de integração (@Tag("integration"), banco de dev real)

frontend/   SPA (Vite + React). src/:
              api/         cliente HTTP, tipos, hooks do TanStack Query
              pages/       as 4 telas (uma por rota)
              components/  componentes de UI reaproveitados entre telas (ex.: gráfico de depreciação)
              lib/         utilitários compartilhados (paleta de combustível, formatação de ano)

data/       CSV de origem da Tabela FIPE (tabela-fipe-336.csv)
docker-compose.yml   Postgres de desenvolvimento
```

## Rodando localmente

Pré-requisitos: Docker, JDK 21+, Node 20+.

A ordem importa — cada passo depende do anterior estar de pé:

```bash
# 1. banco de dados (Postgres, porta 5433)
docker compose up -d

# 2. backend — importa o CSV automaticamente na primeira subida (só quando a tabela
#    price_entry está vazia; nas próximas vezes o import é pulado)
cd backend
./mvnw spring-boot:run

# 3. frontend
cd frontend
cp .env.example .env.local   # já vem com VITE_API_BASE_URL=http://localhost:8080/api/v1
npm install
npm run dev
```

A API sobe em `http://localhost:8080`, o frontend em `http://localhost:5173`. O backend já
libera essa origem via CORS por padrão (veja `FRONTEND_ORIGIN` na seção de produção).

### Variáveis de ambiente do backend

Todas têm default pra desenvolvimento local — só precisa sobrescrever em produção ou se as
portas padrão colidirem com algo na sua máquina.

| Variável | Default | Uso |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5433/fipe_explorer` | conexão Postgres |
| `DB_USER` / `DB_PASSWORD` | `fipe` / `fipe` | credenciais do Postgres |
| `FIPE_CSV_PATH` | `../data/tabela-fipe-336.csv` | caminho do CSV pra importação inicial |
| `FRONTEND_ORIGIN` | `http://localhost:5173` | origem(ns) liberada(s) no CORS, separadas por vírgula |
| `SERVER_PORT` | `8080` | porta HTTP da API |

### Rodando os testes

```bash
cd backend
./mvnw test                                          # unitários (rápidos, sem banco)
./mvnw test -Dgroups=integration -DexcludedGroups=    # integração — precisa do `docker compose up -d`
```

Os testes de integração (`@Tag("integration")`) batem direto no Postgres de desenvolvimento já
populado pelo import do CSV, em vez de Testcontainers — decisão documentada no próprio código
(`CatalogControllerIntegrationTest`), por incompatibilidade do Testcontainers com o Docker
Desktop usado no desenvolvimento deste projeto (Windows). Numa máquina Linux/CI com Docker
"de verdade" os módulos Testcontainers já estão nas dependências, então dá pra migrar esses
testes se fizer sentido.

```bash
cd frontend
npm run lint     # oxlint
npx tsc -b       # type-check
npm run build    # build de produção (roda type-check antes)
```

## As telas

| Rota | Tela | O que faz |
|---|---|---|
| `/` | Busca | Filtros encadeados (tipo → marca/modelo/ano/combustível, todos combináveis) sobre a Tabela FIPE, com paginação e ordenação. Cada linha pode ser marcada para comparação (até 4). |
| `/vehicles/:modelId` | Detalhe | Curva de depreciação de um modelo (gráfico por ano, uma série por combustível quando o modelo teve mais de um) e a tabela de preços por trás do gráfico. |
| `/compare` | Comparador | Até 4 veículos lado a lado, com o mais barato e o mais caro do grupo destacados (cor + selo de texto). Os ids selecionados vão na própria URL — o link é compartilhável e sobrevive a um refresh. |
| `/insights` | Insights | Estatísticas por tipo de veículo: resumo (modelos, faixa de preço), ranking de marcas mais caras/baratas (preço médio ponderado pelo ano mais recente de cada modelo, não pelo histórico inteiro — evita que marcas com modelos antigos distorçam a média) e distribuição por combustível. |

## Calculadora de valor ajustado

Início do pivô do projeto pra uso por pessoa comum (não só análise agregada da Tabela FIPE).
Primeira peça: na tela de Detalhe, `POST /api/v1/vehicles/{priceEntryId}/price-estimate` ajusta
o preço FIPE de uma linha específica (ano+combustível) pros três fatores que o preço médio da
FIPE não captura — quilometragem, estado de conservação e opcionais — e devolve o preço ajustado
junto com o detalhamento de cada componente. Sem persistência: é stateless, calcula e devolve.

Toda a lógica fica centralizada em `PriceEstimateService`
(`backend/.../estimate/PriceEstimateService.java`), com os percentuais como constantes nomeadas
(nos enums `VehicleCondition`/`VehicleExtra`, ou na tabela de faixas de km dentro do serviço) —
não estão espalhados pelo código. **Importante: essa é uma regra de negócio inventada para este
produto, não uma fórmula com resposta "certa"** — os números abaixo são um ponto de partida
propositalmente simples, pensado pra ser fácil de ajustar depois (mudar uma constante, não
reescrever lógica) conforme o feedback de uso real chegar.

Cada componente é um **percentual sobre o preço base da FIPE**, e os percentuais são somados
(não compostos) antes de aplicar ao preço — assim o valor em R$ de cada linha do detalhamento
soma exatamente o ajuste total, sem surpresa de arredondamento composto.

**1. Quilometragem** — calcula-se uma km "esperada" pra idade do veículo (ano da linha de
preço até o ano atual) assumindo um uso moderado de **12.000 km/ano**, e compara a km informada
com essa expectativa em faixas (degraus, não uma curva contínua — mais fácil de explicar e de
ajustar depois):

| km informada ÷ km esperada | Ajuste |
|---|---|
| até 50% | +5% (muito abaixo do esperado) |
| até 90% | +2% |
| até 110% | 0% (dentro do esperado) |
| até 150% | −5% |
| até 200% | −10% |
| acima de 200% | −15% (teto — evita desconto sem limite) |

Veículo "zero km" (código de ano `32000` da Tabela FIPE) não tem idade: a km esperada é 0, então
qualquer km informada acima de zero já cai na pior faixa.

**2. Estado de conservação** — percentual fixo por faixa. "Bom" é a referência (0%, é
aproximadamente o estado médio que o preço da FIPE já assume implicitamente): `Excelente +5%` /
`Bom 0%` / `Regular −10%` / `Ruim −20%`.

**3. Opcionais** — lista curta e pré-definida (não é texto livre), cada item soma um percentual
fixo, independente dos outros: `Ar-condicionado +1%` / `Direção hidráulica/elétrica +1%` /
`Rodas de liga leve +1,5%` / `Teto solar +2%` / `Bancos de couro +2%` / `Central multimídia
+1,5%` / `Blindagem +8%`.

O intervalo resultante possível vai de aproximadamente −35% (estado ruim, km muito alta, sem
opcionais) a +27% (excelente, km baixa, todos os opcionais) — sempre mantém o preço final
positivo, sem precisar de um limite artificial.

## Build de produção

Não há hospedagem configurada ainda; isto documenta o caminho pra chegar lá.

**Backend** — gera um jar executável autocontido:

```bash
cd backend
./mvnw clean package -DskipTests
java -jar target/backend-0.1.0-SNAPSHOT.jar
```

Em produção, defina `DB_URL`/`DB_USER`/`DB_PASSWORD` apontando pro Postgres real e
`FRONTEND_ORIGIN` com o domínio onde o frontend for servido (sem isso, o navegador bloqueia as
chamadas à API por CORS).

**Frontend** — `vite build` gera arquivos estáticos; não existe servidor Node em produção:

```bash
cd frontend
VITE_API_BASE_URL=https://sua-api.exemplo.com/api/v1 npm run build
```

Importante: `VITE_API_BASE_URL` é embutida no bundle em **tempo de build**, não lida em
runtime — o valor certo (URL pública da API) precisa estar setado antes do `npm run build`, não
depois. O resultado fica em `frontend/dist/` — qualquer servidor de arquivos estáticos serve
(nginx, Caddy, `serve -s dist`, um bucket S3 + CDN etc.). Como é uma SPA com rotas via
React Router, o servidor precisa cair pro `index.html` em qualquer caminho não encontrado
(ex. `/vehicles/123` direto na URL), não devolver 404.

Um `docker-compose.yml` de produção real ficaria: Postgres (com volume persistente e senha
fora do compose), o jar do backend numa imagem Java, e o `dist/` do frontend atrás de um nginx
— não incluído aqui porque ainda não há onde hospedar de fato; a intenção é essa lista virar um
`docker-compose.prod.yml` quando isso for necessário.
