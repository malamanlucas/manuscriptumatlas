#!/usr/bin/env python3
"""
Analyzes the Manuscriptum Atlas codebase and updates .cursor/rules/*.mdc
files with current counts, lists, and structure information.

Usage: python scripts/update_rules.py
"""

import re
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
RULES = ROOT / ".cursor" / "rules"
BACKEND = ROOT / "src" / "main" / "kotlin" / "com" / "ntcoverage"
MIGRATIONS = ROOT / "src" / "main" / "resources" / "db" / "migration"
FRONTEND = ROOT / "frontend"


# ---------------------------------------------------------------------------
# Collectors
# ---------------------------------------------------------------------------

def collect_tables() -> list[str]:
    """Return list of Exposed table object names from Tables.kt."""
    tables_file = BACKEND / "model" / "Tables.kt"
    if not tables_file.exists():
        return []
    text = tables_file.read_text()
    return re.findall(r"^object (\w+)\s*:\s*(?:IntIdTable|Table)", text, re.MULTILINE)


def collect_table_sql_names() -> list[str]:
    """Return list of SQL table names (lowercase) from Tables.kt."""
    tables_file = BACKEND / "model" / "Tables.kt"
    if not tables_file.exists():
        return []
    text = tables_file.read_text()
    return re.findall(r'(?:IntIdTable|Table)\("(\w+)"\)', text)


def collect_dto_count() -> int:
    """Count @Serializable data/value classes in model/."""
    total = 0
    for f in (BACKEND / "model").glob("*.kt"):
        total += len(re.findall(r"@Serializable", f.read_text()))
    return total


def collect_kt_files(package: str) -> list[str]:
    """Return sorted list of .kt filenames in a backend package."""
    pkg = BACKEND / package
    if not pkg.exists():
        return []
    return sorted(f.name for f in pkg.glob("*.kt"))


def collect_endpoint_count() -> int:
    """Count HTTP endpoint declarations across all route files."""
    total = 0
    routes_dir = BACKEND / "routes"
    if not routes_dir.exists():
        return 0
    for f in routes_dir.glob("*.kt"):
        text = f.read_text()
        total += len(re.findall(r'^\s*(get|post|put|delete|patch)\("', text, re.MULTILINE))
    return total


def collect_migrations() -> list[dict]:
    """Return list of migration dicts {filename, number, description}, sorted by number."""
    if not MIGRATIONS.exists():
        return []
    migs = []
    for f in MIGRATIONS.glob("V*.sql"):
        m = re.match(r"V(\d+)__(.+)\.sql", f.name)
        if m:
            migs.append({
                "filename": f.name,
                "number": int(m.group(1)),
                "description": m.group(2).replace("_", " "),
            })
    return sorted(migs, key=lambda x: x["number"])


def collect_hooks() -> list[str]:
    """Return sorted list of hook filenames in frontend/hooks/."""
    hooks_dir = FRONTEND / "hooks"
    if not hooks_dir.exists():
        return []
    return sorted(f.name for f in hooks_dir.glob("use*.ts"))


def collect_pages() -> list[str]:
    """Return sorted relative paths of page.tsx under frontend/app/."""
    app_dir = FRONTEND / "app"
    if not app_dir.exists():
        return []
    pages = []
    for p in sorted(app_dir.rglob("page.tsx")):
        pages.append(str(p.relative_to(app_dir)))
    return pages


def collect_components() -> dict[str, list[str]]:
    """Return {subfolder: [file.tsx, ...]} for frontend/components/."""
    comp_dir = FRONTEND / "components"
    if not comp_dir.exists():
        return {}
    result: dict[str, list[str]] = {}
    for item in sorted(comp_dir.iterdir()):
        if item.is_dir():
            tsx_files = sorted(f.name for f in item.glob("*.tsx"))
            if tsx_files:
                result[item.name] = tsx_files
    root_tsx = sorted(f.name for f in comp_dir.glob("*.tsx"))
    if root_tsx:
        result["(raiz)"] = root_tsx
    return result


def collect_type_count() -> int:
    """Count exported interfaces and types in frontend/types/index.ts."""
    types_file = FRONTEND / "types" / "index.ts"
    if not types_file.exists():
        return 0
    text = types_file.read_text()
    return len(re.findall(r"^export\s+(interface|type)\s+", text, re.MULTILINE))


