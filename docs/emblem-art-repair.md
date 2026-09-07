# Molduras de perfil — revisão HD

Conjunto final: níveis 8 a 22, WebP lossless RGBA de 1024×1024, centro transparente e bordas completas. Os níveis 1 a 7 continuam vetoriais.

As artes antigas tinham 160×160/256×256, cortes e arquivos truncados. A sequência visual foi corrigida: luz no 13, armadura no 14, leão no 15, chama no 16 e coroa no 17. Foram corrigidas as imagens, preservando os IDs, nomes e níveis persistidos.

As artes foram reconstruídas pela ferramenta integrada de geração de imagens usando as referências recuperáveis e mantendo seus motivos e estilo 3D. Para Tabernáculo, Arca da Aliança e Nova Jerusalém, cujas artes originais estavam indisponíveis/truncadas, foram produzidas artes companheiras do conjunto. O acabamento local de transparência e dimensionamento foi autorizado pelo usuário. Não houve simples ampliação das miniaturas.

## Integração e verificação

- `docs/profile-emblem-art.json` registra identidade, tamanho, SHA-256 e encaixe de cada avatar.
- Android e PWA compartilham a proporção revisada de cada moldura.
- O Android usa os WebP revisados nos níveis 18 a 22 em lugar das alternativas vetoriais provisórias. Os recursos provisórios foram preservados.
- A PWA usa revisão de URL `hd-20260907` para renovar o cache das imagens.
- `python3 tools/check_profile_emblems.py` verifica decodificação integral, contêiner, resolução, alpha, margem, centro livre, hash e referências de código.
- As composições foram revisadas com Davi, incluindo margens e ausência de vazamento do retrato para fora da moldura.

Avatares PNG, autenticação, progressão/XP, applicationId, assinatura e regras de versionamento não foram alterados por esta revisão.

## Entrega

As molduras Android são recursos locais: sua entrega exige um novo APK assinado pelo processo existente. A PWA recebe as imagens após a publicação da versão web e dos arquivos em `main`. Atualizar somente a branch do PR não atualiza o aplicativo instalado.

O PR 13 é o ponto de validação. A compilação dos níveis 8 a 13 passou; o resultado do pacote completo deve ser conferido no último commit do PR.
