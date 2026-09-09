create table if not exists public.profile_cosmetics (
  id text primary key,
  kind text not null check (kind in ('distintivo','moldura')),
  name text not null,
  description text not null default '',
  challenge text not null,
  challenge_metric text not null,
  challenge_target integer not null check (challenge_target > 0),
  image_ref text not null,
  active boolean not null default true,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index if not exists profile_cosmetics_kind_active_idx
  on public.profile_cosmetics(kind, active, created_at);

alter table public.profile_cosmetics enable row level security;
revoke all on public.profile_cosmetics from anon, authenticated;
grant select, insert, update, delete on public.profile_cosmetics to service_role;

comment on table public.profile_cosmetics is
  'Distintivos e molduras PNG criados pelo ADM da Loja XP com desafios verificáveis.';