# ---------------------------------------------------------------------------
# Updaters
# ---------------------------------------------------------------------------

def replace_section(text: str, start_heading: str, new_content: str, stop_pattern: str = r"^## ") -> str:
    """Replace content between start_heading and the next section heading."""
    pattern = re.compile(
        rf"(^{re.escape(start_heading)}\n)(.*?)(?=\n{stop_pattern}|\Z)",
        re.MULTILINE | re.DOTALL,
    )
    return pattern.sub(rf"\g<1>\n{new_content}\n", text)


def replace_inline_count(text: str, pattern: str, new_value: str) -> str:
    """Replace a regex pattern with new_value (for inline count updates)."""
    return re.sub(pattern, new_value, text)


def update_project_overview():
    path = RULES / "project-overview.mdc"
    text = path.read_text()

    tables = collect_tables()
    table_sql_names = collect_table_sql_names()
    dto_count = collect_dto_count()
    repos = collect_kt_files("repository")
    services = collect_kt_files("service")
    route_files = collect_kt_files("routes")
    endpoint_count = collect_endpoint_count()
    config_files = collect_kt_files("config")
    seed_files = collect_kt_files("seed")

    table_count = len(tables)
    repo_count = len(repos)
    service_count = len(services)
    route_file_count = len(route_files)
    config_count = len(config_files)
    seed_count = len(seed_files)

    # -- Update packages table --
    packages_table = (
        "| Pacote | Responsabilidade |\n"
        "|--------|------------------|\n"
        f"| `config` | Configuração (DB, Flyway com fallback SchemaUtils, Ingestão, Locale) — {config_count} arquivos |\n"
        f"| `model` | Tables ({table_count} tabelas Exposed) e DTOs ({dto_count} `@Serializable`) |\n"
        f"| `repository` | Acesso a dados via `transaction {{}}` ({repo_count} repositórios) |\n"
        f"| `service` | Lógica de negócio e orquestração ({service_count} serviços) |\n"
        f"| `routes` | Endpoints HTTP — Ktor routing ({route_file_count} arquivos de rotas, {endpoint_count} endpoints) |\n"
        "| `scraper` | Clientes NTVMR, parsers TEI/XML, Wikipedia |\n"
        f"| `seed` | Dados canônicos (27 livros, 7.956 versículos), manuscritos, pais da igreja (35), declarações textuais (36), traduções (pt, es) — {seed_count} arquivos |\n"
        "| `util` | Utilitários (NtvmrUrl) |"
    )
    text = replace_section(text, "## Pacotes (`com.ntcoverage.*`)", packages_table)

    # -- Update table list --
    table_list = ", ".join(f"`{n}`" for n in table_sql_names)
    text = replace_section(text, "## Tabelas do banco", table_list)

    path.write_text(text)
    print(f"  project-overview.mdc: {table_count} tables, {dto_count} DTOs, "
          f"{repo_count} repos, {service_count} services, {route_file_count} route files, "
          f"{endpoint_count} endpoints")


def update_database_migrations():
    path = RULES / "database-migrations.mdc"
    text = path.read_text()

    migrations = collect_migrations()
    tables = collect_tables()
    next_num = max((m["number"] for m in migrations), default=0) + 1

    # -- Preserve existing descriptions from .mdc --
    existing_descs: dict[str, str] = {}
    for match in re.finditer(r"- `(V\d+__\S+\.sql)`\s*(?:—\s*(.+))?", text):
        existing_descs[match.group(1)] = (match.group(2) or "").strip()

    # -- Update migrations list --
    mig_lines = []
    for m in migrations:
        desc = existing_descs.get(m["filename"], "")
        if desc:
            mig_lines.append(f"- `{m['filename']}` — {desc}")
        else:
            mig_lines.append(f"- `{m['filename']}`")
    mig_content = "\n".join(mig_lines)
    text = replace_section(text, "## Migrações existentes", mig_content)

    # -- Update next migration number --
    text = re.sub(
        r"Próxima migração: \*\*V\d+\*\*",
        f"Próxima migração: **V{next_num}**",
        text,
    )

    # -- Update SchemaUtils tables --
    schema_lines = ", ".join(tables)
    wrapped = wrap_text(schema_lines, 80)
    schema_block = f"```\n{wrapped}\n```"
    text = replace_section(text, "## Tabelas registradas no SchemaUtils", schema_block)

    path.write_text(text)
    print(f"  database-migrations.mdc: {len(migrations)} migrations, next=V{next_num}, "
          f"{len(tables)} SchemaUtils tables")


