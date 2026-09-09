create table if not exists public.profile_light_effects (
  id text primary key,
  name text not null,
  description text not null default '',
  effect_type text not null,
  tone text not null default 'medio' check (tone in ('suave','medio','forte')),
  color_hex text not null default '#FFD54F',
  purchasable boolean not null default false,
  xp_cost integer not null default 0 check (xp_cost >= 0),
  emblem_ids jsonb not null default '[]'::jsonb,
  active boolean not null default true,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index if not exists profile_light_effects_active_idx
  on public.profile_light_effects(active, created_at);

alter table public.profile_light_effects enable row level security;
revoke all on public.profile_light_effects from anon, authenticated;
grant select, insert, update, delete on public.profile_light_effects to service_role;

insert into public.profile_light_effects (id,name,description,effect_type,tone,color_hex) values
('aura_promessa','Aura da Promessa','Feixe dourado percorre o avatar com partículas suaves.','orbit','#medio','#FFD54F'),
('ceu_estrelado','Céu Estrelado','Estrelas cintilantes ao redor do avatar.','stars','suave','#69A7FF'),
('chama_espirito','Chama do Espírito','Chamas luminosas animadas contornando o perfil.','flame','medio','#FF6A1A'),
('gloria_divina','Glória Divina','Aura pulsante com expansão de luz.','pulse','medio','#A66BFF'),
('vida_abundante','Vida Abundante','Partículas verdes e energia orgânica.','particles','suave','#73E36B'),
('luz_celestial','Luz Celestial','Halo branco-dourado com brilho respirando.','halo','medio','#FFF1B0'),
('espirito_fogo','Espírito de Fogo','Anel de fogo intenso e faíscas rápidas.','fire_ring','forte','#FF3B1F'),
('raios_gloria','Raios de Glória','Raios radiais surgem atrás do avatar.','rays','forte','#FFC928'),
('poeira_dourada','Poeira Dourada','Poeira cintilante sobe lentamente.','dust','suave','#F4CF68'),
('halo_divino','Halo Divino','Círculo luminoso fino girando lentamente.','halo_orbit','suave','#FFF5CF'),
('energia_azul','Energia Azul','Arcos elétricos azuis circulando o avatar.','electric','forte','#3C8DFF'),
('aura_esmeralda','Aura Esmeralda','Brilho verde profundo com partículas.','aura','medio','#34D399'),
('chamas_roxas','Chamas Roxas','Chamas violetas com brilho mágico.','violet_flame','forte','#A855F7'),
('arco_alianca','Arco-Íris da Aliança','Gradiente luminoso giratório multicolorido.','rainbow_orbit','medio','#FFFFFF'),
('luz_espirito','Luz do Espírito','Pulso branco suave com pequenas fagulhas.','spirit_light','suave','#FFFFFF')
on conflict (id) do nothing;

comment on table public.profile_light_effects is
  'Efeitos de luz animados do avatar, configuráveis pelo ADM da Loja XP.';