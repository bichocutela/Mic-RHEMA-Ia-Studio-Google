from pathlib import Path

path = Path('app/src/main/java/com/aistudio/micrhema/AboutScreen.kt')
text = path.read_text(encoding='utf-8')

replacements = [
    ('import androidx.compose.material.icons.filled.Share\n', 'import androidx.compose.material.icons.filled.Android\nimport androidx.compose.material.icons.filled.PhoneIphone\n'),
    ('text = "Compartilhar acesso"', 'text = "Compartilhar Via"'),
    ('Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))\n                                        Spacer(modifier = Modifier.width(6.dp))\n                                        Text("Android")', 'Icon(Icons.Default.Android, contentDescription = "Android", modifier = Modifier.size(20.dp))\n                                        Spacer(modifier = Modifier.width(6.dp))\n                                        Text("Android")'),
    ('Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))\n                                    Spacer(modifier = Modifier.width(6.dp))\n                                    Text("PWA")', 'Icon(Icons.Default.PhoneIphone, contentDescription = "iPhone", modifier = Modifier.size(20.dp))\n                                    Spacer(modifier = Modifier.width(6.dp))\n                                    Text("iPhone")'),
    ('Android baixa a APK mais recente. PWA abre a versão web instalável diretamente no navegador.', 'Android baixa a APK mais recente. iPhone abre a versão web instalável diretamente no navegador.'),
    ('text = "MIC Rhema PWA"', 'text = "MIC Rhema no iPhone"'),
    ('contentDescription = "QR Code do MIC Rhema PWA"', 'contentDescription = "QR Code do MIC Rhema para iPhone"'),
    ('text = "Escaneie para abrir o MIC Rhema PWA no navegador"', 'text = "Escaneie para abrir o MIC Rhema no iPhone"'),
    ('text = "No celular, o PWA pode ser adicionado à tela inicial pelo próprio navegador."', 'text = "No iPhone, o MIC Rhema pode ser adicionado à Tela de Início pelo navegador."'),
    ('text = "Link do PWA copiado!"', 'text = "Link para iPhone copiado!"'),
]

for old, new in replacements:
    if old not in text:
        raise SystemExit(f'Trecho não encontrado: {old[:80]}')
    text = text.replace(old, new, 1)

path.write_text(text, encoding='utf-8')
print('Aba Sobre atualizada com Android/iPhone e Compartilhar Via.')
