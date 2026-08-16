# Supabase Storage — MIC Rhema

O aplicativo mantém o Firebase Auth e usa a Edge Function `storage-gateway` como único gateway de armazenamento. O APK envia o token de sessão Firebase no cabeçalho `Authorization: Bearer ...`; a função valida o token, confere o proprietário ou administrador no Firestore e usa a `service_role` somente no ambiente da função.

Os buckets privados são `profile-photos` e `church-documents`. Fotos aceitas: JPEG, PNG e WebP até 5 MB. Certificados aceitos: PDF até 50 MB. O Firestore salva apenas `supabaseStoragePath` para fotos e `ibrCertificateStoragePath` para certificados. As URLs devolvidas para a interface são assinadas por 15 minutos e não devem ser tratadas como valores permanentes.

## Configuração do CI

No repositório GitHub `bichocutela/Mic-RHEMA-Ia-Studio-Google`, cadastrar os seguintes **Repository secrets**:

| Nome | Valor |
| --- | --- |
| `SUPABASE_URL` | URL pública do projeto Supabase |
| `SUPABASE_ANON_KEY` | Chave pública/publishable do projeto Supabase |

O workflow copia `.env.example` para `.env` durante o build e substitui esses dois valores quando os secrets existem. O plugin `secrets-gradle-plugin` expõe as propriedades como campos `BuildConfig` no app.

A variável `SUPABASE_SERVICE_ROLE_KEY` não pertence ao GitHub, ao APK, ao `.env`, ao Firestore nem a este repositório. Ela deve permanecer exclusivamente nos secrets da Edge Function gerenciados pelo Supabase.

## Compatibilidade

Documentos antigos que ainda possuem `profilePhotoUrl` ou `ibrCertificateUrl` continuam sendo lidos como URLs legadas. Novos uploads gravam o caminho privado e limpam a URL permanente. O cache local da foto serve apenas para uso offline; ele não é a fonte de sincronização do perfil.
