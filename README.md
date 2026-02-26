# NT Manuscript Coverage - Sistema de Cobertura Textual do Novo Testamento

Sistema em Kotlin que analisa manuscritos públicos do Novo Testamento datados até o século V e calcula a cobertura textual acumulada por livro.

## Stack Tecnológica

- **Kotlin** + **Ktor** (servidor HTTP)
- **PostgreSQL** (banco de dados)
- **Exposed** (ORM type-safe)
- **Flyway** (migrações de banco)
- **Jsoup** (scraping HTML)
- **kotlinx.serialization** (JSON)

## Execução com Docker (recomendado)

```bash
# Compilar o projeto
./gradlew build

# Subir PostgreSQL + aplicação
docker compose up -d --build

# Verificar logs
docker compose logs -f app

# Parar
docker compose down

# Parar e limpar dados do banco
docker compose down -v
```

A aplicação estará disponível em `http://localhost:8080`.

## Execução Local (sem Docker)

### Pré-requisitos
- Java 21+
- PostgreSQL 14+

### Configuração do Banco

```bash
createdb nt_coverage
```

Variáveis de ambiente (opcional):

```bash
export DATABASE_URL="jdbc:postgresql://localhost:5432/nt_coverage"
export DATABASE_USER="postgres"
export DATABASE_PASSWORD="postgres"
```

### Build e Execução

```bash
./gradlew build
./gradlew run
```

O servidor inicia na porta 8080 (configurável via variável `PORT`).

Na inicialização:
1. Flyway executa as migrações de esquema
2. Os 27 livros e 7.956 versículos canônicos do NT são inseridos
3. Os manuscritos do seed são carregados e expandidos em versículos individuais
4. A tabela de cobertura por século é materializada

## Endpoints da API

| Endpoint | Descrição |
|---|---|
| `GET /` | Informações do serviço |
| `GET /coverage` | Relatório completo de cobertura (séculos I-V) |
| `GET /coverage/{book}` | Cobertura de um livro específico (ex: `Matthew`, `1 Corinthians`) |
| `GET /century/{number}` | Cobertura acumulada até o século N (1-5) |

### Exemplo de resposta - `GET /century/4`

```json
{
  "century": 4,
  "summary": {
    "totalNtVerses": 7956,
    "coveredVerses": 7956,
    "overallCoveragePercent": 100.00
  },
  "books": [
    {
      "bookName": "Matthew",
      "coveredVerses": 1071,
      "totalVerses": 1071,
      "coveragePercent": 100.00
    }
  ],
  "fullyAttested": ["Matthew", "Mark", ...],
  "notFullyAttested": []
}
```

## Modelo de Dados

- **books** - 27 livros canônicos do NT
- **verses** - 7.956 versículos individuais (chave: book_id + chapter + verse)
- **manuscripts** - Manuscritos com século estimado e tipo (papiro/uncial)
- **manuscript_verses** - Relação N:N entre manuscritos e versículos atestados
- **coverage_by_century** - Cache materializado de cobertura acumulada

## Manuscritos Incluídos

O seed inclui ~55 manuscritos dos séculos I-V:
- Papiros: P1, P4, P5, P9, P10, P12, P13, P15, P16, P17, P18, P20, P22, P23, P24, P27, P30, P32, P37, P38, P39, P40, P45, P46, P47, P48, P49, P51, P52, P53, P64, P65, P66, P69, P70, P72, P74, P75, P77, P78, P87, P90, P98, P100, P103, P104, P115
- Unciais: 01 (Sinaiticus), 02 (Alexandrinus), 03 (Vaticanus), 04 (Ephraemi), 05 (Bezae), 032 (Washingtonianus), 048, 0162, 0171, 0189

## Regras de Negócio

- Século estimado com intervalo (ex: "II/III") usa o **mais antigo**
- Manuscritos posteriores ao século V são **ignorados**
- Cobertura é **cumulativa**: século III inclui tudo dos séculos I-III
- Um versículo conta como **coberto** se aparece em ao menos um manuscrito
- Duplicações entre manuscritos **não** são contadas duas vezes