def update_frontend_conventions():
    path = RULES / "frontend-conventions.mdc"
    text = path.read_text()

    hooks = collect_hooks()
    pages = collect_pages()
    components = collect_components()
    type_count = collect_type_count()
    hook_count = len(hooks)
    page_count = len(pages)

    # -- Update structure tree --
    comp_tree = ""
    comp_items = [(k, v) for k, v in components.items() if k != "(raiz)"]
    for folder, files in comp_items:
        comp_tree += f"│   ├── {folder + '/':20s}{', '.join(f.replace('.tsx', '') for f in files)}\n"
    root_files = components.get("(raiz)", [])
    if root_files:
        comp_tree += f"│   └── {', '.join(root_files):20s}React Query + Theme providers\n"

    structure = (
        "```\n"
        "frontend/\n"
        f"├── app/[locale]/          Páginas ({page_count} rotas) — App Router com i18n\n"
        "├── components/\n"
        f"{comp_tree}"
        f"├── hooks/                 {hook_count} hooks customizados\n"
        "├── i18n/                  routing.ts, navigation.ts, request.ts (config next-intl)\n"
        "├── lib/                   api.ts (funções fetch), utils.ts, verseReference.ts\n"
        f"├── types/                 index.ts ({type_count} interfaces/tipos)\n"
        "└── messages/              en.json, pt.json, es.json\n"
        "```"
    )
    text = replace_section(text, "## Estrutura", structure)

    # -- Update inline counts --
    text = re.sub(r"\d+ hooks customizados", f"{hook_count} hooks customizados", text)
    text = re.sub(r"Páginas \(\d+ rotas\)", f"Páginas ({page_count} rotas)", text)
    text = re.sub(
        r"index\.ts \(\d+ interfaces/tipos\)",
        f"index.ts ({type_count} interfaces/tipos)",
        text,
    )

    # -- Update pages list --
    page_names = build_page_names(pages)
    text = re.sub(
        r"(## Páginas \(\d+ rotas\))\n\n.+",
        rf"\g<1>\n\n{page_names}",
        text,
    )

    path.write_text(text)
    print(f"  frontend-conventions.mdc: {hook_count} hooks, {page_count} pages, "
          f"{type_count} types, {sum(len(v) for v in components.values())} components")


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

def wrap_text(s: str, width: int) -> str:
    """Wrap comma-separated items into lines of max width."""
    items = [item.strip() for item in s.split(",")]
    lines: list[str] = []
    current = ""
    for item in items:
        candidate = f"{current}, {item}" if current else item
        if len(candidate) > width and current:
            lines.append(current + ",")
            current = item
        else:
            current = candidate
    if current:
        lines.append(current)
    return "\n".join(lines)


def build_page_names(pages: list[str]) -> str:
    """Build a human-readable page list from page.tsx paths."""
    names: list[str] = []
    for p in pages:
        parts = Path(p).parent.parts
        clean = [pt for pt in parts if pt not in ("[locale]", "(atlas)", "(manifesto)")]
        if not clean:
            names.append("Home")
        else:
            name = "/".join(clean)
            name = name.replace("[", "").replace("]", "")
            pretty = " ".join(w.capitalize() for w in name.replace("-", " ").replace("/", " / ").split())
            names.append(pretty)
    seen: set[str] = set()
    unique: list[str] = []
    for n in sorted(names):
        if n not in seen:
            seen.add(n)
            unique.append(n)
    return ", ".join(unique)


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------

def main():
    print("update_rules.py — scanning codebase...")
    update_project_overview()
    update_database_migrations()
    update_frontend_conventions()
    print("Done. All .mdc rules updated.")


if __name__ == "__main__":
    main()
