# Distintivos MICRHEMA

14 artes originais em PNG transparente. O manifesto lista nomes, descrições e arquivos.

## Configuração

No Painel Administrativo → Loja XP → Distintivos, edite o item, ligue “Vender na Loja XP” e informe um valor positivo. Opcionalmente selecione os emblemas compatíveis. Sem venda, os emblemas selecionados recebem o distintivo diretamente. A disponibilidade controla tanto a exibição quanto a venda. O comprador pode ativar/desativar em Minhas compras; essa preferência segue o armazenamento local já usado pelos cosméticos existentes. Distintivos comprados aparecem no perfil do comprador neste dispositivo; a sincronização visual em perfis de terceiros não está incluída.

## Servidor

Aplicar schema.sql antes de implantar xp-shop-admin-simple; seed.sql é idempotente e preserva itens existentes. As imagens apontam para o commit imutável dos assets. Nesta entrega, a migração, a função e o cadastro foram aplicados ao projeto associado. Nenhum preço, vínculo ou compra foi atribuído automaticamente.

## Verificação

- 14 PNGs com canal alpha transparente.
- Sintaxe TypeScript validada com Node.
- Teste transacional com rollback confirmou criação do produto, custo, desativação e retirada da venda.
- Catálogo confirmado com 14 registros, nenhum à venda ou vinculado.
- Compilação Android não executada: download do Gradle bloqueado por Network is unreachable. Validar build e prévia no dispositivo antes de distribuir o APK.
