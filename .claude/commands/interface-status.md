---
name: interface-design:status
description: Mostrar estado atual do design system incluindo direção, tokens e padrões.
---

# Status do Design System

Mostrar estado atual do design system do projeto.

## O que mostrar

**Se `.interface-design/system.md` existe:**

```
Design System: [Nome do Projeto]

Direção: [Precisão & Densidade / Calor / etc]
Fundação: [Slate cool / Stone warm / etc]
Profundidade: [Borders-only / Shadows sutis / Camadas]

Tokens:
- Spacing base: 4px
- Radius: 4px, 6px, 8px
- Cores: [contagem] definidas

Padrões:
- Button Primary (36px h, 16px px, 6px radius)
- Card Default (border, 16px pad)
- [outros...]

Atualizado: [data do git ou mtime]
```

**Se não existe system.md:**

```
Nenhum design system encontrado.

Opções:
1. Construa UI → sistema será estabelecido automaticamente
2. Execute /interface-extract → extrair padrões do código existente
```

## Implementação

1. Ler `.interface-design/system.md`
2. Parsear direção, tokens, padrões
3. Formatar e exibir
4. Se não existe, sugerir próximos passos

Analise o pedido do usuário: $ARGUMENTS
