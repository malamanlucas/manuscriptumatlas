---
name: interface-design:init
description: Construir UI com craft e consistência. Para dashboards, apps e ferramentas — não sites de marketing.
---

## Leitura obrigatória

Antes de escrever código, leia completamente:
1. `.claude/skills/interface-design/SKILL.md` — fundação, princípios e craft

---

**Escopo:** Dashboards, apps, ferramentas, painéis admin. Não landing pages ou sites de marketing.

## Intenção primeiro — responda antes de construir

Antes de tocar no código, responda:

**Quem é essa pessoa?** Não "usuários". Onde ela está? O que ela tem em mente?

**O que ela precisa realizar?** Não "usar o dashboard". O verbo. Avaliar manuscritos. Encontrar lacunas de cobertura. Aprovar ingestão.

**Como deve ser a sensação?** Em palavras que significam algo. "Clean" não diz nada. Denso como um terminal? Organizado como um catálogo?

Se não conseguir responder com especificidade, pare e pergunte ao usuário. Não adivinhe.

## Antes de cada componente

Declare a intenção E a abordagem técnica:

```
Intenção: [quem, o que precisa fazer, como deve sentir]
Paleta: [fundação + acento — e POR QUE]
Profundidade: [borders / shadows sutis / camadas — e POR QUE]
Superfícies: [escala de elevação — e POR QUE]
Tipografia: [escolha — e POR QUE]
Espaçamento: [unidade base]
```

Toda escolha deve ser explicável. Se a resposta é "é comum" ou "funciona" — você não escolheu. Você defaultou.

## Fluxo

1. Ler os arquivos obrigatórios acima
2. Verificar se `.interface-design/system.md` existe
3. **Se existe**: aplicar padrões estabelecidos
4. **Se não**: avaliar contexto, sugerir direção, confirmar com usuário, construir

## Comunicação

Seja invisível. Não anuncie modos ou narre processo. Vá direto ao trabalho.

## Após cada tarefa

Ofereça salvar padrões: "Quer que eu salve esses padrões em `.interface-design/system.md`?"

Analise o pedido do usuário: $ARGUMENTS
