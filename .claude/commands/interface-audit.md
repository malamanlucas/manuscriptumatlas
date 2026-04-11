---
name: interface-design:audit
description: Verificar código existente contra o design system para violações de spacing, depth, cor e padrões.
---

# Auditoria de Design

Verificar código contra o design system do projeto.

## O que verificar

**Se `.interface-design/system.md` existe:**

1. **Spacing** — valores fora do grid definido (ex: 17px quando base é 4px)
2. **Depth** — shadows onde o sistema usa borders-only, ou vice-versa
3. **Cores** — cores fora da paleta definida
4. **Padrões** — botões/cards que não seguem os padrões documentados

**Formato do relatório:**
```
Resultados: src/components/

Violações:
  Button.tsx:12 - Altura 38px (padrão: 36px)
  Card.tsx:8 - Shadow usado (sistema: borders-only)
  Input.tsx:20 - Spacing 14px (grid: 4px, mais próximo: 12px ou 16px)

Sugestões:
  - Ajustar altura do Button para o padrão
  - Substituir shadow por border
  - Ajustar spacing para o grid
```

**Se não existe system.md:**
```
Nenhum design system para auditar. Crie um primeiro:
1. Construa UI → sistema estabelecido automaticamente
2. Execute /interface-extract → cria sistema do código existente
```

## Implementação

1. Ler system.md
2. Parsear regras do sistema
3. Ler arquivos alvo (tsx, jsx, css)
4. Comparar contra regras
5. Reportar violações com sugestões

Analise o pedido do usuário: $ARGUMENTS
