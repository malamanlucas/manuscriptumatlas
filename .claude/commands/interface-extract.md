---
name: interface-design:extract
description: Extrair padrões de design do código existente para criar system.md.
---

# Extração de Padrões

Extrair padrões de design do código existente para criar um sistema.

## O que extrair

**Escanear arquivos UI (tsx, jsx) por:**

1. **Spacing repetido** — identificar base e escala (ex: 4, 8, 12, 16, 24)
2. **Radius repetido** — escala de arredondamento
3. **Padrões de Button** — altura, padding, radius dominantes
4. **Padrões de Card** — border vs shadow, padding dominante
5. **Estratégia de depth** — contar borders vs shadows para determinar abordagem

**Resultado esperado:**
```
Padrões extraídos:

Spacing: Base 4px, Escala: 4, 8, 12, 16, 24, 32
Depth: Borders-only (34 borders, 2 shadows)
Button: 36px h, 12px 16px pad, 6px radius
Card: 1px border, 16px pad

Criar .interface-design/system.md com esses padrões? (s/n/customizar)
```

## Implementação

1. Glob por arquivos UI
2. Parsear valores repetidos
3. Identificar padrões comuns por frequência
4. Sugerir sistema baseado nos padrões
5. Oferecer criação do system.md
6. Permitir customização antes de salvar

Analise o pedido do usuário: $ARGUMENTS
