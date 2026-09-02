# Correção da Pesquisa Profunda da Bíblia

Data: 2026-09-02

## Problema encontrado

Os arquivos locais `pt_aa.json` e `pt_acf.json` estavam truncados em aproximadamente 2 MB. A auditoria encontrou apenas 18 livros completos e o 19º livro (Salmos) interrompido no meio de um versículo. Como a pesquisa antiga tentava carregar o arquivo inteiro com `JSONArray`, a leitura do índice falhava e a interface transformava silenciosamente qualquer erro em uma lista vazia, exibindo “Nenhuma referência encontrada”.

## Correção aplicada

- A Pesquisa Profunda passou a utilizar o endpoint de pesquisa do Bolls, o mesmo serviço usado pelo leitor bíblico nativo do MIC Rhema.
- A pesquisa usa a versão atualmente selecionada pelo usuário.
- Consultas comuns usam busca semântica em toda a tradução, permitindo palavras, nomes, temas e situações.
- Referências explícitas como `João 3:16` são tratadas como referências exatas.
- Erros de conexão deixam de ser apresentados como “nenhum resultado” e passam a informar indisponibilidade da pesquisa.
- Foi mantido debounce para não enviar uma requisição a cada tecla.

## Smoke tests executados

A API foi consultada automaticamente com a tradução ARA para:

- `curado`
- `amor`
- `Davi`

Todos os testes retornaram referências contendo livro, capítulo, versículo e texto antes da publicação da correção.
