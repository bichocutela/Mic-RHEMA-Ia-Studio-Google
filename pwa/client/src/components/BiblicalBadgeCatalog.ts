export type ProfileEmblemRarity = "Raro" | "Épico" | "Lendário";

export type PwaBiblicalBadge = {
  id: string;
  name: string;
  description: string;
  requirement: string;
  level?: number;
  achievement?: boolean;
  rarity?: ProfileEmblemRarity;
};

export const biblicalBadges: PwaBiblicalBadge[] = [
  {id:"caminhante",name:"Caminhante",description:"O início de uma jornada de fé e conhecimento.",requirement:"Criar o perfil e escolher um avatar",level:1},
  {id:"semeador",name:"Semeador",description:"Quem planta a Palavra no coração todos os dias.",requirement:"Ler 3 devocionais e concluir 1 tema de plano",level:2},
  {id:"discipulo",name:"Discípulo",description:"Um passo firme no aprendizado da Palavra.",requirement:"Concluir 1 plano, 3 temas e ler 3 capítulos da Bíblia",level:3},
  {id:"perseverante",name:"Perseverante",description:"Constância para continuar mesmo nos dias difíceis.",requirement:"Acumular 60 minutos ativos e realizar 10 atividades",level:4},
  {id:"estudante_rhema",name:"Estudante Rhema",description:"Dedicação reconhecida ao estudo no Instituto Bíblico Rhema.",requirement:"Ler 3 livros, assistir 3 vídeos e ouvir 2 áudios",level:5},
  {id:"mestre_da_palavra",name:"Mestre da Palavra",description:"Conhecimento construído com disciplina e compromisso.",requirement:"Concluir 1 curso IBR, ler 3 notícias e 10 capítulos da Bíblia",level:6},
  {id:"guardiao_da_fe",name:"Guardião da Fé",description:"Um testemunho de perseverança, serviço e maturidade.",requirement:"Realizar todas as 8 áreas de atividade e acumular 180 minutos ativos",level:7},

  {id:"semente_da_fe",name:"Semente da Fé",description:"Uma nova etapa em que conhecimento e prática começam a florescer.",requirement:"100 XP + 2 capítulos e 2 respostas corretas neste nível",level:8,rarity:"Raro"},
  {id:"caminho_da_promessa",name:"Caminho da Promessa",description:"Passos firmes guiados pela Palavra e pelas promessas de Deus.",requirement:"200 XP + 3 capítulos e 3 respostas corretas neste nível",level:9,rarity:"Raro"},
  {id:"escudo_da_fe",name:"Escudo da Fé",description:"Conhecimento aplicado com convicção e discernimento.",requirement:"350 XP + 5 acertos sem Dica Fácil e 10 minutos ativos neste nível",level:10,rarity:"Raro"},
  {id:"aguas_vivas",name:"Águas Vivas",description:"Uma jornada renovada pela leitura e pelo aprendizado constante.",requirement:"500 XP + 5 capítulos e 5 respostas corretas neste nível",level:11,rarity:"Raro"},
  {id:"videira_verdadeira",name:"Videira Verdadeira",description:"Frutos que aparecem quando a Palavra permanece no coração.",requirement:"650 XP + 2 devocionais e 6 respostas corretas neste nível",level:12,rarity:"Raro"},

  {id:"luz_do_mundo",name:"Luz do Mundo",description:"Conhecimento que ilumina escolhas e fortalece o testemunho.",requirement:"850 XP + 8 acertos sem Dica Fácil e 5 capítulos neste nível",level:13,rarity:"Épico"},
  {id:"armadura_de_deus",name:"Armadura de Deus",description:"Disciplina espiritual para permanecer firme no aprendizado.",requirement:"1050 XP + 10 acertos, 3 difíceis e 15 minutos ativos neste nível",level:14,rarity:"Épico"},
  {id:"leao_de_juda",name:"Leão de Judá",description:"Coragem, constância e compromisso crescente com a Palavra.",requirement:"1250 XP + 10 capítulos, 10 acertos e 20 minutos ativos neste nível",level:15,rarity:"Épico"},
  {id:"chama_do_espirito",name:"Chama do Espírito",description:"Uma busca intensa que une conhecimento, dedicação e propósito.",requirement:"1450 XP + 5 acertos difíceis e 10 acertos sem nenhuma dica neste nível",level:16,rarity:"Épico"},
  {id:"coroa_da_vida",name:"Coroa da Vida",description:"Perseverança reconhecida em uma caminhada de estudo consistente.",requirement:"1650 XP + 12 acertos sem Dica Fácil e 30 minutos ativos neste nível",level:17,rarity:"Épico"},

  {id:"asas_da_promessa",name:"Asas da Promessa",description:"Maturidade para avançar com profundidade e constância.",requirement:"1850 XP + 15 capítulos, 8 acertos difíceis e 30 minutos ativos neste nível",level:18,rarity:"Lendário"},
  {id:"tabernaculo",name:"Tabernáculo",description:"Uma jornada ampla que passa por todas as áreas de estudo do MIC Rhema.",requirement:"2050 XP + usar todas as 8 áreas e acertar 10 perguntas difíceis neste nível",level:19,rarity:"Lendário"},
  {id:"arca_da_alianca",name:"Arca da Aliança",description:"Dedicação excepcional ao estudo bíblico e à formação espiritual.",requirement:"2300 XP + 20 capítulos, 12 acertos difíceis e 45 minutos ativos neste nível",level:20,rarity:"Lendário"},
  {id:"nova_jerusalem",name:"Nova Jerusalém",description:"Uma conquista reservada a uma jornada extensa de conhecimento.",requirement:"2600 XP + 20 acertos, 15 difíceis e 60 minutos ativos neste nível",level:21,rarity:"Lendário"},
  {id:"gloria_eterna",name:"Glória Eterna",description:"O emblema máximo da Jornada Bíblica no MIC Rhema.",requirement:"3000 XP + 30 acertos, 20 difíceis e 120 minutos ativos neste nível",level:22,rarity:"Lendário"},

  {id:"primeira_oracao",name:"Primeira Oração",description:"Um primeiro momento separado para falar com Deus.",requirement:"Registrar o primeiro momento de oração",achievement:true},
  {id:"leitor_da_palavra",name:"Leitor da Palavra",description:"A Bíblia aberta e o coração disposto a aprender.",requirement:"Ler 10 capítulos da Bíblia",achievement:true},
  {id:"coracao_grato",name:"Coração Grato",description:"Reconhecimento pelas bênçãos recebidas.",requirement:"Registrar uma mensagem de gratidão",achievement:true},
  {id:"constante",name:"Constante",description:"Pequenos passos repetidos com fidelidade.",requirement:"Estudar por 7 dias consecutivos",achievement:true},
  {id:"certificado_ibr",name:"Certificado IBR",description:"Uma conquista acadêmica no Instituto Bíblico Rhema.",requirement:"Receber um certificado IBR",achievement:true},
];

export const levelBadges = biblicalBadges.filter((badge) => badge.level).sort((a,b)=>(a.level||0)-(b.level||0));

export const profileEmblemLevelById: Record<string, number> = Object.fromEntries(
  biblicalBadges.filter((badge) => (badge.level || 0) >= 8 && (badge.level || 0) <= 22).map((badge) => [badge.id, badge.level!]),
);

export function badgeForId(id: string) {
  return biblicalBadges.find((badge) => badge.id === id) || biblicalBadges[0];
}
