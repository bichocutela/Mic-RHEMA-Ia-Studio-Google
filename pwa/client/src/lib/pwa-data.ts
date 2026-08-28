/**
 * SANTUÁRIO EM MOVIMENTO — Conteúdo de continuidade visual.
 * Dados reais substituem estes cartões automaticamente quando os listeners Firestore estiverem configurados.
 */
export const ASSETS = {
  hero: "https://cwphbkdtorfpgmnlafqb.supabase.co/storage/v1/object/public/media-assets/rhema-admin/media-73d6c1c3-49f9-4725-8e06-5939165e4226.jpg",
  devotional: "https://cwphbkdtorfpgmnlafqb.supabase.co/storage/v1/object/public/media-assets/rhema-admin/media-dc267d77-3931-49bf-944d-e633b1c2f69c.jpg",
  media: "https://cwphbkdtorfpgmnlafqb.supabase.co/storage/v1/object/public/media-assets/rhema-admin/media-f98917a4-16e2-44db-b2b0-82844006a344.jpg",
  logo: `${import.meta.env.BASE_URL}brand/mic-rhema-android-logo.png`,
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

/** PARIDADE ANDROID — mesma ordem e quantidade de capítulos de BibleModule.kt. */
export const bibleBookChapters = {
  "Gênesis": 50, "Êxodo": 40, "Levítico": 27, "Números": 36, "Deuteronômio": 34,
  "Josué": 24, "Juízes": 21, "Rute": 4, "1 Samuel": 31, "2 Samuel": 24,
  "1 Reis": 22, "2 Reis": 25, "1 Crônicas": 29, "2 Crônicas": 36, "Esdras": 10,
  "Neemias": 13, "Ester": 10, "Jó": 42, "Salmos": 150, "Provérbios": 31,
  "Eclesiastes": 12, "Cânticos": 8, "Isaías": 66, "Jeremias": 52, "Lamentações": 5,
  "Ezequiel": 48, "Daniel": 12, "Oséias": 14, "Joel": 3, "Amós": 9, "Obadias": 1,
  "Jonas": 4, "Miquéias": 7, "Naum": 3, "Habacuque": 3, "Sofonias": 3, "Ageu": 2,
  "Zacarias": 14, "Malaquias": 4, "Mateus": 28, "Marcos": 16, "Lucas": 24, "João": 21,
  "Atos": 28, "Romanos": 16, "1 Coríntios": 16, "2 Coríntios": 13, "Gálatas": 6,
  "Efésios": 6, "Filipenses": 4, "Colossenses": 4, "1 Tessalonicenses": 5,
  "2 Tessalonicenses": 3, "1 Timóteo": 6, "2 Timóteo": 4, "Tito": 3, "Filemom": 1,
  "Hebreus": 13, "Tiago": 5, "1 Pedro": 5, "2 Pedro": 3, "1 João": 5,
  "2 João": 1, "3 João": 1, "Judas": 1, "Apocalipse": 22,
} as const;

/** Lista usada por selects e navegação. Como a interface recebe valores do DOM/localStorage,
 * ela é exposta como string[] e validada contra bibleBookChapters quando precisa do tipo estrito. */
export const bibleBooks: string[] = Object.keys(bibleBookChapters);

export const settingsSections = [
  ["Aparência", "Tema e tamanho de fonte"],
  ["Leitura bíblica", "Posição, fonte e modo de leitura"],
  ["Áudio", "Velocidade, pulo e temporizador"],
  ["Downloads", "Pastas, Wi‑Fi e limpeza"],
  ["Notificações", "Conteúdo, cultos e atualizações"],
  ["Internet e dados", "Pré-carregamento e economia de dados"],
  ["Favoritos e histórico", "Sincronização e registros"],
];