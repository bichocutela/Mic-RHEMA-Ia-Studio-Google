/**
 * SANTUÁRIO EM MOVIMENTO — Conteúdo de continuidade visual.
 * Dados reais substituem estes cartões automaticamente quando os listeners Firestore estiverem configurados.
 */
export const ASSETS = {
  hero: "https://cwphbkdtorfpgmnlafqb.supabase.co/storage/v1/object/public/media-assets/rhema-admin/media-73d6c1c3-49f9-4725-8e06-5939165e4226.jpg",
  devotional: "https://cwphbkdtorfpgmnlafqb.supabase.co/storage/v1/object/public/media-assets/rhema-admin/media-dc267d77-3931-49bf-944d-e633b1c2f69c.jpg",
  media: "https://cwphbkdtorfpgmnlafqb.supabase.co/storage/v1/object/public/media-assets/rhema-admin/media-f98917a4-16e2-44db-b2b0-82844006a344.jpg",
  logo: "https://cwphbkdtorfpgmnlafqb.supabase.co/storage/v1/object/public/media-assets/rhema-admin/media-841abe59-055a-4982-a029-9d432c4ec0c6.png",
};

export type ContentCard = {
  id: string;
  title: string;
  subtitle: string;
  image?: string;
  tag?: string;
};

export const sampleNews: ContentCard[] = [
  { id: "news-1", title: "A esperança que reorganiza o coração", subtitle: "Leitura bíblica para esta semana", image: ASSETS.devotional, tag: "PALAVRA" },
  { id: "news-2", title: "Comunhão que alcança quem chega", subtitle: "Notícias da nossa igreja", image: ASSETS.media, tag: "IGREJA" },
  { id: "news-3", title: "Permaneça firme na caminhada", subtitle: "Plano de leitura disponível", image: ASSETS.hero, tag: "PLANO" },
];

export const sampleMedia: ContentCard[] = [
  { id: "media-1", title: "Culto de celebração", subtitle: "Mensagem e louvor", image: ASSETS.media, tag: "VÍDEO" },
  { id: "media-2", title: "Devocional em áudio", subtitle: "Uma pausa para ouvir", image: ASSETS.devotional, tag: "ÁUDIO" },
  { id: "media-3", title: "Biblioteca Rhema", subtitle: "Estudos para continuar", image: ASSETS.hero, tag: "LIVRO" },
];

export const bibleBooks = ["Gênesis", "Êxodo", "Salmos", "Provérbios", "Mateus", "João", "Romanos", "Apocalipse"];
export const genesisVerses = [
  "No princípio, Deus criou os céus e a terra.",
  "A terra era sem forma e vazia; havia trevas sobre a face do abismo, e o Espírito de Deus pairava por sobre as águas.",
  "Disse Deus: Haja luz; e houve luz.",
  "E viu Deus que a luz era boa; e fez separação entre a luz e as trevas.",
  "Chamou Deus à luz Dia e às trevas, Noite. Houve tarde e manhã, o primeiro dia.",
];

export const settingsSections = [
  ["Aparência", "Tema e tamanho de fonte"],
  ["Leitura bíblica", "Posição, fonte e modo de leitura"],
  ["Áudio", "Velocidade, pulo e temporizador"],
  ["Downloads", "Pastas, Wi‑Fi e limpeza"],
  ["Notificações", "Conteúdo, cultos e atualizações"],
  ["Internet e dados", "Pré-carregamento e economia de dados"],
  ["Favoritos e histórico", "Sincronização e registros"],
];
