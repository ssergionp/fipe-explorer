# FIPE Explorer

Monorepo para consulta e análise de dados da Tabela FIPE.

- `backend/` — Java 21 + Spring Boot 3 (Web, Data JPA, Validation), PostgreSQL, Flyway.
- `frontend/` — React 18 + TypeScript + Vite, TanStack Query, React Router, Tailwind, Recharts.
- `data/` — CSV de origem da Tabela FIPE (`tabela-fipe-336.csv`).

## Subindo o projeto

```bash
# 1. banco de dados
docker compose up -d

# 2. backend (importa o CSV automaticamente na primeira subida)
cd backend
./mvnw spring-boot:run

# 3. frontend
cd frontend
npm install
npm run dev
```

A API sobe em `http://localhost:8080`, o frontend em `http://localhost:5173`.
