# 🚀 Branch: `produção`

Esta é a **branch de desenvolvimento ativo** do projeto **MeuPimenta / ConnectaRO**. Aqui são centralizadas todas as melhorias contínuas, novas funcionalidades e correções do sistema antes de irem para a versão estável final.

---

## 📌 Propósito desta Branch

Nesta branch são realizadas:
* 🛠️ **Correções de bugs (*Bug Fixes*):** Ajustes de layout, regras de negócio e falhas encontradas na aplicação.
* ✨ **Novas Funcionalidades (*Features*):** Implementação de novas páginas, melhorias de API, DTOs e componentes de UI.
* 🧹 **Refatorações:** Melhorias de arquitetura, segurança e organização do código.

---

## 🔄 Fluxo de Trabalho e Sincronização

Para manter o código seguro e bem testado, seguimos a seguinte rotina de *merges*:

1. **Desenvolvimento:** Todas as alterações diárias são testadas e validadas nesta branch.
2. **Merge Semanal:** A cada **semana**, o código acumulado e estabilizado na branch `produção` passa por um *Merge* para a branch principal (`master`).

```text
[Desenvolvimento & Fixes]  ──>  produção (Branch Atual)
                                   │
                                   ▼ (Merge Semanal)
                             master / main (Versão Estável)
